package es.unizar.ghostmark.backend.mision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import es.unizar.ghostmark.backend.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * La biblioteca curada de la tarea 5.1 (#67) se carga con su migración.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MisionMigracionTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void laMigracionCargaAlMenos20MisionesActivas() {
        Integer activas = jdbc.queryForObject(
                "select count(*) from mision where activa", Integer.class);
        assertTrue(activas >= 20, "Hay " + activas + " misiones activas");
    }

    @Test
    void cadaMisionTieneTextoUnicoDeComoMucho200Caracteres() {
        Integer total = jdbc.queryForObject("select count(*) from mision", Integer.class);
        Integer distintas = jdbc.queryForObject(
                "select count(distinct lower(texto)) from mision", Integer.class);
        Integer largas = jdbc.queryForObject(
                "select count(*) from mision where char_length(texto) > 200", Integer.class);
        assertEquals(total, distintas);
        assertEquals(0, largas);
    }
}
