package es.unizar.ghostmark.backend.evento;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioEvento {

    private final JdbcTemplate jdbc;

    public ServicioEvento(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void guardar(UUID partidaId, String tipo, String datos, Instant ahora) {
        // Todos los escritores comparten este bloqueo antes de calcular la secuencia.
        jdbc.queryForObject("select id from partida where id = ? for update",
                UUID.class, partidaId);
        jdbc.update("insert into evento (id, partida_id, secuencia, tipo, datos, ocurrido_en)"
                        + " select ?, ?, coalesce(max(secuencia), 0) + 1, ?, ?::jsonb, ?"
                        + " from evento where partida_id = ?",
                UUID.randomUUID(), partidaId, tipo, datos,
                OffsetDateTime.ofInstant(ahora, ZoneOffset.UTC), partidaId);
    }
}
