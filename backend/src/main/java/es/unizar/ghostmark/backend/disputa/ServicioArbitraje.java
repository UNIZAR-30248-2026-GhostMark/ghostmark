package es.unizar.ghostmark.backend.disputa;

import es.unizar.ghostmark.backend.evento.ServicioEvento;
import es.unizar.ghostmark.backend.juego.ErrorJuego;
import es.unizar.ghostmark.backend.juego.ServicioEliminacion;
import es.unizar.ghostmark.backend.partida.Partida;
import es.unizar.ghostmark.backend.partida.PartidaNoEncontradaException;
import es.unizar.ghostmark.backend.partida.PartidaRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** Arbitraje de la 11a.3. La apertura y la respuesta pertenecen a la 11a.2. */
@Service
public class ServicioArbitraje {

    private static final String ABIERTA = "ABIERTA";
    private static final Set<String> RAZONES = Set.of("CAZADOR", "VICTIMA");

    private final JdbcTemplate jdbc;
    private final PartidaRepository partidas;
    private final DisputaRepository disputas;
    private final VistaDisputa vista;
    private final ServicioEliminacion eliminacion;
    private final ServicioEvento eventos;
    private final ApplicationEventPublisher publicador;
    private final Clock reloj;
    private final SecureRandom aleatorio = new SecureRandom();

    public ServicioArbitraje(JdbcTemplate jdbc, PartidaRepository partidas,
            DisputaRepository disputas, VistaDisputa vista, ServicioEliminacion eliminacion,
            ServicioEvento eventos, ApplicationEventPublisher publicador, Clock reloj) {
        this.jdbc = jdbc;
        this.partidas = partidas;
        this.disputas = disputas;
        this.vista = vista;
        this.eliminacion = eliminacion;
        this.eventos = eventos;
        this.publicador = publicador;
        this.reloj = reloj;
    }

    /**
     * Al abrir una disputa (11a.2 publica {@link DisputaAbierta} dentro de su transacción, con la
     * fila de la partida bloqueada): si la eliminación se guardó con la zona {@code DENTRO}, queda
     * resuelta a favor de la víctima; si no, se sortea árbitro cuando el máster está implicado o
     * ha borrado su cuenta.
     */
    @EventListener
    public void alAbrir(DisputaAbierta abierta) {
        resolverPorZona(abierta.partidaId(), abierta.disputaId(), abierta.ahora());
        asignarInicial(abierta.partidaId(), abierta.disputaId(), abierta.ahora());
    }

    void asignarInicial(UUID partidaId, UUID disputaId, Instant ahora) {
        Partida partida = partida(partidaId);
        DisputaRepository.Fila disputa = fila(partidaId, disputaId);
        if (!ABIERTA.equals(disputa.estado())) {
            return;
        }
        UUID master = partida.getMasterId();
        UUID inscritoMaster = master == null ? null : jdbc.queryForList(
                "select id from inscripcion where partida_id = ? and cuenta_id = ?",
                UUID.class, partidaId, master).stream().findFirst().orElse(null);
        if (master == null || inscritoMaster != null
                && (inscritoMaster.equals(disputa.cazadorId())
                || inscritoMaster.equals(disputa.victimaId()))) {
            sortear(partida, disputa, ahora, "MASTER_IMPLICADO");
        }
    }

    /** Reasignacion al vencer; la llama la tarea y puede llamarse antes de responder. */
    @Transactional
    public void revisarVencimiento(UUID partidaId, UUID disputaId, Instant ahora) {
        Partida partida = partida(partidaId);
        DisputaRepository.Fila disputa = fila(partidaId, disputaId);
        if (ABIERTA.equals(disputa.estado()) && !ahora.isBefore(disputa.plazoHasta())) {
            sortear(partida, disputa, ahora, "PLAZO_VENCIDO");
        }
    }

    /** Revisa solo la partida visitada antes de servir la peticion HTTP. */
    @Transactional
    public void revisarPartida(UUID partidaId, Instant ahora) {
        List<UUID> ids = jdbc.queryForList("select id from disputa"
                        + " where partida_id = ? and estado = 'ABIERTA' and plazo_hasta <= ?"
                        + " order by id", UUID.class, partidaId, fechaSql(ahora));
        for (UUID id : ids) {
            revisarVencimiento(partidaId, id, ahora);
        }
    }

    @Transactional(readOnly = true)
    public List<Pendiente> vencidas(Instant ahora) {
        return jdbc.query("select partida_id, id from disputa"
                        + " where estado = 'ABIERTA' and plazo_hasta <= ?"
                        + " order by partida_id, id",
                (fila, numero) -> new Pendiente(
                        fila.getObject("partida_id", UUID.class),
                        fila.getObject("id", UUID.class)), fechaSql(ahora));
    }

    /** Decide sin esperar a la respuesta del otro implicado (R12-26). */
    @Transactional(noRollbackFor = ErrorJuego.class)
    public Map<String, Object> decidir(UUID cuentaId, UUID partidaId, UUID disputaId,
            String razon) {
        if (!RAZONES.contains(razon)) {
            throw new ErrorJuego(HttpStatus.BAD_REQUEST, "DATOS_NO_VALIDOS",
                    "La razon debe ser CAZADOR o VICTIMA");
        }
        Partida partida = partida(partidaId);
        DisputaRepository.Fila disputa = fila(partidaId, disputaId);
        boolean arbitro = vista.esArbitro(cuentaId, partida, disputa);
        if (!arbitro && vista.inscripcion(cuentaId, disputa) == null) {
            throw new PartidaNoEncontradaException();
        }
        if (!ABIERTA.equals(disputa.estado())) {
            throw new ErrorJuego(HttpStatus.CONFLICT, "DISPUTA_NO_ABIERTA",
                    "La disputa ya no esta abierta");
        }
        Instant ahora = Instant.now(reloj);
        if (!ahora.isBefore(disputa.plazoHasta())) {
            sortear(partida, disputa, ahora, "PLAZO_VENCIDO");
            throw new ErrorJuego(HttpStatus.FORBIDDEN, "NO_ES_ARBITRO",
                    "El plazo del arbitro ha vencido");
        }
        if (!arbitro) {
            throw new ErrorJuego(HttpStatus.FORBIDDEN, "NO_ES_ARBITRO",
                    "Solo el arbitro actual puede decidir");
        }
        if ("NEGATIVA".equals(disputa.tipo()) && "CAZADOR".equals(razon)) {
            comprobarNegativa(partidaId, disputa);
        }
        String origen = disputa.arbitroId() == null ? "MASTER" : "SORTEADO";
        resolver(disputa, razon, origen, ahora);
        if ("ELIMINACION".equals(disputa.tipo()) && "VICTIMA".equals(razon)) {
            publicador.publishEvent(new EliminacionAnulable(partidaId,
                    disputa.eventoEliminacionId(), ahora));
        } else if ("NEGATIVA".equals(disputa.tipo()) && "CAZADOR".equals(razon)) {
            eliminacion.eliminarPorDisputa(partidaId, disputa.cazadorId(), disputa.victimaId(),
                    ahora);
        }
        return vista.de(cuentaId, partida, fila(partidaId, disputaId));
    }

    /** Resolución automática solo para un resultado de zona DENTRO (R12-24). */
    void resolverPorZona(UUID partidaId, UUID disputaId, Instant ahora) {
        partida(partidaId);
        DisputaRepository.Fila disputa = fila(partidaId, disputaId);
        if (!ABIERTA.equals(disputa.estado()) || !"ELIMINACION".equals(disputa.tipo())
                || !"DENTRO".equals(resultadoZona(disputa))) {
            return;
        }
        resolver(disputa, "VICTIMA", "ZONA", ahora);
        publicador.publishEvent(new EliminacionAnulable(partidaId,
                disputa.eventoEliminacionId(), ahora));
    }

    private void sortear(Partida partida, DisputaRepository.Fila disputa, Instant ahora,
            String motivo) {
        List<UUID> candidatos = jdbc.queryForList("select i.id from inscripcion i"
                        + " where i.partida_id = ? and i.cuenta_id is not null"
                        + " and i.id <> ? and i.id <> ?"
                        + " and (i.cuenta_id <> ?::uuid or ?::uuid is null)"
                        + " order by i.id", UUID.class, partida.getId(), disputa.cazadorId(),
                disputa.victimaId(), partida.getMasterId(), partida.getMasterId());
        Set<UUID> anteriores = new HashSet<>(jdbc.queryForList(
                "select (datos->>'arbitroId')::uuid from evento"
                        + " where partida_id = ? and tipo = 'DISPUTA_ARBITRO_SORTEADO'"
                        + " and datos->>'disputaId' = ? and datos->>'arbitroId' is not null",
                UUID.class, partida.getId(), disputa.id().toString()));
        List<UUID> nuevos = new ArrayList<>(candidatos);
        nuevos.removeAll(anteriores);
        List<UUID> bolsa = nuevos.isEmpty() ? candidatos : nuevos;
        UUID elegido = bolsa.isEmpty() ? null : bolsa.get(aleatorio.nextInt(bolsa.size()));
        Instant plazo = ahora.plusSeconds(partida.getPlazoDisputaHoras().longValue() * 3600);
        jdbc.update("update disputa set arbitro_id = ?, plazo_hasta = ? where id = ?",
                elegido, fechaSql(plazo), disputa.id());
        if (elegido != null) {
            eventos.guardar(partida.getId(), "DISPUTA_ARBITRO_SORTEADO",
                    json(Map.of("disputaId", disputa.id().toString(),
                            "arbitroId", elegido.toString(), "motivo", motivo,
                            "plazoHasta", plazo.toString())), ahora);
        }
    }

    private void resolver(DisputaRepository.Fila disputa, String razon, String origen,
            Instant ahora) {
        jdbc.update("update disputa set estado = 'RESUELTA', razon = ?,"
                        + " resuelta_por = ?, cerrada_en = ?"
                        + " where id = ? and estado = 'ABIERTA'",
                razon, origen, fechaSql(ahora), disputa.id());
        eventos.guardar(disputa.partidaId(), "DISPUTA_RESUELTA",
                json(Map.of("disputaId", disputa.id().toString(), "estado", "RESUELTA",
                        "razon", razon, "resueltaPor", origen)), ahora);
    }

    private void comprobarNegativa(UUID partidaId, DisputaRepository.Fila disputa) {
        Integer valido = jdbc.queryForObject("select count(*) from jugador_partida cazador"
                        + " join jugador_partida victima on victima.inscripcion_id = ?"
                        + " and victima.partida_id = cazador.partida_id"
                        + " where cazador.partida_id = ? and cazador.inscripcion_id = ?"
                        + " and cazador.estado = 'VIVO' and victima.estado = 'VIVO'"
                        + " and cazador.objetivo_id = victima.inscripcion_id",
                Integer.class, disputa.victimaId(), partidaId, disputa.cazadorId());
        if (valido == 0) {
            throw new ErrorJuego(HttpStatus.CONFLICT, "DISPUTA_NO_ABIERTA",
                    "La disputa por negativa ya no tiene objeto");
        }
    }

    private String resultadoZona(DisputaRepository.Fila disputa) {
        if (disputa.eventoEliminacionId() == null) {
            return null;
        }
        return jdbc.queryForList("select datos->>'resultadoZona' from evento where id = ?",
                String.class, disputa.eventoEliminacionId()).stream().findFirst().orElse(null);
    }

    private DisputaRepository.Fila fila(UUID partidaId, UUID disputaId) {
        return disputas.obtener(partidaId, disputaId, true)
                .orElseThrow(PartidaNoEncontradaException::new);
    }

    private Partida partida(UUID partidaId) {
        return partidas.bloquearPorId(partidaId)
                .orElseThrow(PartidaNoEncontradaException::new);
    }

    private static String json(Map<String, Object> datos) {
        return JsonMapper.shared().writeValueAsString(datos);
    }

    private static OffsetDateTime fechaSql(Instant instante) {
        return OffsetDateTime.ofInstant(instante, ZoneOffset.UTC);
    }

    public record Pendiente(UUID partidaId, UUID disputaId) {
    }
}
