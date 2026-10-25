package es.unizar.ghostmark.backend.inscripcion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import es.unizar.ghostmark.backend.TestcontainersConfiguration;
import java.sql.Connection;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InscripcionMigracionTest {

    @Autowired
    DataSource datos;

    @Test
    void migrarPartidasExistentesInscribeMasterCopiaHorarioYConservaEventos() throws Exception {
        String esquema = "prueba_inscripcion_" + UUID.randomUUID().toString().replace("-", "");
        Flyway completa = Flyway.configure().dataSource(datos).schemas(esquema)
                .defaultSchema(esquema).load();
        MigrationInfo[] pendientes = completa.info().pending();
        MigrationVersion version = Arrays.stream(pendientes)
                .filter(info -> info.getScript().endsWith("__inscripcion.sql"))
                .findFirst().orElseThrow().getVersion();
        MigrationVersion anterior = Arrays.stream(pendientes).map(MigrationInfo::getVersion)
                .filter(valor -> valor.compareTo(version) < 0)
                .max(MigrationVersion::compareTo).orElseThrow();
        Flyway.configure().dataSource(datos).schemas(esquema).defaultSchema(esquema)
                .target(anterior).load().migrate();
        try (Connection conexion = datos.getConnection()) {
            String esquemaAnterior = conexion.getSchema();
            try {
                conexion.setSchema(esquema);
                JdbcTemplate jdbc = new JdbcTemplate(new SingleConnectionDataSource(conexion, true));
                UUID cuenta = UUID.randomUUID();
                OffsetDateTime fecha = OffsetDateTime.parse("2026-10-20T08:00:00Z");
                jdbc.update("insert into cuenta (id, alias, email, hash_contrasena, creada_en,"
                                + " actualizada_en) values (?, 'migracion', 'migracion@example.com',"
                                + " 'hash_de_prueba', ?, ?)", cuenta, fecha, fecha);
                jdbc.update("insert into franja_horario values (?, 1, '08:00')", cuenta);
                UUID abierta = partida(jdbc, cuenta, true, "EN_INSCRIPCION", "ABCDEFGH", fecha);
                UUID enJuego = partida(jdbc, cuenta, true, "EN_JUEGO", "BCDEFGHJ", fecha);
                UUID cancelada = partida(jdbc, cuenta, true, "CANCELADA", "CDEFGHJK", fecha);
                partida(jdbc, cuenta, false, "EN_INSCRIPCION", "DEFGHJKM", fecha);
                partida(jdbc, null, true, "EN_INSCRIPCION", "EFGHJKMN", fecha);
                partida(jdbc, cuenta, true, "CERRADA", "FGHJKMNP", fecha);
                completa.migrate();
                assertEquals(3, jdbc.queryForObject("select count(*) from inscripcion", Integer.class));
                assertEquals(3, jdbc.queryForObject("select count(*) from franja_horario_inscripcion",
                        Integer.class));
                for (UUID id : new UUID[] {abierta, enJuego, cancelada}) {
                    assertEquals(fecha.toInstant(), jdbc.queryForObject(
                            "select normas_aceptadas_en from inscripcion where partida_id = ?",
                            OffsetDateTime.class, id).toInstant());
                    assertEquals(2L, jdbc.queryForObject("select secuencia from evento"
                            + " where partida_id = ? and tipo = 'JUGADOR_INSCRITO'", Long.class, id));
                    assertEquals(1, jdbc.queryForObject("select count(*) from evento e"
                            + " join inscripcion i on e.datos->>'inscripcionId' = i.id::text"
                            + " where i.partida_id = ?", Integer.class, id));
                }
                assertEquals(6, jdbc.queryForObject("select count(*) from evento"
                        + " where tipo = 'PARTIDA_CREADA' and datos = '{}'::jsonb", Integer.class));
                completa.migrate();
                assertEquals(3, jdbc.queryForObject("select count(*) from inscripcion", Integer.class));
                assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                        "insert into inscripcion select gen_random_uuid(), partida_id, cuenta_id,"
                                + " normas_aceptadas_en, inscrita_en from inscripcion limit 1"));
                UUID id = jdbc.queryForObject("select id from inscripcion where partida_id = ?",
                        UUID.class, abierta);
                assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                        "insert into franja_horario_inscripcion values (?, 6, '08:00')", id));
                assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                        "insert into franja_horario_inscripcion values (?, 2, '08:15')", id));
            } finally {
                conexion.setSchema(esquemaAnterior);
            }
        } finally {
            new JdbcTemplate(datos).execute("drop schema " + esquema + " cascade");
        }
    }

    private UUID partida(JdbcTemplate jdbc, UUID master, boolean juega, String estado,
            String codigo, OffsetDateTime fecha) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into partida (id, nombre, master_id, master_juega, inicio_en, fin_en,"
                        + " tipo_misiones, plazo_disputa_horas, entradas_tardias, codigo_invitacion,"
                        + " estado, creada_en, actualizada_en) values (?, 'Partida anterior', ?, ?,"
                        + " ?, ?, 'BIBLIOTECA', 6, false, ?, ?, ?, ?)", id, master, juega,
                fecha.plusDays(1), fecha.plusDays(7), codigo, estado, fecha, fecha);
        jdbc.update("insert into evento values (?, ?, 1, 'PARTIDA_CREADA', '{}'::jsonb, ?)",
                UUID.randomUUID(), id, fecha);
        return id;
    }
}
