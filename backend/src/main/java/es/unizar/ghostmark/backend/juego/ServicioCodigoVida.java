package es.unizar.ghostmark.backend.juego;

import es.unizar.ghostmark.backend.cuenta.CuentaRepository;
import es.unizar.ghostmark.backend.inscripcion.ErrorInscripcion;
import es.unizar.ghostmark.backend.inscripcion.InscripcionRepository;
import es.unizar.ghostmark.backend.partida.Partida;
import es.unizar.ghostmark.backend.partida.PartidaNoEncontradaException;
import es.unizar.ghostmark.backend.partida.PartidaRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioCodigoVida {

    private final CuentaRepository cuentas;
    private final PartidaRepository partidas;
    private final InscripcionRepository inscripciones;
    private final JugadorPartidaRepository jugadores;
    private final Clock reloj;
    private final GeneradorCodigoVida generador = new GeneradorCodigoVida();

    public ServicioCodigoVida(CuentaRepository cuentas, PartidaRepository partidas,
            InscripcionRepository inscripciones, JugadorPartidaRepository jugadores, Clock reloj) {
        this.cuentas = cuentas;
        this.partidas = partidas;
        this.inscripciones = inscripciones;
        this.jugadores = jugadores;
        this.reloj = reloj;
    }

    @Transactional
    public CodigoVida obtener(UUID cuentaId, UUID partidaId) {
        if (cuentas.bloquearPorId(cuentaId).isEmpty()) {
            throw new ErrorInscripcion(HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO",
                    "La cuenta no está disponible");
        }
        Partida partida = partidas.bloquearPorId(partidaId)
                .orElseThrow(PartidaNoEncontradaException::new);
        // La inscripción se resuelve con la cuenta autenticada antes de revelar el estado
        UUID inscripcionId = inscripciones.idDeCuentaEnPartida(partidaId, cuentaId)
                .orElseThrow(PartidaNoEncontradaException::new);
        if (!"EN_JUEGO".equals(partida.getEstado())) {
            throw new ErrorInscripcion(HttpStatus.CONFLICT, "PARTIDA_NO_EN_JUEGO",
                    "La partida no está en juego");
        }
        var jugador = jugadores.obtenerCodigo(partidaId, inscripcionId)
                .orElseThrow(() -> new IllegalStateException("La partida no tiene al jugador"));
        if (!"VIVO".equals(jugador.estado())) {
            throw new ErrorInscripcion(HttpStatus.CONFLICT, "JUGADOR_ELIMINADO",
                    "El jugador está eliminado");
        }
        return generador.generar(jugador.secreto(), Instant.now(reloj));
    }
}
