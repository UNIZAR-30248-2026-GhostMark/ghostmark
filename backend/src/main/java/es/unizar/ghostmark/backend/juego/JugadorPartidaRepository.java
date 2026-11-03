package es.unizar.ghostmark.backend.juego;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JugadorPartidaRepository {

    private final JdbcTemplate jdbc;
    private final SecureRandom aleatorio = new SecureRandom();

    public JugadorPartidaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void crearAlArrancar(UUID partidaId) {
        var asignaciones = jdbc.query("""
                with ciclo as (
                    select jugador.id::uuid as id,
                           lead(jugador.id, 1, e.datos->'orden'->>0)
                               over (order by jugador.pos)::uuid as objetivo_id
                    from evento e,
                         jsonb_array_elements_text(e.datos->'orden')
                             with ordinality as jugador(id, pos)
                    where e.partida_id = ? and e.tipo = 'CICLO_FORMADO'
                )
                select ciclo.id, ciclo.objetivo_id, (a.valor->>'misionId')::uuid as mision_id
                from ciclo
                join evento e on e.partida_id = ? and e.tipo = 'MISIONES_REPARTIDAS'
                cross join lateral jsonb_array_elements(e.datos->'asignaciones') as a(valor)
                where (a.valor->>'inscripcionId')::uuid = ciclo.id
                """, (fila, numero) -> new Asignacion(fila.getObject("id", UUID.class),
                        fila.getObject("objetivo_id", UUID.class),
                        fila.getObject("mision_id", UUID.class)), partidaId, partidaId);
        for (Asignacion asignacion : asignaciones) {
            byte[] secreto = new byte[20];
            aleatorio.nextBytes(secreto);
            jdbc.update("insert into jugador_partida (inscripcion_id, partida_id, estado,"
                            + " objetivo_id, mision_id, secreto_codigo)"
                            + " values (?, ?, 'VIVO', ?, ?, ?)",
                    asignacion.id(), partidaId, asignacion.objetivoId(), asignacion.misionId(),
                    secreto);
        }
    }

    public Optional<DatosCodigo> obtenerCodigo(UUID partidaId, UUID inscripcionId) {
        return jdbc.query("select estado, secreto_codigo from jugador_partida"
                        + " where partida_id = ? and inscripcion_id = ?",
                (fila, numero) -> new DatosCodigo(fila.getString("estado"),
                        fila.getBytes("secreto_codigo")), partidaId, inscripcionId)
                .stream().findFirst();
    }

    record DatosCodigo(String estado, byte[] secreto) {
    }

    private record Asignacion(UUID id, UUID objetivoId, UUID misionId) {
    }
}
