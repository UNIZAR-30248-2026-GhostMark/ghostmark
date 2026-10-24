package es.unizar.ghostmark.backend.partida;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import es.unizar.ghostmark.backend.TestcontainersConfiguration;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Tests de aceptación de la PBI 2 (#3): uno por criterio de satisfacción, más el de rol (solo el
 * máster ve y cambia la configuración) y los casos de ver y editar la partida.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PartidaIntegracionTest {

    private static final String RUTA_PARTIDAS = "/api/v1/partidas";
    private static final String RUTA_PARTIDA = "/api/v1/partidas/{partidaId}";
    private static final String PREFIJO_ENLACE = "ghostmark://unirse/";

    @LocalServerPort
    int puerto;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PlatformTransactionManager transacciones;

    @Test
    void criterio1CrearPartidaGuardaSusValoresYRechazaFinAnteriorAlInicio() {
        String master = registrar();
        Instant inicio = Instant.now().plus(Duration.ofDays(1)).truncatedTo(ChronoUnit.SECONDS);
        Map<String, Object> cuerpo = configuracion("Killer Bienvenida", inicio);
        cuerpo.put("tipoMisiones", "IA");
        cuerpo.put("plazoDisputaHoras", 12);
        cuerpo.put("entradasTardias", true);
        cuerpo.put("masterJuega", false);

        String id = crear(master, cuerpo).then().statusCode(201).extract().path("id");

        ver(master, id)
                .then()
                .statusCode(200)
                .body("nombre", equalTo("Killer Bienvenida"))
                .body("inicioEn", equalTo(inicio.toString()))
                .body("finEn", equalTo(inicio.plus(Duration.ofDays(7)).toString()))
                .body("tipoMisiones", equalTo("IA"))
                .body("plazoDisputaHoras", equalTo(12))
                .body("entradasTardias", equalTo(true))
                .body("masterJuega", equalTo(false))
                .body("estado", equalTo("EN_INSCRIPCION"));

        Map<String, Object> finAntes = configuracion("Killer Al Reves", inicio);
        finAntes.put("finEn", inicio.minus(Duration.ofHours(1)).toString());
        crear(master, finAntes)
                .then()
                .statusCode(400)
                .body("codigo", equalTo("DATOS_NO_VALIDOS"))
                .body("errores.campo", hasItems("finEn"));
    }

    @Test
    void criterio2ElEnlaceDelQrAbreEsaPartida() {
        String master = registrar();
        String jugador = registrar();
        Response creada = crear(master, configuracion("Killer Compartido", manana()));
        String enlace = creada.path("enlace");
        String codigo = creada.path("codigoInvitacion");

        assertEquals(PREFIJO_ENLACE + codigo, enlace);
        given()
                .port(puerto)
                .auth().oauth2(jugador)
                .when()
                .get(RUTA_PARTIDAS + "/codigo/{codigo}", enlace.substring(PREFIJO_ENLACE.length()))
                .then()
                .statusCode(200)
                .body("nombre", equalTo("Killer Compartido"))
                .body("estado", equalTo("EN_INSCRIPCION"));
    }

    @Test
    void soloElMasterVeYCambiaLaConfiguracion() {
        String master = registrar();
        String otro = registrar();
        String id = crearId(master, "Killer Privado");

        ver(otro, id)
                .then()
                .statusCode(404)
                .body("codigo", equalTo("NO_ENCONTRADA"));
        editar(otro, id, Map.of("nombre", "Killer Robado"))
                .then()
                .statusCode(403)
                .body("codigo", equalTo("NO_ES_MASTER"));

        ver(master, id)
                .then()
                .statusCode(200)
                .body("nombre", equalTo("Killer Privado"))
                .body("codigoInvitacion", equalTo(codigoDe(id)));
    }

    @Test
    @Disabled("Falta la inscripción: llega con la tarea 3.2 (#54)")
    void inscritoVeElResumen() {
        // Cuando exista POST /api/v1/inscripciones: el jugador se inscribe con el código y
        // GET /partidas/{id} le devuelve el resumen (id, nombre, estado, fechas,
        // soyMaster = false, numInscritos), sin código de invitación ni reglas.
    }

    @Test
    void partidaQueNoExisteDa404() {
        String master = registrar();

        ver(master, UUID.randomUUID().toString())
                .then()
                .statusCode(404)
                .body("codigo", equalTo("NO_ENCONTRADA"));
        ver(master, "no-es-un-uuid")
                .then()
                .statusCode(404)
                .body("codigo", equalTo("NO_ENCONTRADA"));
    }

    @Test
    void elMasterCambiaLaConfiguracionYSeGuardaElEvento() {
        String master = registrar();
        String id = crearId(master, "Killer Antiguo");

        editar(master, id, Map.of("nombre", "Killer Nuevo", "plazoDisputaHoras", 24,
                "masterJuega", true))
                .then()
                .statusCode(200)
                .body("nombre", equalTo("Killer Nuevo"))
                .body("plazoDisputaHoras", equalTo(24))
                .body("masterJuega", equalTo(true));
        ver(master, id).then().body("nombre", equalTo("Killer Nuevo"));

        Map<String, Object> evento = jdbc.queryForMap(
                "select secuencia, datos::text as datos from evento"
                        + " where partida_id = ? and tipo = 'CONFIGURACION_CAMBIADA'",
                UUID.fromString(id));
        assertEquals(2L, ((Number) evento.get("secuencia")).longValue());
        // masterJuega ya era true: no cambia y no va en el evento.
        assertEquals("{\"nombre\": \"Killer Nuevo\", \"plazoDisputaHoras\": 24}",
                evento.get("datos"));
    }

    @Test
    void editarEsperaAQueTermineElArranqueYDa409() throws Exception {
        String master = registrar();
        String id = crearId(master, "Killer Simultaneo");
        UUID partidaId = UUID.fromString(id);

        CountDownLatch bloqueada = new CountDownLatch(1);
        CountDownLatch soltar = new CountDownLatch(1);
        CompletableFuture<Void> arranque = CompletableFuture.runAsync(() ->
                new TransactionTemplate(transacciones).executeWithoutResult(estado -> {
                    jdbc.queryForObject("select id from partida where id = ? for update",
                            UUID.class, partidaId);
                    jdbc.update("update partida set estado = 'EN_JUEGO' where id = ?",
                            partidaId);
                    bloqueada.countDown();
                    try {
                        soltar.await(10, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }));
        assertTrue(bloqueada.await(10, TimeUnit.SECONDS));

        CompletableFuture<Integer> edicion = CompletableFuture.supplyAsync(() ->
                editar(master, id, Map.of("nombre", "Killer Editado")).statusCode());
        Thread.sleep(500);
        soltar.countDown();
        arranque.get(10, TimeUnit.SECONDS);

        // Sin el bloqueo, editar lee la partida antes del arranque y la devuelve a EN_INSCRIPCION.
        assertEquals(409, edicion.get(10, TimeUnit.SECONDS));
        assertEquals("EN_JUEGO", jdbc.queryForObject(
                "select estado from partida where id = ?", String.class, partidaId));
    }

    @Test
    void cambiosNoValidosDan400YNoSeGuardan() {
        String master = registrar();
        Instant inicio = manana();
        String id = crear(master, configuracion("Killer Firme", inicio))
                .then().statusCode(201).extract().path("id");

        editar(master, id, Map.of("finEn", inicio.minus(Duration.ofHours(1)).toString()))
                .then()
                .statusCode(400)
                .body("codigo", equalTo("DATOS_NO_VALIDOS"))
                .body("errores.campo", hasItems("finEn"));
        editar(master, id, Map.of("inicioEn", Instant.now().plus(Duration.ofMinutes(5)).toString(),
                "nombre", "ab", "plazoDisputaHoras", 3))
                .then()
                .statusCode(400)
                .body("errores.campo", hasItems("inicioEn", "nombre", "plazoDisputaHoras"));
        editar(master, id, Map.of())
                .then()
                .statusCode(400)
                .body("codigo", equalTo("DATOS_NO_VALIDOS"));

        ver(master, id).then().body("nombre", equalTo("Killer Firme"));
        List<String> tipos = jdbc.queryForList(
                "select tipo from evento where partida_id = ?", String.class, UUID.fromString(id));
        assertFalse(tipos.contains("CONFIGURACION_CAMBIADA"));
    }

    @Test
    void partidaQueYaNoEstaEnInscripcionDa409() {
        String master = registrar();
        String id = crearId(master, "Killer Arrancado");
        jdbc.update("update partida set estado = 'EN_JUEGO' where id = ?", UUID.fromString(id));

        editar(master, id, Map.of("nombre", "Killer Tarde"))
                .then()
                .statusCode(409)
                .body("codigo", equalTo("PARTIDA_NO_EDITABLE"));
    }

    @Test
    void sinTokenDa401() {
        given()
                .port(puerto)
                .when()
                .get(RUTA_PARTIDA, UUID.randomUUID().toString())
                .then()
                .statusCode(401)
                .body("codigo", equalTo("NO_AUTENTICADO"));
    }

    private String codigoDe(String id) {
        return jdbc.queryForObject("select codigo_invitacion from partida where id = ?",
                String.class, UUID.fromString(id));
    }

    private String crearId(String token, String nombre) {
        return crear(token, configuracion(nombre, manana()))
                .then().statusCode(201).extract().path("id");
    }

    private Response crear(String token, Map<String, Object> cuerpo) {
        return given()
                .port(puerto)
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(cuerpo)
                .when()
                .post(RUTA_PARTIDAS);
    }

    private Response ver(String token, String id) {
        return given()
                .port(puerto)
                .auth().oauth2(token)
                .when()
                .get(RUTA_PARTIDA, id);
    }

    private Response editar(String token, String id, Map<String, Object> cambios) {
        return given()
                .port(puerto)
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(cambios)
                .when()
                .patch(RUTA_PARTIDA, id);
    }

    private static Instant manana() {
        return Instant.now().plus(Duration.ofDays(1)).truncatedTo(ChronoUnit.SECONDS);
    }

    private static Map<String, Object> configuracion(String nombre, Instant inicio) {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("nombre", nombre);
        cuerpo.put("inicioEn", inicio.toString());
        cuerpo.put("finEn", inicio.plus(Duration.ofDays(7)).toString());
        cuerpo.put("tipoMisiones", "BIBLIOTECA");
        cuerpo.put("plazoDisputaHoras", 6);
        cuerpo.put("entradasTardias", false);
        cuerpo.put("masterJuega", true);
        return cuerpo;
    }

    private String registrar() {
        String alias = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return given()
                .port(puerto)
                .contentType(ContentType.JSON)
                .body(Map.of("alias", alias, "email", alias + "@example.com",
                        "contrasena", "fantasma2026"))
                .when()
                .post("/api/v1/cuentas")
                .then()
                .statusCode(201)
                .extract()
                .path("tokenAcceso");
    }
}
