package es.unizar.ghostmark.backend.juego;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CodigoEmitidoRepository {

    static final Duration TOLERANCIA = Duration.ofSeconds(15);
    static final Duration CADUCADOS = Duration.ofMinutes(10);

    private final JdbcTemplate jdbc;

    public CodigoEmitidoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void emitir(UUID partidaId, UUID inscripcionId, CodigoVida codigo, Instant ahora) {
        jdbc.update("delete from codigo_emitido where inscripcion_id = ? and caduca_en < ?",
                inscripcionId, fecha(ahora.minus(CADUCADOS)));
        jdbc.update("insert into codigo_emitido (id, partida_id, inscripcion_id, codigo,"
                        + " emitido_en, caduca_en) values (?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), partidaId, inscripcionId, codigo.codigo(), fecha(ahora),
                fecha(codigo.caducaEn()));
    }

    public boolean valido(UUID inscripcionId, String codigo, Instant ahora) {
        return caducaDespuesDe(inscripcionId, codigo, ahora.minus(TOLERANCIA));
    }

    public boolean caducado(UUID inscripcionId, String codigo, Instant ahora) {
        return caducaDespuesDe(inscripcionId, codigo, ahora.minus(CADUCADOS));
    }

    private boolean caducaDespuesDe(UUID inscripcionId, String codigo, Instant desde) {
        return Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1"
                        + " from codigo_emitido where inscripcion_id = ? and codigo = ?"
                        + " and caduca_en > ?)", Boolean.class, inscripcionId, codigo,
                fecha(desde)));
    }

    private static OffsetDateTime fecha(Instant instante) {
        return OffsetDateTime.ofInstant(instante, ZoneOffset.UTC);
    }
}
