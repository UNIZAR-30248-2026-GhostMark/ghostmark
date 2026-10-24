package es.unizar.ghostmark.backend.partida;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import es.unizar.ghostmark.backend.TestcontainersConfiguration;
import es.unizar.ghostmark.backend.cuenta.CuentaRepository;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PartidaTest {

    @LocalServerPort
    int puerto;

    @Autowired
    CuentaRepository cuentas;

    @Autowired
    PartidaRepository partidas;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void crearPartidaDevuelve201ConMasterYCodigo() {
        String token = registrar("ana_m", "ana@example.com");

        String id = crear(token, configuracion("Killer EINA"))
                .then()
                .statusCode(201)
                .body("nombre", equalTo("Killer EINA"))
                .body("estado", equalTo("EN_INSCRIPCION"))
                .body("tipoMisiones", equalTo("BIBLIOTECA"))
                .body("plazoDisputaHoras", equalTo(6))
                .body("entradasTardias", equalTo(false))
                .body("masterJuega", equalTo(true))
                .body("numInscritos", equalTo(1))
                .body("codigoInvitacion", matchesPattern("^[2-9A-HJKMNP-Z]{8}$"))
                .body("enlace", startsWith("ghostmark://unirse/"))
                .extract()
                .path("id");

        Partida guardada = partidas.findById(UUID.fromString(id)).orElseThrow();
        UUID cuentaId = cuentas.findByEmail("ana@example.com").orElseThrow().getId();
        assertEquals(cuentaId, guardada.getMasterId());
    }

    @Test
    void crearPartidaGuardaEventoPartidaCreada() {
        String token = registrar("berta_p", "berta@example.com");

        String id = crear(token, configuracion("Killer Fiestas"))
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        Map<String, Object> evento = jdbc.queryForMap(
                "select secuencia, tipo, datos->>'nombre' as nombre from evento"
                        + " where partida_id = ? and tipo = 'PARTIDA_CREADA'",
                UUID.fromString(id));
        assertEquals(1L, ((Number) evento.get("secuencia")).longValue());
        assertEquals("PARTIDA_CREADA", evento.get("tipo"));
        assertEquals("Killer Fiestas", evento.get("nombre"));
    }

    @Test
    void finAntesDelInicioDa400() {
        String token = registrar("carlos_q", "carlos@example.com");
        Map<String, Object> cuerpo = configuracion("Killer Campus");
        Instant inicio = Instant.now().plus(Duration.ofDays(2));
        cuerpo.put("inicioEn", inicio.toString());
        cuerpo.put("finEn", inicio.minus(Duration.ofHours(1)).toString());

        crear(token, cuerpo)
                .then()
                .statusCode(400)
                .body("codigo", equalTo("DATOS_NO_VALIDOS"))
                .body("errores.campo", hasItems("finEn"));
    }

    @Test
    void inicioDemasiadoProntoDa400() {
        String token = registrar("dora_r", "dora@example.com");
        Map<String, Object> cuerpo = configuracion("Killer Express");
        cuerpo.put("inicioEn", Instant.now().plus(Duration.ofMinutes(5)).toString());

        crear(token, cuerpo)
                .then()
                .statusCode(400)
                .body("codigo", equalTo("DATOS_NO_VALIDOS"))
                .body("errores.campo", hasItems("inicioEn"));
    }

    @Test
    void reglasNoValidasDan400() {
        String token = registrar("eva_s", "eva@example.com");
        Map<String, Object> cuerpo = configuracion("ab");
        cuerpo.put("plazoDisputaHoras", 3);
        cuerpo.put("tipoMisiones", "OTRO");

        crear(token, cuerpo)
                .then()
                .statusCode(400)
                .body("codigo", equalTo("DATOS_NO_VALIDOS"))
                .body("errores.campo", hasItems("nombre", "plazoDisputaHoras", "tipoMisiones"));
    }

    @Test
    void cuerpoVacioDa400ConTodosLosCampos() {
        String token = registrar("hugo_v", "hugo@example.com");

        crear(token, new HashMap<>())
                .then()
                .statusCode(400)
                .body("codigo", equalTo("DATOS_NO_VALIDOS"))
                .body("errores.campo", hasItems("nombre", "inicioEn", "finEn", "tipoMisiones",
                        "plazoDisputaHoras", "entradasTardias", "masterJuega"));
    }

    @Test
    void rutaDeErrorNoExigeSesion() {
        int estado = given()
                .port(puerto)
                .when()
                .get("/error")
                .then()
                .extract()
                .statusCode();

        assertNotEquals(401, estado);
    }

    @Test
    void sinTokenDa401() {
        given()
                .port(puerto)
                .contentType(ContentType.JSON)
                .body(configuracion("Killer Anonimo"))
                .when()
                .post("/api/v1/partidas")
                .then()
                .statusCode(401)
                .body("codigo", equalTo("NO_AUTENTICADO"));
    }

    @Test
    void misPartidasDevuelveLasQueSoyMaster() {
        String token = registrar("fito_t", "fito@example.com");
        String id = crear(token, configuracion("Killer Propio"))
                .then()
                .statusCode(201)
                .extract()
                .path("id");
        String otro = registrar("gema_u", "gema@example.com");

        given()
                .port(puerto)
                .auth()
                .oauth2(token)
                .when()
                .get("/api/v1/cuentas/yo/partidas")
                .then()
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].id", equalTo(id))
                .body("[0].nombre", equalTo("Killer Propio"))
                .body("[0].estado", equalTo("EN_INSCRIPCION"))
                .body("[0].soyMaster", equalTo(true))
                .body("[0].numInscritos", equalTo(1));

        given()
                .port(puerto)
                .auth()
                .oauth2(otro)
                .when()
                .get("/api/v1/cuentas/yo/partidas")
                .then()
                .statusCode(200)
                .body("size()", equalTo(0));
    }

    @Test
    void misPartidasSinTokenDa401() {
        given()
                .port(puerto)
                .when()
                .get("/api/v1/cuentas/yo/partidas")
                .then()
                .statusCode(401)
                .body("codigo", equalTo("NO_AUTENTICADO"));
    }

    private Response crear(String token, Map<String, Object> cuerpo) {
        return given()
                .port(puerto)
                .auth()
                .oauth2(token)
                .contentType(ContentType.JSON)
                .body(cuerpo)
                .when()
                .post("/api/v1/partidas");
    }

    private static Map<String, Object> configuracion(String nombre) {
        Instant inicio = Instant.now().plus(Duration.ofDays(1));
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
