package es.unizar.ghostmark.backend.juego;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CartaRepository {

    private final JdbcTemplate jdbc;

    public CartaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<UUID> misionesActivas() {
        return jdbc.queryForList("select id from mision where activa order by id", UUID.class);
    }

    public Optional<Carta> obtener(UUID partidaId, UUID inscripcionId) {
        return jdbc.query("""
                select jp.estado, c.alias, m.texto, jp.eliminaciones
                from jugador_partida jp
                left join inscripcion objetivo on objetivo.id = jp.objetivo_id
                left join cuenta c on c.id = objetivo.cuenta_id
                left join mision m on m.id = jp.mision_id
                where jp.partida_id = ? and jp.inscripcion_id = ?
                """, (fila, numero) -> new Carta(fila.getString("estado"),
                        fila.getString("alias"), fila.getString("texto"),
                        fila.getInt("eliminaciones")),
                partidaId, inscripcionId).stream().findFirst();
    }
}
