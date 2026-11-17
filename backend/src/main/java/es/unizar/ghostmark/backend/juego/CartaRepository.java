package es.unizar.ghostmark.backend.juego;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CartaRepository {

    private static final int INTENTOS = 3;
    private static final Duration PLAZO_PARA_DISPUTAR = Duration.ofMinutes(10);

    private final JdbcTemplate jdbc;
    private final HorarioLectivo horario;

    public CartaRepository(JdbcTemplate jdbc, HorarioLectivo horario) {
        this.jdbc = jdbc;
        this.horario = horario;
    }

    public List<UUID> misionesActivas() {
        return jdbc.queryForList("select id from mision where activa and partida_id is null"
                + " order by id", UUID.class);
    }

    public List<UUID> misionesAprobadas(UUID partidaId) {
        return jdbc.queryForList("select m.id from mision m join partida p on p.id = m.partida_id"
                + " where m.partida_id = ? and m.estado = 'APROBADA'"
                + " and p.tipo_misiones = 'IA' order by m.id", UUID.class, partidaId);
    }

    public Optional<Carta> obtener(UUID partidaId, UUID inscripcionId, Instant ahora) {
        return jdbc.query("""
                select jp.estado, c.alias, m.texto, jp.eliminaciones, jp.objetivo_id,
                       jp.intentos_fallidos, jp.bloqueado_hasta,
                       exists (select 1 from disputa d where d.cazador_id = jp.inscripcion_id
                               and d.tipo = 'ELIMINACION' and d.estado = 'ABIERTA')
                           as bloqueado_disputa,
                       (select e.ocurrido_en from evento e
                        where e.partida_id = jp.partida_id and e.tipo = 'JUGADOR_ELIMINADO'
                          and e.datos->>'victimaId' = jp.inscripcion_id::text
                          and coalesce(e.datos->>'origen', 'CODIGO') = 'CODIGO'
                          and not exists (select 1 from disputa d
                                          where d.evento_eliminacion_id = e.id)
                          and e.secuencia = (select max(u.secuencia) from evento u
                                             where u.partida_id = jp.partida_id
                                               and u.tipo = 'JUGADOR_ELIMINADO'
                                               and u.datos->>'victimaId'
                                                   = jp.inscripcion_id::text))
                           as eliminado_disputable
                from jugador_partida jp
                left join inscripcion objetivo on objetivo.id = jp.objetivo_id
                left join cuenta c on c.id = objetivo.cuenta_id
                left join mision m on m.id = jp.mision_id
                where jp.partida_id = ? and jp.inscripcion_id = ?
                """, (fila, numero) -> {
                    UUID objetivoId = fila.getObject("objetivo_id", UUID.class);
                    boolean caza = "VIVO".equals(fila.getString("estado")) && objetivoId != null;
                    OffsetDateTime bloqueo = fila.getObject("bloqueado_hasta",
                            OffsetDateTime.class);
                    boolean bloqueado = caza && bloqueo != null
                            && bloqueo.toInstant().isAfter(ahora);
                    OffsetDateTime eliminado = fila.getObject("eliminado_disputable",
                            OffsetDateTime.class);
                    Instant disputarHasta = eliminado == null
                            || !"ELIMINADO".equals(fila.getString("estado")) ? null
                            : eliminado.toInstant().plus(PLAZO_PARA_DISPUTAR);
                    return new Carta(fila.getString("estado"), fila.getString("alias"),
                            fila.getString("texto"), fila.getInt("eliminaciones"),
                            caza ? INTENTOS - fila.getInt("intentos_fallidos") : null,
                            bloqueado ? bloqueo.toInstant() : null,
                            caza ? horario.noDisponible(inscripcionId, objetivoId, ahora)
                                    .orElse(null) : null,
                            fila.getBoolean("bloqueado_disputa"),
                            disputarHasta != null && disputarHasta.isAfter(ahora)
                                    ? disputarHasta : null);
                }, partidaId, inscripcionId).stream().findFirst();
    }
}
