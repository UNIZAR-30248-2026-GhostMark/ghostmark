package es.unizar.ghostmark.backend.juego;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import es.unizar.ghostmark.backend.TestcontainersConfiguration;
import java.sql.Connection;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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
@SpringBootTest
class JugadorPartidaMigracionTest {

    @Autowired
    DataSource datos;

    @Test
    void rellenaSoloPartidasEnJuegoSinCambiarEventosYConservaElCiclo() throws Exception {
        String esquema = "prueba_jugador_" + UUID.randomUUID().toString().replace("-", "");
        Flyway completa = Flyway.configure().dataSource(datos).schemas(esquema)
                .defaultSchema(esquema).load();
        MigrationInfo[] pendientes = completa.info().pending();
        MigrationVersion version = Arrays.stream(pendientes)
                .filter(info -> info.getScript().endsWith("__jugador_partida.sql"))
                .findFirst().orElseThrow().getVersion();
        MigrationVersion anterior = Arrays.stream(pendientes).map(MigrationInfo::getVersion)
                .filter(valor -> valor.compareTo(version) < 0)
                .max(MigrationVersion::compareTo).orElseThrow();
        try {
            Flyway.configure().dataSource(datos).schemas(esquema).defaultSchema(esquema)
                    .target(anterior).load().migrate();
            try (Connection conexion = datos.getConnection()) {
                String esquemaAnterior = conexion.getSchema();
                try {
                    conexion.setSchema(esquema);
                    JdbcTemplate jdbc = new JdbcTemplate(
                            new SingleConnectionDataSource(conexion, true));
                    UUID primera = crearPartida(jdbc, "EN_JUEGO", "ABCDEFGH", 3);
                    UUID segunda = crearPartida(jdbc, "EN_JUEGO", "BCDEFGHJ", 4);
                    crearPartida(jdbc, "EN_INSCRIPCION", "CDEFGHJK", 3);
                    crearPartida(jdbc, "CANCELADA", "DEFGHJKM", 3);
                    crearPartida(jdbc, "CERRADA", "EFGHJKMN", 3);
                    List<Map<String, Object>> eventos = jdbc.queryForList(
                            "select * from evento order by partida_id, secuencia");
                    completa.migrate();
                    assertEquals(7, jdbc.queryForObject("select count(*) from jugador_partida",
                            Integer.class));
                    assertEquals(7, jdbc.queryForObject("select count(*) from jugador_partida"
                            + " where estado = 'VIVO' and eliminaciones = 0"
                            + " and intentos_fallidos = 0"
                            + " and bloqueado_hasta is null and eliminado_en is null"
                            + " and octet_length(secreto_codigo) = 20", Integer.class));
                    assertEquals(7, jdbc.queryForObject(
                            "select count(distinct secreto_codigo) from jugador_partida",
                            Integer.class));
                    comprobarAsignaciones(jdbc, primera);
                    comprobarAsignaciones(jdbc, segunda);
                    assertEquals(eventos, jdbc.queryForList(
                            "select * from evento order by partida_id, secuencia"));
                    List<Map<String, Object>> jugadores = jdbc.queryForList("select inscripcion_id,"
                            + " encode(secreto_codigo, 'hex') as secreto from jugador_partida"
                            + " order by inscripcion_id");
                    completa.migrate();
                    assertEquals(jugadores, jdbc.queryForList("select inscripcion_id,"
                            + " encode(secreto_codigo, 'hex') as secreto from jugador_partida"
                            + " order by inscripcion_id"));
                    assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                            "update jugador_partida set secreto_codigo = decode('00', 'hex')"));
                    assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                            "update jugador_partida set estado = 'OTRO'"));
                    assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                            "update jugador_partida set intentos_fallidos = 4"));
                    jdbc.update("delete from partida where id = ?", primera);
                    assertEquals(4, jdbc.queryForObject("select count(*) from jugador_partida",
                            Integer.class));
                } finally {
                    conexion.setSchema(esquemaAnterior);
                }
            }
        } finally {
            new JdbcTemplate(datos).execute("drop schema if exists " + esquema + " cascade");
        }
    }

    private UUID crearPartida(JdbcTemplate jdbc, String estado, String codigo, int numero) {
        UUID id = UUID.randomUUID();
        OffsetDateTime fecha = OffsetDateTime.parse("2026-11-01T12:00:00Z");
        jdbc.update("insert into partida (id, nombre, master_juega, inicio_en, fin_en,"
                        + " tipo_misiones, plazo_disputa_horas, entradas_tardias,"
                        + " codigo_invitacion, estado, creada_en, actualizada_en)"
                        + " values (?, 'Partida anterior', false,"
                        + " ?, ?, 'BIBLIOTECA', 6, false, ?, ?, ?, ?)",
                id, fecha, fecha.plusDays(7), codigo, estado, fecha, fecha);
        List<UUID> inscripciones = new ArrayList<>();
        for (int i = 0; i < numero; i++) {
            UUID inscripcion = UUID.randomUUID();
            // Cuenta nula: también se migra a quien ya borró su cuenta en una partida en juego
            jdbc.update("insert into inscripcion (id, partida_id, normas_aceptadas_en, inscrita_en)"
                    + " values (?, ?, ?, ?)", inscripcion, id, fecha, fecha);
            inscripciones.add(inscripcion);
        }
        String orden = inscripciones.stream().map(valor -> "\"" + valor + "\"")
                .collect(Collectors.joining(",", "{\"orden\":[", "]}"));
        List<UUID> misiones = jdbc.queryForList("select id from mision order by id limit ?",
                UUID.class, numero);
        List<String> asignaciones = new ArrayList<>();
        for (int i = 0; i < numero; i++) {
            asignaciones.add("{\"inscripcionId\":\"" + inscripciones.get(i)
                    + "\",\"misionId\":\"" + misiones.get(i) + "\"}");
        }
        jdbc.update("insert into evento values (?, ?, 1, 'CICLO_FORMADO', ?::jsonb, ?)",
                UUID.randomUUID(), id, orden, fecha);
        jdbc.update("insert into evento values (?, ?, 2, 'MISIONES_REPARTIDAS', ?::jsonb, ?)",
                UUID.randomUUID(), id,
                "{\"asignaciones\":[" + String.join(",", asignaciones) + "]}", fecha);
        return id;
    }

    private void comprobarAsignaciones(JdbcTemplate jdbc, UUID partidaId) {
        List<UUID> orden = jdbc.queryForList("select jugador.id::uuid from evento e,"
                + " jsonb_array_elements_text(e.datos->'orden') with ordinality as jugador(id, pos)"
                + " where e.partida_id = ? and e.tipo = 'CICLO_FORMADO' order by jugador.pos",
                UUID.class, partidaId);
        for (int i = 0; i < orden.size(); i++) {
            UUID id = orden.get(i);
            assertEquals(orden.get((i + 1) % orden.size()), jdbc.queryForObject(
                    "select objetivo_id from jugador_partida where inscripcion_id = ?",
                    UUID.class, id));
            UUID mision = jdbc.queryForObject("select (a.valor->>'misionId')::uuid from evento e,"
                    + " jsonb_array_elements(e.datos->'asignaciones') as a(valor)"
                    + " where e.partida_id = ? and e.tipo = 'MISIONES_REPARTIDAS'"
                    + " and (a.valor->>'inscripcionId')::uuid = ?", UUID.class, partidaId, id);
            assertEquals(mision, jdbc.queryForObject(
                    "select mision_id from jugador_partida where inscripcion_id = ?",
                    UUID.class, id));
        }
    }
}
