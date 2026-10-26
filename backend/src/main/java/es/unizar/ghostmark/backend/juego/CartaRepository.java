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
        // El siguiente del ciclo es el objetivo; el último vuelve al primero.
        return jdbc.query("""
                with ciclo as (
                    select jugador.id::uuid as id,
                           lead(jugador.id, 1, e.datos->'orden'->>0)
                               over (order by jugador.pos)::uuid as objetivo_id
                    from evento e,
                         jsonb_array_elements_text(e.datos->'orden')
                             with ordinality as jugador(id, pos)
                    where e.partida_id = ? and e.tipo = 'CICLO_FORMADO'
                )
                select c.alias, m.texto
                from ciclo
                join inscripcion objetivo on objetivo.id = ciclo.objetivo_id
                left join cuenta c on c.id = objetivo.cuenta_id
                join evento e on e.partida_id = ? and e.tipo = 'MISIONES_REPARTIDAS'
                cross join lateral jsonb_array_elements(e.datos->'asignaciones') as a(valor)
                join mision m on m.id = (a.valor->>'misionId')::uuid
                where ciclo.id = ? and (a.valor->>'inscripcionId')::uuid = ciclo.id
                """, (fila, numero) -> new Carta(fila.getString("alias"), fila.getString("texto")),
                partidaId, partidaId, inscripcionId).stream().findFirst();
    }
}
