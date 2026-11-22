package es.unizar.ghostmark.backend.juego;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

public final class CodigosDePrueba {

    private CodigosDePrueba() {
    }

    public static String emitir(JdbcTemplate jdbc, UUID inscripcionId, Instant cuando) {
        CodigoVida codigo = new GeneradorCodigoVida().generar(cuando);
        jdbc.update("insert into codigo_emitido (id, partida_id, inscripcion_id, codigo,"
                        + " emitido_en, caduca_en) select ?, partida_id, inscripcion_id, ?, ?, ?"
                        + " from jugador_partida where inscripcion_id = ?",
                UUID.randomUUID(), codigo.codigo(), fecha(cuando), fecha(codigo.caducaEn()),
                inscripcionId);
        return codigo.codigo();
    }

    private static OffsetDateTime fecha(Instant instante) {
        return OffsetDateTime.ofInstant(instante, ZoneOffset.UTC);
    }
}
