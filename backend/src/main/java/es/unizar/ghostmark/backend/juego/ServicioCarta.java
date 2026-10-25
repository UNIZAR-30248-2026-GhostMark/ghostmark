package es.unizar.ghostmark.backend.juego;

import es.unizar.ghostmark.backend.cuenta.CuentaRepository;
import es.unizar.ghostmark.backend.evento.ServicioEvento;
import es.unizar.ghostmark.backend.inscripcion.ErrorInscripcion;
import es.unizar.ghostmark.backend.inscripcion.InscripcionRepository;
import es.unizar.ghostmark.backend.partida.Partida;
import es.unizar.ghostmark.backend.partida.PartidaNoEncontradaException;
import es.unizar.ghostmark.backend.partida.PartidaRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioCarta {

    private final CuentaRepository cuentas;
    private final PartidaRepository partidas;
    private final InscripcionRepository inscripciones;
    private final CartaRepository cartas;
    private final ServicioEvento eventos;
    private final SecureRandom aleatorio = new SecureRandom();

    public ServicioCarta(CuentaRepository cuentas, PartidaRepository partidas,
            InscripcionRepository inscripciones, CartaRepository cartas, ServicioEvento eventos) {
        this.cuentas = cuentas;
        this.partidas = partidas;
        this.inscripciones = inscripciones;
        this.cartas = cartas;
        this.eventos = eventos;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void repartir(UUID partidaId, List<UUID> jugadores, Instant ahora) {
        List<UUID> misiones = new ArrayList<>(cartas.misionesActivas());
        if (misiones.isEmpty()) {
            throw new IllegalStateException("No hay misiones activas para repartir");
        }
        StringJoiner asignaciones = new StringJoiner(",", "{\"asignaciones\":[", "]}");
        for (int i = 0; i < jugadores.size(); i++) {
            // Se agota cada baraja antes de repetir, también con tipoMisiones = IA en S1.
            int posicion = i % misiones.size();
            if (posicion == 0) {
                Collections.shuffle(misiones, aleatorio);
            }
            asignaciones.add("{\"inscripcionId\":\"" + jugadores.get(i)
                    + "\",\"misionId\":\"" + misiones.get(posicion) + "\"}");
        }
        eventos.guardar(partidaId, "MISIONES_REPARTIDAS", asignaciones.toString(), ahora);
    }

    @Transactional
    public Carta obtener(UUID cuentaId, UUID partidaId) {
        if (cuentas.bloquearPorId(cuentaId).isEmpty()) {
            throw new ErrorInscripcion(HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO",
                    "La cuenta no está disponible");
        }
        Partida partida = partidas.bloquearPorId(partidaId)
                .orElseThrow(PartidaNoEncontradaException::new);
        // El máster solo tiene carta si juega; nunca se acepta el id de otro jugador.
        UUID inscripcionId = inscripciones.idDeCuentaEnPartida(partidaId, cuentaId)
                .orElseThrow(PartidaNoEncontradaException::new);
        if (!"EN_JUEGO".equals(partida.getEstado())) {
            throw new ErrorInscripcion(HttpStatus.CONFLICT, "PARTIDA_NO_EN_JUEGO",
                    "La partida no está en juego");
        }
        return cartas.obtener(partidaId, inscripcionId)
                .orElseThrow(() -> new IllegalStateException("La partida no tiene la carta"));
    }
}
