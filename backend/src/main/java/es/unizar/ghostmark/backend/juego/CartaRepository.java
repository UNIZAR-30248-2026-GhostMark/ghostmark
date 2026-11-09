package es.unizar.ghostmark.backend.juego;

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

    private final JdbcTemplate jdbc;

    public CartaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
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
                       jp.intentos_fallidos, jp.bloqueado_hasta
                from jugador_partida jp
                left join inscripcion objetivo on objetivo.id = jp.objetivo_id
                left join cuenta c on c.id = objetivo.cuenta_id
                left join mision m on m.id = jp.mision_id
                where jp.partida_id = ? and jp.inscripcion_id = ?
                """, (fila, numero) -> {
                    boolean caza = "VIVO".equals(fila.getString("estado"))
                            && fila.getObject("objetivo_id", UUID.class) != null;
                    OffsetDateTime bloqueo = fila.getObject("bloqueado_hasta",
                            OffsetDateTime.class);
                    boolean bloqueado = caza && bloqueo != null
                            && bloqueo.toInstant().isAfter(ahora);
                    return new Carta(fila.getString("estado"), fila.getString("alias"),
                            fila.getString("texto"), fila.getInt("eliminaciones"),
                            caza ? INTENTOS - fila.getInt("intentos_fallidos") : null,
                            bloqueado ? bloqueo.toInstant() : null);
                }, partidaId, inscripcionId).stream().findFirst();
    }
}
