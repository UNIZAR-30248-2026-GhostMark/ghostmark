package es.unizar.ghostmark.backend.cuenta;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import es.unizar.ghostmark.backend.TestcontainersConfiguration;
import io.restassured.http.ContentType;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import es.unizar.ghostmark.backend.cuenta.repositories.FranjaHorarioRepository;

/*
borrarCuentaEliminaDatosEImpideIniciarSesion: comprueba que al borrar una cuenta, se eliminan sus datos y no se puede iniciar sesión con ella.
*/
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BorradoCuentaTest {

    @LocalServerPort
    int puerto;

    @Autowired
    CuentaRepository cuentas;

    @Autowired
    FranjaHorarioRepository franjas;

    @Test
    void borrarCuentaEliminaDatosEImpideIniciarSesion() {
        String alias = "borrar_u";
        String email = "borrar@example.com";
        String contrasena = "fantasma2026";

        var registro = given()
            .port(puerto)
            .contentType(ContentType.JSON)
            .body(Map.of(
                "alias", alias,
                "email", email,
                "contrasena", contrasena))
            .when()
            .post("/api/v1/cuentas")
            .then()
            .statusCode(201)
            .extract()
            .response();

        String acceso = registro.path("tokenAcceso");
        String refresco = registro.path("tokenRefresco");

        UUID cuentaId = cuentas.findByEmail(email).orElseThrow().getId();

        given()
            .port(puerto)
            .header("Authorization", "Bearer " + acceso)
            .contentType(ContentType.JSON)
            .body(Map.of(
                "franjas", List.of(
                    Map.of("dia", 1, "horaInicio", "08:00"))))
            .when()
            .put("/api/v1/cuentas/yo/horario")
            .then()
            .statusCode(200);

        assertFalse(franjas.buscarPorCuentaId(cuentaId).isEmpty());

        given()
            .port(puerto)
            .header("Authorization", "Bearer " + acceso)
            .when()
            .delete("/api/v1/cuentas/yo")
            .then()
            .statusCode(204);

        // La cuenta y su horario ya no existen.
        assertTrue(cuentas.findById(cuentaId).isEmpty());
        assertTrue(franjas.buscarPorCuentaId(cuentaId).isEmpty());

        // Ya no puede iniciar sesión.
        given()
            .port(puerto)
            .contentType(ContentType.JSON)
            .body(Map.of(
                "email", email,
                "contrasena", contrasena))
            .when()
            .post("/api/v1/sesiones")
            .then()
            .statusCode(401);

        // El token de refresco anterior deja de funcionar.
        given()
            .port(puerto)
            .contentType(ContentType.JSON)
            .body(Map.of("tokenRefresco", refresco))
            .when()
            .post("/api/v1/sesiones/refresco")
            .then()
            .statusCode(401);

        // El token de acceso anterior tampoco permite consultar el horario.
        given()
            .port(puerto)
            .header("Authorization", "Bearer " + acceso)
            .when()
            .get("/api/v1/cuentas/yo/horario")
            .then()
            .statusCode(401);

        // El alias y el email quedan disponibles para otra cuenta.
        given()
            .port(puerto)
            .contentType(ContentType.JSON)
            .body(Map.of(
                "alias", alias,
                "email", email,
                "contrasena", contrasena))
            .when()
            .post("/api/v1/cuentas")
            .then()
            .statusCode(201);
    }
}