package es.unizar.ghostmark.backend.sesion;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import es.unizar.ghostmark.backend.TestcontainersConfiguration;
import es.unizar.ghostmark.backend.cuenta.Cuenta;
import es.unizar.ghostmark.backend.cuenta.CuentaRepository;
import es.unizar.ghostmark.backend.cuenta.TokenRefresco;
import es.unizar.ghostmark.backend.cuenta.TokenRefrescoRepository;
import io.restassured.http.ContentType;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SesionTest {

    @LocalServerPort
    int puerto;

    @Autowired
    CuentaRepository cuentas;

    @Autowired
    TokenRefrescoRepository refrescos;

    @Autowired
    ServicioSesion sesiones;

    @Test
    void registroAbreSensionYLoginFunciona() {
        Map<String, Object> sesion =
                given()
                        .port(puerto)
                        .contentType(ContentType.JSON)
                        .body(
                                Map.of(
                                        "alias", "imad_h",
                                        "email", "imad@example.com",
                                        "contrasena", "fantasma2026"))
                        .when()
                        .post("/api/v1/cuentas")
                        .then()
                        .statusCode(201)
                        .body("tokenAcceso", notNullValue())
                        .body("tokenRefresco", notNullValue())
                        .body("cuenta.alias", equalTo("imad_h"))
                        .extract()
                        .as(Map.class);

        given()
                .port(puerto)
                .contentType(ContentType.JSON)
                .body(Map.of("email", "imad@example.com", "contrasena", "fantasma2026"))
                .when()
                .post("/api/v1/sesiones")
                .then()
                .statusCode(200)
                .body("cuenta.alias", equalTo("imad_h"));

        Cuenta guardada = cuentas.findByEmail("imad@example.com").orElseThrow();
        assertNotEquals("fantasma2026", guardada.getHashContrasena());
        assertTrue(guardada.getHashContrasena().startsWith("$2"));
    }

    @Test
    void duplicadosYDatosMalDanError() {
        registrar("pablo_r", "pablo@example.com");

        given()
                .port(puerto)
                .contentType(ContentType.JSON)
                .body(
                        Map.of("alias", "pablo_r", "email", "otro@example.com", "contrasena",
                                "fantasma2026"))
                .when()
                .post("/api/v1/cuentas")
                .then()
                .statusCode(409)
                .body("codigo", equalTo("ALIAS_OCUPADO"));

        given()
                .port(puerto)
                .contentType(ContentType.JSON)
                .body(Map.of("alias", "otro_x", "email", "PABLO@example.com", "contrasena",
                        "fantasma2026"))
                .when()
                .post("/api/v1/cuentas")
                .then()
                .statusCode(409)
                .body("codigo", equalTo("EMAIL_OCUPADO"));

        given()
                .port(puerto)
                .contentType(ContentType.JSON)
                .body(Map.of("alias", "x", "email", "mal", "contrasena", "corta"))
                .when()
                .post("/api/v1/cuentas")
                .then()
                .statusCode(400)
                .body("codigo", equalTo("DATOS_NO_VALIDOS"));

        given()
                .port(puerto)
                .contentType(ContentType.JSON)
                .body(Map.of("email", "pablo@example.com", "contrasena", "otra2026xx"))
                .when()
                .post("/api/v1/sesiones")
                .then()
                .statusCode(401)
                .body("codigo", equalTo("CREDENCIALES_INCORRECTAS"));

        given()
                .port(puerto)
                .contentType(ContentType.JSON)
                .body(Map.of("email", "nadie@example.com", "contrasena", "fantasma2026"))
                .when()
                .post("/api/v1/sesiones")
                .then()
                .statusCode(401)
                .body("codigo", equalTo("CREDENCIALES_INCORRECTAS"));
    }

    @Test
    void loginConDatosMalFormadosDa400() {
        given()
                .port(puerto)
                .contentType(ContentType.JSON)
                .body(Map.of("email", "no-es-email", "contrasena", "123"))
                .when()
                .post("/api/v1/sesiones")
                .then()
                .statusCode(400)
                .body("codigo", equalTo("DATOS_NO_VALIDOS"));
    }

    @Test
    void rutaProtegidaSinTokenDa401NoAutenticado() {
        given()
                .port(puerto)
                .contentType(ContentType.JSON)
                .body(Map.of("tokenRefresco", "cualquiera"))
                .when()
                .post("/api/v1/sesiones/cierre")
                .then()
                .statusCode(401)
                .header("Content-Type", containsString("application/problem+json"))
                .body("codigo", equalTo("NO_AUTENTICADO"));
    }

    @Test
    void refrescoRotaYCierreRevoca() {
        Map<String, Object> sesion = registrar("lucia_m", "lucia@example.com");
        String refresco = (String) sesion.get("tokenRefresco");
        String acceso = (String) sesion.get("tokenAcceso");

        Map<String, Object> renovada =
                given()
                        .port(puerto)
                        .contentType(ContentType.JSON)
                        .body(Map.of("tokenRefresco", refresco))
                        .when()
                        .post("/api/v1/sesiones/refresco")
                        .then()
                        .statusCode(200)
                        .body("tokenRefresco", notNullValue())
                        .extract()
                        .as(Map.class);

        given()
                .port(puerto)
                .contentType(ContentType.JSON)
                .body(Map.of("tokenRefresco", refresco))
                .when()
                .post("/api/v1/sesiones/refresco")
                .then()
                .statusCode(401)
                .body("codigo", equalTo("TOKEN_NO_VALIDO"));

        given()
                .port(puerto)
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + acceso)
                .body(Map.of("tokenRefresco", renovada.get("tokenRefresco")))
                .when()
                .post("/api/v1/sesiones/cierre")
                .then()
                .statusCode(204);

        given()
                .port(puerto)
                .contentType(ContentType.JSON)
                .body(Map.of("tokenRefresco", renovada.get("tokenRefresco")))
                .when()
                .post("/api/v1/sesiones/refresco")
                .then()
                .statusCode(401);
    }

    @Test
    void cierreConTokenAjenoNoLoRevoca() {
        String accesoAna = (String) registrar("ana_c", "ana_c@example.com").get("tokenAcceso");
        String refrescoBruno =
                (String) registrar("bruno_c", "bruno_c@example.com").get("tokenRefresco");

        // Responde igual que con un token que no existe (#97).
        given()
                .port(puerto)
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + accesoAna)
                .body(Map.of("tokenRefresco", refrescoBruno))
                .when()
                .post("/api/v1/sesiones/cierre")
                .then()
                .statusCode(204);

        given()
                .port(puerto)
                .contentType(ContentType.JSON)
                .body(Map.of("tokenRefresco", refrescoBruno))
                .when()
                .post("/api/v1/sesiones/refresco")
                .then()
                .statusCode(200);
    }

    @Test
    void limpiezaBorraRefrescosCaducadosHaceMasDe30Dias() {
        Map<String, Object> sesion = registrar("dani_t", "dani@example.com");
        Cuenta cuenta = cuentas.findByEmail("dani@example.com").orElseThrow();
        Instant ahora = Instant.now();
        TokenRefresco viejo =
                new TokenRefresco(
                        cuenta, ServicioSesion.sha256("viejo"), ahora.minus(Duration.ofDays(31)),
                        ahora);
        refrescos.save(viejo);
        TokenRefresco reciente =
                new TokenRefresco(
                        cuenta, ServicioSesion.sha256("reciente"),
                        ahora.minus(Duration.ofDays(10)), ahora);
        refrescos.save(reciente);

        sesiones.borrarRefrescosCaducados();

        assertTrue(refrescos.findByHashToken(ServicioSesion.sha256("viejo")).isEmpty());
        assertTrue(refrescos.findByHashToken(ServicioSesion.sha256("reciente")).isPresent());
    }

    private Map<String, Object> registrar(String alias, String email) {
        return given()
                .port(puerto)
                .contentType(ContentType.JSON)
                .body(Map.of("alias", alias, "email", email, "contrasena", "fantasma2026"))
                .when()
                .post("/api/v1/cuentas")
                .then()
                .statusCode(201)
                .extract()
                .as(Map.class);
    }
}
