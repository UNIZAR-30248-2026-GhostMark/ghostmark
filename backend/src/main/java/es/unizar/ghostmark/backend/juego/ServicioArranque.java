package es.unizar.ghostmark.backend.juego;

import es.unizar.ghostmark.backend.cuenta.CuentaRepository;
import es.unizar.ghostmark.backend.evento.ServicioEvento;
import es.unizar.ghostmark.backend.inscripcion.ErrorInscripcion;
import es.unizar.ghostmark.backend.inscripcion.InscripcionRepository;
import es.unizar.ghostmark.backend.partida.Partida;
import es.unizar.ghostmark.backend.partida.PartidaNoEncontradaException;
import es.unizar.ghostmark.backend.partida.PartidaRepository;
import es.unizar.ghostmark.backend.partida.ServicioPartida;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioArranque {

    private static final int MINIMO_JUGADORES = 3;

    private final CuentaRepository cuentas;
    private final PartidaRepository partidas;
    private final InscripcionRepository inscripciones;
    private final ServicioEvento eventos;
    private final ServicioPartida servicioPartida;

    public ServicioArranque(CuentaRepository cuentas, PartidaRepository partidas,
            InscripcionRepository inscripciones, ServicioEvento eventos,
            ServicioPartida servicioPartida) {
        this.cuentas = cuentas;
        this.partidas = partidas;
        this.inscripciones = inscripciones;
        this.eventos = eventos;
        this.servicioPartida = servicioPartida;
    }

    @Transactional
    public boolean arrancarSiToca(UUID partidaId, Instant ahora) {
        Partida partida = partidas.bloquearPorId(partidaId).orElse(null);
        if (partida == null || !"EN_INSCRIPCION".equals(partida.getEstado())
                || ahora.isBefore(partida.getInicioEn())) {
            return false;
        }
        int jugadores = inscripciones.contar(partidaId);
        if (jugadores < MINIMO_JUGADORES) {
            return false;
        }
        arrancar(partida, "PROGRAMADO", jugadores, ahora);
        return true;
    }

    @Transactional
    public Map<String, Object> arrancarYa(UUID cuentaId, UUID partidaId, Instant ahora) {
        if (cuentas.bloquearPorId(cuentaId).isEmpty()) {
            throw new ErrorInscripcion(HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO",
                    "La cuenta no está disponible");
        }
        Partida partida = partidas.bloquearPorId(partidaId)
                .orElseThrow(PartidaNoEncontradaException::new);
        if (!cuentaId.equals(partida.getMasterId())) {
            if (inscripciones.existe(partidaId, cuentaId)) {
                throw new ErrorInscripcion(HttpStatus.FORBIDDEN, "NO_ES_MASTER",
                        "Solo el máster puede arrancar la partida");
            }
            throw new PartidaNoEncontradaException();
        }
        if (!"EN_INSCRIPCION".equals(partida.getEstado())) {
            throw new ErrorInscripcion(HttpStatus.CONFLICT, "PARTIDA_NO_ABIERTA",
                    "La partida ya no está en inscripción");
        }
        int jugadores = inscripciones.contar(partidaId);
        if (jugadores < MINIMO_JUGADORES) {
            throw new ErrorInscripcion(HttpStatus.CONFLICT, "POCOS_JUGADORES",
                    "Hacen falta al menos 3 jugadores");
        }
        arrancar(partida, "ADELANTADO", jugadores, ahora);
        return servicioPartida.vistaResumen(partida, cuentaId);
    }

    private void arrancar(Partida partida, String modo, int jugadores, Instant ahora) {
        partida.arrancar(ahora);
        eventos.guardar(partida.getId(), "PARTIDA_ARRANCADA",
                "{\"modo\":\"" + modo + "\",\"numJugadores\":" + jugadores + "}", ahora);
    }
}
