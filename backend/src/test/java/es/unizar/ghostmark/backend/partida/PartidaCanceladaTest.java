package es.unizar.ghostmark.backend.partida;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import es.unizar.ghostmark.backend.TestcontainersConfiguration;
import io.restassured.http.ContentType;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PartidaCanceladaTest {

    @LocalServerPort
    int puerto;

    @Autowired
    PartidaRepository partidas;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PlatformTransactionManager transacciones;

    @Test
    void borrarCuentaDelMasterCancelaPartidasEnInscripcion() {
        String token = registrar("master_x", "master_x@example.com");
        String id = crearPartida(token, "Killer Cancelado");

        borrarCuenta(token);

        Partida partida = partidas.findById(UUID.fromString(id)).orElseThrow();
        assertEquals("CANCELADA", partida.getEstado());

        Map<String, Object> evento = jdbc.queryForMap(
                "select secuencia, tipo, datos->>'motivo' as motivo,"
                        + " datos->>'numJugadores' as numJugadores from evento"
                        + " where partida_id = ? and tipo = 'PARTIDA_CANCELADA'",
                UUID.fromString(id));
        assertEquals(3L, ((Number) evento.get("secuencia")).longValue());
        assertEquals("MASTER_SIN_CUENTA", evento.get("motivo"));
        assertEquals("1", evento.get("numJugadores"));
    }

    @Test
    void borrarOtraCuentaNoTocaSusPartidas() {
        String tokenMaster = registrar("master_y", "master_y@example.com");
        String id = crearPartida(tokenMaster, "Killer Intacto");

        String tokenOtro = registrar("otro_z", "otro_z@example.com");
        borrarCuenta(tokenOtro);

        Partida partida = partidas.findById(UUID.fromString(id)).orElseThrow();
        assertEquals("EN_INSCRIPCION", partida.getEstado());

        Integer cancelados = jdbc.queryForObject(
                "select count(*) from evento where partida_id = ?"
                        + " and tipo = 'PARTIDA_CANCELADA'",
                Integer.class, UUID.fromString(id));
        assertEquals(0, cancelados);
    }

    @Test
    void cancelarEsperaAOtroEventoDeLaMismaPartida() throws Exception {
        String token = registrar("master_w", "master_w@example.com");
        UUID id = UUID.fromString(crearPartida(token, "Killer Simultaneo"));

        CountDownLatch bloqueada = new CountDownLatch(1);
        CountDownLatch soltar = new CountDownLatch(1);
        CompletableFuture<Void> otroEvento = CompletableFuture.runAsync(() ->
                new TransactionTemplate(transacciones).executeWithoutResult(estado -> {
                    jdbc.queryForObject("select id from partida where id = ? for update",
                            UUID.class, id);
                    jdbc.update("insert into evento"
                            + " (id, partida_id, secuencia, tipo, datos, ocurrido_en)"
                            + " select ?, ?, max(secuencia) + 1, 'JUGADOR_INSCRITO',"
                            + " '{}'::jsonb, now() from evento where partida_id = ?",
                            UUID.randomUUID(), id, id);
                    bloqueada.countDown();
                    try {
                        soltar.await(10, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }));
        assertTrue(bloqueada.await(10, TimeUnit.SECONDS));

        CompletableFuture<Void> borrado = CompletableFuture.runAsync(() -> borrarCuenta(token));
        Thread.sleep(500);
        soltar.countDown();
        otroEvento.get(10, TimeUnit.SECONDS);
        borrado.get(10, TimeUnit.SECONDS);

        Long secuencia = jdbc.queryForObject(
                "select secuencia from evento where partida_id = ? and tipo = 'PARTIDA_CANCELADA'",
                Long.class, id);
        assertEquals(4L, secuencia);
    }

    private void borrarCuenta(String token) {
        given()
                .port(puerto)
                .auth()
                .oauth2(token)
                .when()
                .delete("/api/v1/cuentas/yo")
                .then()
                .statusCode(204);
    }

    private String crearPartida(String token, String nombre) {
        Instant inicio = Instant.now().plus(Duration.ofDays(1));
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("nombre", nombre);
        cuerpo.put("inicioEn", inicio.toString());
        cuerpo.put("finEn", inicio.plus(Duration.ofDays(7)).toString());
        cuerpo.put("tipoMisiones", "BIBLIOTECA");
        cuerpo.put("plazoDisputaHoras", 6);
        cuerpo.put("entradasTardias", false);
        cuerpo.put("masterJuega", true);

        return given()
                .port(puerto)
                .auth()
                .oauth2(token)
                .contentType(ContentType.JSON)
                .body(cuerpo)
                .when()
                .post("/api/v1/partidas")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }

    private String registrar(String alias, String email) {
        return given()
                .port(puerto)
                .contentType(ContentType.JSON)
                .body(Map.of("alias", alias, "email", email, "contrasena", "fantasma2026"))
                .when()
                .post("/api/v1/cuentas")
                .then()
                .statusCode(201)
                .extract()
                .path("tokenAcceso");
    }
}