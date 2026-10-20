package es.unizar.ghostmark.backend.cuenta;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import es.unizar.ghostmark.backend.TestcontainersConfiguration;
import es.unizar.ghostmark.backend.cuenta.repositories.FranjaHorarioRepository;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CuentaIntegracionTest {

    private static final String RUTA_CUENTAS = "/api/v1/cuentas";
    private static final String RUTA_SESIONES = "/api/v1/sesiones";
    private static final String RUTA_HORARIO = "/api/v1/cuentas/yo/horario";
    private static final String RUTA_ACEPTACIONES = "/api/v1/cuentas/yo/aceptaciones";

    @LocalServerPort
    int puerto;

    @Autowired
    CuentaRepository cuentas;

    @Autowired
    FranjaHorarioRepository franjas;

    @Test
    void registroConfiguracionYBorradoDeCuenta() {
        String alias = UUID.randomUUID().toString().substring(0, 8);
        String email = alias + "@example.com";
        String contrasena = "fantasma2026";

        // Registra una cuenta nueva y comprueba que se generan los tokens de sesión.
        Response registro = given()
                .port(puerto)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "alias", alias,
                        "email", email,
                        "contrasena", contrasena))
                .when()
                .post(RUTA_CUENTAS)
                .then()
                .statusCode(201)
                .body("cuenta.alias", equalTo(alias))
                .extract()
                .response();

        String tokenRefresco = registro.path("tokenRefresco");
        assertNotNull(registro.path("tokenAcceso"));
        assertNotNull(tokenRefresco);

        // Comprueba que no se exponen datos sensibles y que la contraseña se almacena como un hash, nunca en texto plano.
        Map<String, Object> datosPublicos = registro.path("cuenta");
        assertFalse(datosPublicos.containsKey("contrasena"));
        assertFalse(datosPublicos.containsKey("hashContrasena"));
        assertFalse(datosPublicos.containsKey("latitud"));
        assertFalse(datosPublicos.containsKey("longitud"));

        Cuenta creada = cuentas.findByEmail(email).orElseThrow();
        UUID cuentaId = creada.getId();
        assertNotEquals(contrasena, creada.getHashContrasena());
        assertTrue(creada.getHashContrasena().startsWith("$2"));

        // Verifica que las credenciales incorrectas se rechazan y que las correctas permiten iniciar sesión.
        given()
                .port(puerto)
                .contentType(ContentType.JSON)
                .body(Map.of("email", email, "contrasena", "equivocada2026"))
                .when()
                .post(RUTA_SESIONES)
                .then()
                .statusCode(401)
                .body("codigo", equalTo("CREDENCIALES_INCORRECTAS"));

        String tokenAcceso = given()
                .port(puerto)
                .contentType(ContentType.JSON)
                .body(Map.of("email", email, "contrasena", contrasena))
                .when()
                .post(RUTA_SESIONES)
                .then()
                .statusCode(200)
                .body("cuenta.alias", equalTo(alias))
                .extract()
                .path("tokenAcceso");

        // Guarda un horario lectivo y comprueba que puede recuperarse correctamente utilizando el token de la cuenta.
        given()
                .port(puerto)
                .header("Authorization", "Bearer " + tokenAcceso)
                .contentType(ContentType.JSON)
                .body(Map.of("franjas", List.of(
                        Map.of("dia", 1, "horaInicio", "08:30"),
                        Map.of("dia", 3, "horaInicio", "15:00"))))
                .when()
                .put(RUTA_HORARIO)
                .then()
                .statusCode(200);

        given()
                .port(puerto)
                .header("Authorization", "Bearer " + tokenAcceso)
                .when()
                .get(RUTA_HORARIO)
                .then()
                .statusCode(200)
                .body("franjas.size()", equalTo(2))
                .body("franjas[0].dia", equalTo(1))
                .body("franjas[0].horaInicio", equalTo("08:30"));

        // Registra las aceptaciones y verifica que repetir la operación no modifica las fechas originales.
        given()
                .port(puerto)
                .header("Authorization", "Bearer " + tokenAcceso)
                .contentType(ContentType.JSON)
                .body(Map.of("ubicacion", true, "avisoDisponibilidad", true))
                .when()
                .put(RUTA_ACEPTACIONES)
                .then()
                .statusCode(200)
                .body("ubicacionAceptada", equalTo(true))
                .body("avisoDisponibilidadAceptado", equalTo(true));

        Cuenta primera = cuentas.findById(cuentaId).orElseThrow();
        Instant fechaUbicacion = primera.getUbicacionAceptadaEn();
        Instant fechaAviso = primera.getAvisoDisponibilidadAceptadoEn();
        assertNotNull(fechaUbicacion);
        assertNotNull(fechaAviso);

        given()
                .port(puerto)
                .header("Authorization", "Bearer " + tokenAcceso)
                .contentType(ContentType.JSON)
                .body(Map.of("ubicacion", true, "avisoDisponibilidad", true))
                .when()
                .put(RUTA_ACEPTACIONES)
                .then()
                .statusCode(200);

        Cuenta segunda = cuentas.findById(cuentaId).orElseThrow();
        assertEquals(fechaUbicacion, segunda.getUbicacionAceptadaEn());
        assertEquals(fechaAviso, segunda.getAvisoDisponibilidadAceptadoEn());

        // Elimina la cuenta y comprueba que desaparecen sus datos, que el alias queda libre y que las credenciales dejan de funcionar.
        given()
                .port(puerto)
                .header("Authorization", "Bearer " + tokenAcceso)
                .when()
                .delete("/api/v1/cuentas/yo")
                .then()
                .statusCode(204);

        assertTrue(cuentas.findById(cuentaId).isEmpty());
        assertTrue(franjas.buscarPorCuentaId(cuentaId).isEmpty());
        assertFalse(cuentas.existsByAliasIgnoreCase(alias));

        given()
                .port(puerto)
                .contentType(ContentType.JSON)
                .body(Map.of("email", email, "contrasena", contrasena))
                .when()
                .post(RUTA_SESIONES)
                .then()
                .statusCode(401);

        given()
                .port(puerto)
                .contentType(ContentType.JSON)
                .body(Map.of("tokenRefresco", tokenRefresco))
                .when()
                .post("/api/v1/sesiones/refresco")
                .then()
                .statusCode(401);
    }
}