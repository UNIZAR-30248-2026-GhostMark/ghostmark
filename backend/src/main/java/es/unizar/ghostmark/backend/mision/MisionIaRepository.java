package es.unizar.ghostmark.backend.mision;

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
public class MisionIaRepository {

    private static final RowMapper<MisionIa> MISION = (fila, numero) -> new MisionIa(
            fila.getObject("id", UUID.class), fila.getString("texto"), fila.getString("estado"),
            fila.getInt("tanda"));

    private final JdbcTemplate jdbc;

    public MisionIaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record DatosPartida(UUID masterId, String estado, String tipoMisiones) {
    }

    public boolean cuentaExiste(UUID cuentaId, boolean bloquear) {
        return !jdbc.queryForList("select id from cuenta where id = ?"
                + (bloquear ? " for update" : ""), UUID.class, cuentaId).isEmpty();
    }

    public Optional<DatosPartida> partida(UUID partidaId, boolean bloquear) {
        return jdbc.query("select master_id, estado, tipo_misiones from partida where id = ?"
                + (bloquear ? " for update" : ""), (fila, numero) -> new DatosPartida(
                        fila.getObject("master_id", UUID.class), fila.getString("estado"),
                        fila.getString("tipo_misiones")), partidaId).stream().findFirst();
    }

    public boolean inscrita(UUID partidaId, UUID cuentaId) {
        return jdbc.queryForObject("select exists (select 1 from inscripcion where partida_id = ?"
                + " and cuenta_id = ?)", Boolean.class, partidaId, cuentaId);
    }

    public List<MisionIa> dePartida(UUID partidaId) {
        return jdbc.query("select id, texto, estado, tanda from mision where partida_id = ?"
                + " order by tanda, texto", MISION, partidaId);
    }

    public List<String> textos(UUID partidaId) {
        return jdbc.queryForList("select texto from mision where partida_id = ?", String.class,
                partidaId);
    }

    public int tandas(UUID partidaId) {
        return jdbc.queryForObject("select count(*) from evento where partida_id = ?"
                + " and tipo = 'MISIONES_GENERADAS'", Integer.class, partidaId);
    }

    public UUID crear(UUID partidaId, String texto, int tanda, Instant ahora) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into mision (id, texto, activa, creada_en, partida_id, estado, tanda)"
                + " values (?, ?, true, ?, ?, 'PROPUESTA', ?)", id, texto, fecha(ahora), partidaId,
                tanda);
        return id;
    }

    public Optional<MisionIa> bloquear(UUID partidaId, UUID misionId) {
        return jdbc.query("select id, texto, estado, tanda from mision where id = ?"
                + " and partida_id = ? for update", MISION, misionId, partidaId).stream()
                .findFirst();
    }

    public void revisar(UUID misionId, String estado, Instant ahora) {
        jdbc.update("update mision set estado = ?, revisada_en = ? where id = ?", estado,
                fecha(ahora), misionId);
    }

    private static OffsetDateTime fecha(Instant instante) {
        return OffsetDateTime.ofInstant(instante, ZoneOffset.UTC);
    }
}
