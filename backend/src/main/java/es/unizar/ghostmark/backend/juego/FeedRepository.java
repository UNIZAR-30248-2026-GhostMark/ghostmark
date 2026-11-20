package es.unizar.ghostmark.backend.juego;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FeedRepository {

    private final JdbcTemplate jdbc;

    public FeedRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<EntradaFeed> eliminaciones(UUID partidaId) {
        return jdbc.query("""
                select noticia.tipo, noticia.alias, noticia.ocurrido_en
                from (
                    select 'ELIMINACION' as tipo, c.alias, e.ocurrido_en, e.secuencia
                    from evento e
                    left join inscripcion i on i.id = (e.datos->>'victimaId')::uuid
                    left join cuenta c on c.id = i.cuenta_id
                    where e.partida_id = ? and e.tipo = 'JUGADOR_ELIMINADO'
                    union all
                    select 'ANULACION' as tipo, c.alias, e.ocurrido_en, e.secuencia
                    from evento e
                    cross join lateral jsonb_array_elements_text(
                        coalesce(e.datos->'revividos', '[]'::jsonb)) as r(inscripcion_id)
                    left join inscripcion i on i.id = r.inscripcion_id::uuid
                    left join cuenta c on c.id = i.cuenta_id
                    where e.partida_id = ? and e.tipo = 'ELIMINACION_ANULADA'
                ) noticia
                order by noticia.ocurrido_en desc, noticia.secuencia desc
                """, (fila, numero) -> new EntradaFeed(fila.getString("tipo"),
                        fila.getString("alias"),
                        fila.getObject("ocurrido_en", OffsetDateTime.class).toInstant()),
                partidaId, partidaId);
    }
}
