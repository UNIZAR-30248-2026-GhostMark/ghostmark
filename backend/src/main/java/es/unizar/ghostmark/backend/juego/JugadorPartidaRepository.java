package es.unizar.ghostmark.backend.juego;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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

    record EstadoJugador(String estado, UUID objetivoId, UUID misionId, int eliminaciones,
            byte[] secreto, int intentosFallidos, Instant bloqueadoHasta) {
    }

    public Optional<EstadoJugador> bloquear(UUID partidaId, UUID inscripcionId) {
        return leer(partidaId, inscripcionId, true);
    }

    public Optional<EstadoJugador> obtener(UUID partidaId, UUID inscripcionId) {
        return leer(partidaId, inscripcionId, false);
    }

    private Optional<EstadoJugador> leer(UUID partidaId, UUID inscripcionId, boolean bloquear) {
        String sql = "select estado, objetivo_id, mision_id, eliminaciones, secreto_codigo,"
                + " intentos_fallidos, bloqueado_hasta from jugador_partida"
                + " where partida_id = ? and inscripcion_id = ?"
                + (bloquear ? " for update" : "");
        return jdbc.query(sql, (fila, numero) -> new EstadoJugador(
                        fila.getString("estado"),
                        fila.getObject("objetivo_id", UUID.class),
                        fila.getObject("mision_id", UUID.class),
                        fila.getInt("eliminaciones"),
                        fila.getBytes("secreto_codigo"),
                        fila.getInt("intentos_fallidos"),
                        fecha(fila.getObject("bloqueado_hasta", OffsetDateTime.class))),
                partidaId, inscripcionId).stream().findFirst();
    }

    public void eliminar(UUID partidaId, UUID inscripcionId, Instant ahora) {
        jdbc.update("update jugador_partida set estado = 'ELIMINADO', eliminado_en = ?,"
                        + " objetivo_id = null, mision_id = null"
                        + " where partida_id = ? and inscripcion_id = ?",
                OffsetDateTime.ofInstant(ahora, ZoneOffset.UTC), partidaId, inscripcionId);
    }

    public void heredar(UUID partidaId, UUID inscripcionId, UUID objetivoId, UUID misionId) {
        jdbc.update("update jugador_partida set objetivo_id = ?, mision_id = ?,"
                        + " eliminaciones = eliminaciones + 1, intentos_fallidos = 0,"
                        + " bloqueado_hasta = null"
                        + " where partida_id = ? and inscripcion_id = ?",
                objetivoId, misionId, partidaId, inscripcionId);
    }

    public int sumarFallo(UUID partidaId, UUID inscripcionId) {
        return jdbc.queryForObject("update jugador_partida"
                        + " set intentos_fallidos = least(intentos_fallidos + 1, 3)"
                        + " where partida_id = ? and inscripcion_id = ?"
                        + " returning intentos_fallidos",
                Integer.class, partidaId, inscripcionId);
    }

    public void bloquearIntentos(UUID partidaId, UUID inscripcionId, Instant hasta) {
        jdbc.update("update jugador_partida set intentos_fallidos = 0, bloqueado_hasta = ?"
                        + " where partida_id = ? and inscripcion_id = ?",
                OffsetDateTime.ofInstant(hasta, ZoneOffset.UTC), partidaId, inscripcionId);
    }

    private static Instant fecha(OffsetDateTime valor) {
        return valor == null ? null : valor.toInstant();
    }

    private record Asignacion(UUID id, UUID objetivoId, UUID misionId) {
    }
}
