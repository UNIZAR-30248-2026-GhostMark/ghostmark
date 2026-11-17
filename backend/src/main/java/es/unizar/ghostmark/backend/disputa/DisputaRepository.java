package es.unizar.ghostmark.backend.disputa;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class DisputaRepository {

    private static final RowMapper<Fila> FILA = (fila, numero) -> new Fila(
            fila.getObject("id", UUID.class), fila.getObject("partida_id", UUID.class),
            fila.getString("tipo"), fila.getString("estado"),
            fila.getObject("cazador_id", UUID.class), fila.getObject("victima_id", UUID.class),
            fila.getObject("arbitro_id", UUID.class),
            fila.getObject("evento_eliminacion_id", UUID.class),
            fila.getString("texto_apertura"), fila.getString("texto_respuesta"),
            instante(fila.getObject("abierta_en", OffsetDateTime.class)),
            instante(fila.getObject("plazo_hasta", OffsetDateTime.class)),
            fila.getString("razon"), fila.getString("resuelta_por"),
            instante(fila.getObject("cerrada_en", OffsetDateTime.class)));

    private final JdbcTemplate jdbc;

    public DisputaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Fila> obtener(UUID partidaId, UUID disputaId, boolean bloquear) {
        return jdbc.query("select * from disputa where partida_id = ? and id = ?"
                + (bloquear ? " for update" : ""), FILA, partidaId, disputaId).stream()
                .findFirst();
    }

    public List<UUID> visibles(UUID partidaId, UUID cuentaId, UUID masterId) {
        return jdbc.queryForList("select d.id from disputa d"
                + " join inscripcion ic on ic.id = d.cazador_id"
                + " join inscripcion iv on iv.id = d.victima_id"
                + " left join inscripcion ia on ia.id = d.arbitro_id"
                + " where d.partida_id = ? and (ic.cuenta_id = ? or iv.cuenta_id = ?"
                + " or ia.cuenta_id = ? or (d.arbitro_id is null and ?::uuid = ?::uuid))"
                + " order by d.abierta_en desc, d.id", UUID.class, partidaId, cuentaId, cuentaId,
                cuentaId, cuentaId, masterId);
    }

    public Optional<Jugador> jugador(UUID partidaId, UUID inscripcionId) {
        return jdbc.query("select estado, objetivo_id from jugador_partida"
                + " where partida_id = ? and inscripcion_id = ? for update",
                (fila, numero) -> new Jugador(fila.getString("estado"),
                        fila.getObject("objetivo_id", UUID.class)),
                partidaId, inscripcionId).stream().findFirst();
    }

    public Optional<Eliminacion> ultimaEliminacion(UUID partidaId, UUID victimaId) {
        return jdbc.query("select id, ocurrido_en, (datos->>'cazadorId')::uuid as cazador,"
                + " coalesce(datos->>'origen', 'CODIGO') as origen from evento"
                + " where partida_id = ? and tipo = 'JUGADOR_ELIMINADO'"
                + " and datos->>'victimaId' = ? order by secuencia desc limit 1",
                (fila, numero) -> new Eliminacion(fila.getObject("id", UUID.class),
                        fila.getObject("cazador", UUID.class),
                        instante(fila.getObject("ocurrido_en", OffsetDateTime.class)),
                        fila.getString("origen")),
                partidaId, victimaId.toString()).stream().findFirst();
    }

    public boolean disputada(UUID eventoId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from disputa"
                + " where evento_eliminacion_id = ?)", Boolean.class, eventoId));
    }

    public boolean bloqueaAlCazador(UUID cazadorId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from disputa"
                + " where cazador_id = ? and tipo = 'ELIMINACION' and estado = 'ABIERTA')",
                Boolean.class, cazadorId));
    }

    public boolean negativaAbierta(UUID cazadorId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from disputa"
                + " where cazador_id = ? and tipo = 'NEGATIVA' and estado = 'ABIERTA')",
                Boolean.class, cazadorId));
    }

    public void crear(Fila disputa) {
        jdbc.update("insert into disputa (id, partida_id, tipo, cazador_id, victima_id,"
                + " evento_eliminacion_id, texto_apertura, plazo_hasta, estado, abierta_en)"
                + " values (?, ?, ?, ?, ?, ?, ?, ?, 'ABIERTA', ?)", disputa.id(),
                disputa.partidaId(), disputa.tipo(), disputa.cazadorId(), disputa.victimaId(),
                disputa.eventoEliminacionId(), disputa.textoApertura(),
                fecha(disputa.plazoHasta()), fecha(disputa.abiertaEn()));
    }

    public void responder(UUID disputaId, String texto, Instant ahora) {
        jdbc.update("update disputa set texto_respuesta = ?, respondida_en = ? where id = ?",
                texto, fecha(ahora), disputaId);
    }

    public List<UUID> negativasSinObjeto(UUID partidaId) {
        return jdbc.queryForList("select d.id from disputa d where d.partida_id = ?"
                + " and d.tipo = 'NEGATIVA' and d.estado = 'ABIERTA' and not exists"
                + " (select 1 from jugador_partida jp where jp.inscripcion_id = d.cazador_id"
                + " and jp.estado = 'VIVO' and jp.objetivo_id = d.victima_id) order by d.id",
                UUID.class, partidaId);
    }

    public void sinObjeto(UUID disputaId, Instant ahora) {
        jdbc.update("update disputa set estado = 'SIN_OBJETO', cerrada_en = ? where id = ?",
                fecha(ahora), disputaId);
    }

    private static OffsetDateTime fecha(Instant instante) {
        return OffsetDateTime.ofInstant(instante, ZoneOffset.UTC);
    }

    private static Instant instante(OffsetDateTime valor) {
        return valor == null ? null : valor.toInstant();
    }

    public record Fila(UUID id, UUID partidaId, String tipo, String estado, UUID cazadorId,
            UUID victimaId, UUID arbitroId, UUID eventoEliminacionId, String textoApertura,
            String textoRespuesta, Instant abiertaEn, Instant plazoHasta, String razon,
            String resueltaPor, Instant cerradaEn) {
    }

    public record Jugador(String estado, UUID objetivoId) {
    }

    public record Eliminacion(UUID eventoId, UUID cazadorId, Instant ocurridoEn, String origen) {
    }
}
