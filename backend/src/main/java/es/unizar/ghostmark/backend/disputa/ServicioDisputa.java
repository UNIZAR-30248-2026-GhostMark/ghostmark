package es.unizar.ghostmark.backend.disputa;

import es.unizar.ghostmark.backend.cuenta.CuentaRepository;
import es.unizar.ghostmark.backend.evento.ServicioEvento;
import es.unizar.ghostmark.backend.inscripcion.InscripcionRepository;
import es.unizar.ghostmark.backend.juego.EliminacionHecha;
import es.unizar.ghostmark.backend.juego.ErrorJuego;
import es.unizar.ghostmark.backend.partida.Partida;
import es.unizar.ghostmark.backend.partida.PartidaNoEncontradaException;
import es.unizar.ghostmark.backend.partida.PartidaRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
public class ServicioDisputa {

    static final Duration PLAZO_PARA_DISPUTAR = Duration.ofMinutes(10);
    static final int MAXIMO_TEXTO = 280;
    private static final Set<String> TIPOS = Set.of("ELIMINACION", "NEGATIVA");

    private final CuentaRepository cuentas;
    private final PartidaRepository partidas;
    private final InscripcionRepository inscripciones;
    private final DisputaRepository disputas;
    private final VistaDisputa vista;
    private final ServicioEvento eventos;
    private final ApplicationEventPublisher publicador;
    private final Clock reloj;

    public ServicioDisputa(CuentaRepository cuentas, PartidaRepository partidas,
            InscripcionRepository inscripciones, DisputaRepository disputas, VistaDisputa vista,
            ServicioEvento eventos, ApplicationEventPublisher publicador, Clock reloj) {
        this.cuentas = cuentas;
        this.partidas = partidas;
        this.inscripciones = inscripciones;
        this.disputas = disputas;
        this.vista = vista;
        this.eventos = eventos;
        this.publicador = publicador;
        this.reloj = reloj;
    }

    @Transactional
    public Map<String, Object> abrir(UUID cuentaId, UUID partidaId, NuevaDisputa peticion) {
        final String texto = texto(peticion.texto());
        if (peticion.tipo() == null || !TIPOS.contains(peticion.tipo())) {
            throw datosNoValidos("tipo", "Debe ser ELIMINACION o NEGATIVA");
        }
        comprobarCuenta(cuentaId);
        Partida partida = partidas.bloquearPorId(partidaId)
                .orElseThrow(PartidaNoEncontradaException::new);
        UUID inscripcionId = inscripciones.idDeCuentaEnPartida(partidaId, cuentaId)
                .orElseThrow(PartidaNoEncontradaException::new);
        if (!"EN_JUEGO".equals(partida.getEstado())) {
            throw conflicto("PARTIDA_NO_EN_JUEGO", "La partida no está en juego");
        }
        Instant ahora = Instant.now(reloj);
        DisputaRepository.Jugador jugador = disputas.jugador(partidaId, inscripcionId)
                .orElseThrow(() -> new IllegalStateException("La partida no tiene al jugador"));
        DisputaRepository.Fila nueva = "ELIMINACION".equals(peticion.tipo())
                ? porEliminacion(partidaId, inscripcionId, jugador, texto, partida, ahora)
                : porNegativa(partidaId, inscripcionId, jugador, texto, partida, ahora);
        disputas.crear(nueva);
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("disputaId", nueva.id().toString());
        datos.put("tipo", nueva.tipo());
        datos.put("cazadorId", nueva.cazadorId().toString());
        datos.put("victimaId", nueva.victimaId().toString());
        datos.put("eventoEliminacionId", nueva.eventoEliminacionId() == null ? null
                : nueva.eventoEliminacionId().toString());
        datos.put("texto", texto);
        datos.put("arbitroId", null);
        datos.put("plazoHasta", nueva.plazoHasta().toString());
        eventos.guardar(partidaId, "DISPUTA_ABIERTA", JsonMapper.shared().writeValueAsString(datos),
                ahora);
        publicador.publishEvent(new DisputaAbierta(partidaId, nueva.id(), ahora));
        return vista.de(cuentaId, partida, disputas.obtener(partidaId, nueva.id(), false)
                .orElseThrow());
    }

    @Transactional
    public Map<String, Object> responder(UUID cuentaId, UUID partidaId, UUID disputaId,
            TextoDisputa peticion) {
        final String texto = texto(peticion.texto());
        comprobarCuenta(cuentaId);
        Partida partida = partidas.bloquearPorId(partidaId)
                .orElseThrow(PartidaNoEncontradaException::new);
        DisputaRepository.Fila disputa = disputas.obtener(partidaId, disputaId, true)
                .orElseThrow(PartidaNoEncontradaException::new);
        UUID implicado = vista.inscripcion(cuentaId, disputa);
        boolean arbitro = vista.esArbitro(cuentaId, partida, disputa);
        if (implicado == null && !arbitro) {
            throw new PartidaNoEncontradaException();
        }
        UUID responde = "ELIMINACION".equals(disputa.tipo()) ? disputa.cazadorId()
                : disputa.victimaId();
        if (!responde.equals(implicado)) {
            throw new ErrorJuego(HttpStatus.FORBIDDEN, "NO_PUEDE_RESPONDER",
                    "Solo responde el otro implicado");
        }
        if (!"ABIERTA".equals(disputa.estado())) {
            throw conflicto("DISPUTA_NO_ABIERTA", "La disputa ya no está abierta");
        }
        if (disputa.textoRespuesta() != null) {
            throw conflicto("YA_RESPONDIDA", "La disputa ya tiene respuesta");
        }
        Instant ahora = Instant.now(reloj);
        disputas.responder(disputaId, texto, ahora);
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("disputaId", disputaId.toString());
        datos.put("texto", texto);
        eventos.guardar(partidaId, "DISPUTA_RESPONDIDA",
                JsonMapper.shared().writeValueAsString(datos), ahora);
        return vista.de(cuentaId, partida, disputas.obtener(partidaId, disputaId, false)
                .orElseThrow());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listar(UUID cuentaId, UUID partidaId) {
        Partida partida = partidas.findById(partidaId)
                .orElseThrow(PartidaNoEncontradaException::new);
        if (!cuentaId.equals(partida.getMasterId())
                && inscripciones.idDeCuentaEnPartida(partidaId, cuentaId).isEmpty()) {
            throw new PartidaNoEncontradaException();
        }
        List<Map<String, Object>> lista = new ArrayList<>();
        for (UUID id : disputas.visibles(partidaId, cuentaId, partida.getMasterId())) {
            lista.add(vista.de(cuentaId, partida, disputas.obtener(partidaId, id, false)
                    .orElseThrow()));
        }
        return lista;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtener(UUID cuentaId, UUID partidaId, UUID disputaId) {
        Partida partida = partidas.findById(partidaId)
                .orElseThrow(PartidaNoEncontradaException::new);
        DisputaRepository.Fila disputa = disputas.obtener(partidaId, disputaId, false)
                .orElseThrow(PartidaNoEncontradaException::new);
        if (vista.inscripcion(cuentaId, disputa) == null
                && !vista.esArbitro(cuentaId, partida, disputa)) {
            throw new PartidaNoEncontradaException();
        }
        return vista.de(cuentaId, partida, disputa);
    }

    @EventListener
    public void alEliminar(EliminacionHecha hecha) {
        for (UUID id : disputas.negativasSinObjeto(hecha.partidaId())) {
            disputas.sinObjeto(id, hecha.ahora());
            Map<String, Object> datos = new LinkedHashMap<>();
            datos.put("disputaId", id.toString());
            datos.put("estado", "SIN_OBJETO");
            datos.put("razon", null);
            datos.put("resueltaPor", null);
            eventos.guardar(hecha.partidaId(), "DISPUTA_RESUELTA",
                    JsonMapper.shared().writeValueAsString(datos), hecha.ahora());
        }
    }

    private DisputaRepository.Fila porEliminacion(UUID partidaId, UUID inscripcionId,
            DisputaRepository.Jugador jugador, String texto, Partida partida, Instant ahora) {
        DisputaRepository.Eliminacion eliminacion = "ELIMINADO".equals(jugador.estado())
                ? disputas.ultimaEliminacion(partidaId, inscripcionId).orElse(null) : null;
        if (eliminacion == null || !"CODIGO".equals(eliminacion.origen())
                || disputas.disputada(eliminacion.eventoId())) {
            throw conflicto("ELIMINACION_NO_DISPUTABLE", "No hay una eliminación que disputar");
        }
        if (!ahora.isBefore(eliminacion.ocurridoEn().plus(PLAZO_PARA_DISPUTAR))) {
            throw conflicto("PLAZO_DISPUTA_VENCIDO",
                    "Han pasado los 10 minutos para disputar la eliminación");
        }
        return nueva(partidaId, "ELIMINACION", eliminacion.cazadorId(), inscripcionId,
                eliminacion.eventoId(), texto, partida, ahora);
    }

    private DisputaRepository.Fila porNegativa(UUID partidaId, UUID inscripcionId,
            DisputaRepository.Jugador jugador, String texto, Partida partida, Instant ahora) {
        if (!"VIVO".equals(jugador.estado()) || jugador.objetivoId() == null) {
            throw conflicto("JUGADOR_ELIMINADO", "El jugador no tiene objetivo");
        }
        if (!ahora.isBefore(partida.getFinEn())) {
            throw conflicto("PARTIDA_NO_EN_JUEGO", "La partida ha llegado a su fecha de fin");
        }
        if (disputas.negativaAbierta(inscripcionId)) {
            throw conflicto("DISPUTA_YA_ABIERTA", "Ya tienes una disputa por negativa abierta");
        }
        return nueva(partidaId, "NEGATIVA", inscripcionId, jugador.objetivoId(), null, texto,
                partida, ahora);
    }

    private static DisputaRepository.Fila nueva(UUID partidaId, String tipo, UUID cazadorId,
            UUID victimaId, UUID eventoId, String texto, Partida partida, Instant ahora) {
        Instant plazo = ahora.plus(Duration.ofHours(partida.getPlazoDisputaHoras()));
        return new DisputaRepository.Fila(UUID.randomUUID(), partidaId, tipo, "ABIERTA",
                cazadorId, victimaId, null, eventoId, texto, null, ahora, plazo, null, null,
                null);
    }

    private void comprobarCuenta(UUID cuentaId) {
        if (cuentas.bloquearPorId(cuentaId).isEmpty()) {
            throw new ErrorJuego(HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO",
                    "La cuenta no está disponible");
        }
    }

    private static String texto(String texto) {
        String limpio = texto == null ? "" : texto.strip();
        int longitud = limpio.codePointCount(0, limpio.length());
        if (longitud < 1 || longitud > MAXIMO_TEXTO) {
            throw datosNoValidos("texto", "Debe tener de 1 a 280 caracteres");
        }
        return limpio;
    }

    private static ErrorJuego conflicto(String codigo, String mensaje) {
        return new ErrorJuego(HttpStatus.CONFLICT, codigo, mensaje);
    }

    private static ErrorJuego datosNoValidos(String campo, String mensaje) {
        return new ErrorJuego(HttpStatus.BAD_REQUEST, "DATOS_NO_VALIDOS",
                "Algún campo no cumple sus reglas",
                Map.of("errores", List.of(Map.of("campo", campo, "mensaje", mensaje))));
    }
}
