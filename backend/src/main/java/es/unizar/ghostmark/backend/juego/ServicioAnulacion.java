package es.unizar.ghostmark.backend.juego;

import es.unizar.ghostmark.backend.disputa.EliminacionAnulable;
import es.unizar.ghostmark.backend.disputa.ServicioDisputa;
import es.unizar.ghostmark.backend.evento.ServicioEvento;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
public class ServicioAnulacion {

    private static final String NO_ANULADA = "not exists (select 1 from evento a"
            + " where a.partida_id = e.partida_id and a.tipo = 'ELIMINACION_ANULADA'"
            + " and a.datos->'anuladas' @> jsonb_build_array(e.id::text))";

    private static final RowMapper<Eliminacion> FILA =
            (fila, numero) -> new Eliminacion(fila.getObject("id", UUID.class),
                    fila.getLong("secuencia"), fila.getObject("cazador", UUID.class),
                    fila.getObject("victima", UUID.class));

    private final JdbcTemplate jdbc;
    private final JugadorPartidaRepository jugadores;
    private final GeneradorCiclo ciclo;
    private final ServicioMisionesAnulacion misiones;
    private final ServicioDisputa disputas;
    private final ServicioEvento eventos;

    public ServicioAnulacion(JdbcTemplate jdbc, JugadorPartidaRepository jugadores,
            GeneradorCiclo ciclo, ServicioMisionesAnulacion misiones, ServicioDisputa disputas,
            ServicioEvento eventos) {
        this.jdbc = jdbc;
        this.jugadores = jugadores;
        this.ciclo = ciclo;
        this.misiones = misiones;
        this.disputas = disputas;
        this.eventos = eventos;
    }

    @EventListener
    public void alAnular(EliminacionAnulable anulable) {
        anular(anulable.partidaId(), anulable.eventoEliminacionId(), anulable.ahora());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void anular(UUID partidaId, UUID eventoEliminacionId, Instant ahora) {
        Optional<Eliminacion> disputada = eliminacion(partidaId, eventoEliminacionId);
        if (disputada.isEmpty()) {
            return;
        }
        UUID cazadorId = disputada.get().cazadorId();
        List<Eliminacion> anuladas = cadena(partidaId, cazadorId, disputada.get().secuencia());
        List<UUID> revividos = new ArrayList<>();
        for (Eliminacion anulada : anuladas) {
            jugadores.revivir(partidaId, anulada.victimaId());
            revividos.add(anulada.victimaId());
        }
        jugadores.restarEliminaciones(partidaId, cazadorId, anuladas.size());
        disputas.cerrarPorAnulacion(partidaId, anuladas.stream()
                .map(Eliminacion::eventoId).filter(id -> !id.equals(eventoEliminacionId))
                .toList(), ahora);

        boolean cazadorVivo = jugadores.obtener(partidaId, cazadorId)
                .map(estado -> "VIVO".equals(estado.estado())).orElse(false);
        List<UUID> orden = ciclo.formarEvitando(jugadores.idsVivos(partidaId),
                cazadorVivo ? cazadorId : null, new LinkedHashSet<>(revividos));
        jugadores.fijarCiclo(partidaId, orden);
        final List<ServicioMisionesAnulacion.Asignacion> nuevas = misiones.sustituir(partidaId,
                cazadorId, revividos);

        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("eventoEliminacionId", eventoEliminacionId.toString());
        datos.put("anuladas", anuladas.stream().map(a -> a.eventoId().toString()).toList());
        datos.put("revividos", revividos.stream().map(UUID::toString).toList());
        datos.put("orden", orden.stream().map(UUID::toString).toList());
        datos.put("misiones", nuevas.stream().map(n -> Map.of(
                "inscripcionId", n.inscripcionId().toString(),
                "misionId", n.misionId().toString())).toList());
        eventos.guardar(partidaId, "ELIMINACION_ANULADA",
                JsonMapper.shared().writeValueAsString(datos), ahora);
        disputas.cerrarNegativasSinObjeto(partidaId, ahora);
    }

    private Optional<Eliminacion> eliminacion(UUID partidaId, UUID eventoId) {
        return jdbc.query("select e.id, e.secuencia, (e.datos->>'cazadorId')::uuid as cazador,"
                        + " (e.datos->>'victimaId')::uuid as victima from evento e"
                        + " where e.partida_id = ? and e.id = ? and e.tipo = 'JUGADOR_ELIMINADO'"
                        + " and " + NO_ANULADA,
                FILA, partidaId, eventoId).stream().findFirst();
    }

    private List<Eliminacion> cadena(UUID partidaId, UUID cazadorId, long desde) {
        return jdbc.query("select e.id, e.secuencia, (e.datos->>'cazadorId')::uuid as cazador,"
                        + " (e.datos->>'victimaId')::uuid as victima from evento e"
                        + " where e.partida_id = ? and e.tipo = 'JUGADOR_ELIMINADO'"
                        + " and e.datos->>'cazadorId' = ? and e.secuencia >= ?"
                        + " and " + NO_ANULADA + " order by e.secuencia",
                FILA, partidaId, cazadorId.toString(), desde);
    }

    private record Eliminacion(UUID eventoId, long secuencia, UUID cazadorId, UUID victimaId) {
    }
}
