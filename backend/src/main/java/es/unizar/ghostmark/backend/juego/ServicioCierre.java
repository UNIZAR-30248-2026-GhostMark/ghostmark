package es.unizar.ghostmark.backend.juego;

import es.unizar.ghostmark.backend.cuenta.CuentaRepository;
import es.unizar.ghostmark.backend.evento.ServicioEvento;
import es.unizar.ghostmark.backend.inscripcion.InscripcionRepository;
import es.unizar.ghostmark.backend.partida.Partida;
import es.unizar.ghostmark.backend.partida.PartidaNoEncontradaException;
import es.unizar.ghostmark.backend.partida.PartidaRepository;
import es.unizar.ghostmark.backend.partida.ServicioPartida;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioCierre {

    private static final String ULTIMO_SUPERVIVIENTE = "ULTIMO_SUPERVIVIENTE";
    private static final String FECHA_FIN = "FECHA_FIN";
    private static final String POR_MASTER = "MASTER";

    private final CuentaRepository cuentas;
    private final PartidaRepository partidas;
    private final InscripcionRepository inscripciones;
    private final JugadorPartidaRepository jugadores;
    private final ServicioEvento eventos;
    private final ServicioPartida servicioPartida;

    public ServicioCierre(CuentaRepository cuentas, PartidaRepository partidas,
            InscripcionRepository inscripciones, JugadorPartidaRepository jugadores,
            ServicioEvento eventos, ServicioPartida servicioPartida) {
        this.cuentas = cuentas;
        this.partidas = partidas;
        this.inscripciones = inscripciones;
        this.jugadores = jugadores;
        this.eventos = eventos;
        this.servicioPartida = servicioPartida;
    }

    /**
     * Cierra la partida si le toca (un único vivo o la fecha de fin pasada) y no queda ninguna
     * disputa abierta ni eliminación que aún se pueda disputar. Se vuelve a comprobar con la fila
     * bloqueada, porque entre la consulta y el bloqueo alguien ha podido abrir una disputa.
     */
    @Transactional
    public boolean aplicarCierre(UUID partidaId, Instant ahora) {
        Partida partida = partidas.bloquearPorId(partidaId).orElse(null);
        if (partida == null || !"EN_JUEGO".equals(partida.getEstado())
                || partidas.conCierrePendiente(partidaId, ahora)) {
            return false;
        }
        if (jugadores.vivos(partidaId).size() == 1) {
            cerrar(partida, ULTIMO_SUPERVIVIENTE, ahora);
            return true;
        }
        if (!ahora.isBefore(partida.getFinEn())) {
            cerrar(partida, FECHA_FIN, ahora);
            return true;
        }
        return false;
    }

    @Transactional
    public Map<String, Object> cerrarPorMaster(UUID cuentaId, UUID partidaId, Instant ahora) {
        if (cuentas.bloquearPorId(cuentaId).isEmpty()) {
            throw new ErrorJuego(HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO",
                    "La cuenta no está disponible");
        }
        Partida partida = partidas.bloquearPorId(partidaId)
                .orElseThrow(PartidaNoEncontradaException::new);
        if (!cuentaId.equals(partida.getMasterId())) {
            if (inscripciones.existe(partidaId, cuentaId)) {
                throw new ErrorJuego(HttpStatus.FORBIDDEN, "NO_ES_MASTER",
                        "Solo el máster puede cerrar la partida");
            }
            throw new PartidaNoEncontradaException();
        }
        if (!"EN_JUEGO".equals(partida.getEstado())) {
            throw new ErrorJuego(HttpStatus.CONFLICT, "PARTIDA_NO_EN_JUEGO",
                    "La partida no está en juego");
        }
        if (partidas.conCierrePendiente(partidaId, ahora)) {
            throw new ErrorJuego(HttpStatus.CONFLICT, "DISPUTAS_PENDIENTES",
                    "Hay una disputa abierta o una eliminación que aún se puede disputar");
        }
        cerrar(partida, POR_MASTER, ahora);
        return servicioPartida.vistaResumen(partida, cuentaId);
    }

    private void cerrar(Partida partida, String motivo, Instant ahora) {
        List<JugadorPartidaRepository.Ganador> vivos = jugadores.vivos(partida.getId());
        if (vivos.isEmpty()) {
            throw new IllegalStateException("La partida no tiene jugadores vivos");
        }
        String tipo;
        List<JugadorPartidaRepository.Ganador> ganadores;
        int eliminaciones;
        if (vivos.size() == 1) {
            tipo = "SUPERVIVIENTE";
            ganadores = vivos;
            eliminaciones = vivos.get(0).eliminaciones();
        } else {
            int maximo = vivos.stream()
                    .mapToInt(JugadorPartidaRepository.Ganador::eliminaciones)
                    .max().orElse(0);
            ganadores = vivos.stream()
                    .filter(jugador -> jugador.eliminaciones() == maximo)
                    .toList();
            tipo = ganadores.size() == 1 ? "GANADOR" : "EMPATE";
            eliminaciones = maximo;
        }
        partida.cerrar(ahora);
        partidas.saveAndFlush(partida);
        eventos.guardar(partida.getId(), "PARTIDA_CERRADA",
                datos(motivo, tipo, ganadores, eliminaciones), ahora);
    }

    private static String datos(String motivo, String tipo,
            List<JugadorPartidaRepository.Ganador> ganadores, int eliminaciones) {
        String ids = ganadores.stream()
                .map(ganador -> "\"" + ganador.inscripcionId() + "\"")
                .collect(Collectors.joining(",", "[", "]"));
        return "{\"motivo\":\"" + motivo + "\",\"resultado\":\"" + tipo
                + "\",\"ganadores\":" + ids + ",\"eliminaciones\":" + eliminaciones + "}";
    }
}
