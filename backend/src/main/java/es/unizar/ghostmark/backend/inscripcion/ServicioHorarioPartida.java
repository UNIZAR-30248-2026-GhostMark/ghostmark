package es.unizar.ghostmark.backend.inscripcion;

import es.unizar.ghostmark.backend.cuenta.CuentaRepository;
import es.unizar.ghostmark.backend.cuenta.dto.Horario;
import es.unizar.ghostmark.backend.cuenta.services.ServicioHorario;
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
public class ServicioHorarioPartida {

    private final CuentaRepository cuentas;
    private final PartidaRepository partidas;
    private final InscripcionRepository inscripciones;
    private final ServicioHorario validacion;
    private final Clock reloj;

    public ServicioHorarioPartida(CuentaRepository cuentas, PartidaRepository partidas,
            InscripcionRepository inscripciones, ServicioHorario validacion, Clock reloj) {
        this.cuentas = cuentas;
        this.partidas = partidas;
        this.inscripciones = inscripciones;
        this.validacion = validacion;
        this.reloj = reloj;
    }

    @Transactional
    public Horario obtener(UUID cuentaId, UUID partidaId) {
        comprobarCuenta(cuentaId);
        UUID inscripcionId = inscripciones.idDeCuentaEnPartida(partidaId, cuentaId)
                .orElseThrow(PartidaNoEncontradaException::new);
        return inscripciones.horario(inscripcionId);
    }

    @Transactional
    public Horario guardar(UUID cuentaId, UUID partidaId, Horario horario) {
        validacion.validar(horario);
        comprobarCuenta(cuentaId);
        Partida partida = partidas.bloquearPorId(partidaId)
                .orElseThrow(PartidaNoEncontradaException::new);
        UUID inscripcionId = inscripciones.idDeCuentaEnPartida(partidaId, cuentaId)
                .orElseThrow(PartidaNoEncontradaException::new);
        if (!"EN_INSCRIPCION".equals(partida.getEstado())
                || !Instant.now(reloj).isBefore(partida.getInicioEn())) {
            throw new ErrorInscripcion(HttpStatus.CONFLICT, "PARTIDA_NO_EDITABLE",
                    "El horario de la partida ya no se puede modificar");
        }
        inscripciones.reemplazarHorario(inscripcionId, horario);
        return inscripciones.horario(inscripcionId);
    }

    private void comprobarCuenta(UUID cuentaId) {
        if (cuentas.bloquearPorId(cuentaId).isEmpty()) {
            throw new ErrorInscripcion(HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO",
                    "La cuenta no está disponible");
        }
    }
}
