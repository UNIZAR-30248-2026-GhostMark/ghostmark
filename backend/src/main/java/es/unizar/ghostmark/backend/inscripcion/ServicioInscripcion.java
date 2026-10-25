package es.unizar.ghostmark.backend.inscripcion;

import es.unizar.ghostmark.backend.cuenta.CuentaRepository;
import es.unizar.ghostmark.backend.evento.ServicioEvento;
import es.unizar.ghostmark.backend.partida.Partida;
import es.unizar.ghostmark.backend.partida.PartidaNoEncontradaException;
import es.unizar.ghostmark.backend.partida.PartidaRepository;
import es.unizar.ghostmark.backend.sesion.DatosNoValidosException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioInscripcion {

    private final InscripcionRepository inscripciones;
    private final PartidaRepository partidas;
    private final CuentaRepository cuentas;
    private final ServicioEvento eventos;

    public ServicioInscripcion(InscripcionRepository inscripciones, PartidaRepository partidas,
            CuentaRepository cuentas, ServicioEvento eventos) {
        this.inscripciones = inscripciones;
        this.partidas = partidas;
        this.cuentas = cuentas;
        this.eventos = eventos;
    }

    @Transactional
    public Map<String, Object> inscribir(UUID cuentaId, NuevaInscripcion peticion, Instant ahora) {
        comprobarCuenta(cuentaId);
        if (peticion.codigo() == null || !peticion.codigo().matches("^[2-9A-HJKMNP-Z]{8}$")) {
            throw new DatosNoValidosException(Map.of("codigo", "Debe ser un código de invitación"));
        }
        if (peticion.aceptaNormas() == null) {
            throw new DatosNoValidosException(Map.of("aceptaNormas", "Es obligatorio"));
        }
        if (!peticion.aceptaNormas()) {
            throw new ErrorInscripcion(HttpStatus.BAD_REQUEST, "NORMAS_NO_ACEPTADAS",
                    "Debe aceptar las normas para inscribirse");
        }
        Partida partida = partidas.bloquearPorCodigo(peticion.codigo())
                .orElseThrow(PartidaNoEncontradaException::new);
        if (!"EN_INSCRIPCION".equals(partida.getEstado())
                || !ahora.isBefore(partida.getInicioEn())) {
            throw new ErrorInscripcion(HttpStatus.CONFLICT, "PARTIDA_NO_ABIERTA",
                    "La partida ya no admite inscripciones");
        }
        if (cuentaId.equals(partida.getMasterId()) && !partida.isMasterJuega()) {
            throw new ErrorInscripcion(HttpStatus.CONFLICT, "ES_MASTER", "El máster no juega");
        }
        if (inscripciones.existe(partida.getId(), cuentaId)) {
            throw new ErrorInscripcion(HttpStatus.CONFLICT, "YA_INSCRITO", "Ya está inscrito");
        }
        UUID id = alta(partida.getId(), cuentaId, ahora);
        return Map.of("id", id.toString(), "partidaId", partida.getId().toString(),
                "inscritaEn", ahora.toString(), "horario", inscripciones.horario(id));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public UUID alta(UUID partidaId, UUID cuentaId, Instant ahora) {
        partidas.bloquearPorId(partidaId).orElseThrow(PartidaNoEncontradaException::new);
        UUID id = inscripciones.crear(partidaId, cuentaId, ahora);
        eventos.guardar(partidaId, "JUGADOR_INSCRITO",
                "{\"inscripcionId\":\"" + id + "\"}", ahora);
        return id;
    }

    @Transactional
    public List<Map<String, Object>> listar(UUID cuentaId, UUID partidaId) {
        comprobarCuenta(cuentaId);
        Partida partida = partidas.findById(partidaId)
                .orElseThrow(PartidaNoEncontradaException::new);
        if (!cuentaId.equals(partida.getMasterId()) && !inscripciones.existe(partidaId, cuentaId)) {
            throw new PartidaNoEncontradaException();
        }
        return inscripciones.listar(partidaId);
    }

    @Transactional
    public void bloquearPartidasDeCuenta(UUID cuentaId) {
        inscripciones.bloquearPartidasDeCuenta(cuentaId);
    }

    @Transactional
    public void borrarPorCuenta(UUID cuentaId, Instant ahora) {
        for (InscripcionRepository.Retirada retirada : inscripciones.deCuenta(cuentaId)) {
            inscripciones.borrarHorario(retirada.id());
            if ("EN_JUEGO".equals(retirada.estado())) {
                inscripciones.desvincular(retirada.id());
            } else {
                eventos.guardar(retirada.partidaId(), "INSCRIPCION_RETIRADA",
                        "{\"inscripcionId\":\"" + retirada.id()
                                + "\",\"motivo\":\"CUENTA_BORRADA\"}", ahora);
                inscripciones.borrar(retirada.id());
            }
        }
    }

    private void comprobarCuenta(UUID cuentaId) {
        if (cuentas.bloquearPorId(cuentaId).isEmpty()) {
            throw new ErrorInscripcion(HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO",
                    "La cuenta no está disponible");
        }
    }
}
