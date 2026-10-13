package es.unizar.ghostmark.backend.cuenta;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import es.unizar.ghostmark.backend.TestcontainersConfiguration;
import io.restassured.http.ContentType;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

/*
registraAceptacionesSoloUnaVez: comprueba que se pueden registrar las aceptaciones de ubicación y aviso de disponibilidad solo una vez, y que no se modifican si se intenta registrarlas de nuevo.
rechazaAceptacionesInvalidas: comprueba que si se intenta registrar aceptaciones inválidas (por ejemplo, no aceptar la ubicación), se devuelve un error y no se modifican las aceptaciones anteriores.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AceptacionesTest {

  private static final String RUTA = "/api/v1/cuentas/yo/aceptaciones";

  @LocalServerPort
  int puerto;

  @Autowired
  CuentaRepository cuentas;

  @Test
  void registraAceptacionesSoloUnaVez() {
    String token = registrar("acepta_a", "acepta_a@example.com");

    Cuenta inicial = cuentas.findByEmail("acepta_a@example.com").orElseThrow();
    assertNull(inicial.getUbicacionAceptadaEn());
    assertNull(inicial.getAvisoDisponibilidadAceptadoEn());

    given()
        .port(puerto)
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body(Map.of(
            "ubicacion", true,
            "avisoDisponibilidad", true))
        .when()
        .put(RUTA)
        .then()
        .statusCode(200)
        .body("ubicacionAceptada", equalTo(true))
        .body("avisoDisponibilidadAceptado", equalTo(true));

    Cuenta primera = cuentas.findByEmail("acepta_a@example.com").orElseThrow();

    Instant fechaUbicacion = primera.getUbicacionAceptadaEn();
    Instant fechaAviso = primera.getAvisoDisponibilidadAceptadoEn();

    assertNotNull(fechaUbicacion);
    assertNotNull(fechaAviso);

    given()
        .port(puerto)
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body(Map.of(
            "ubicacion", true,
            "avisoDisponibilidad", true))
        .when()
        .put(RUTA)
        .then()
        .statusCode(200);

    Cuenta segunda = cuentas.findByEmail("acepta_a@example.com").orElseThrow();

    assertEquals(fechaUbicacion, segunda.getUbicacionAceptadaEn());
    assertEquals(fechaAviso, segunda.getAvisoDisponibilidadAceptadoEn());
  }

  @Test
  void rechazaAceptacionesInvalidas() {
    String token = registrar("acepta_b", "acepta_b@example.com");

    given()
        .port(puerto)
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body(Map.of(
            "ubicacion", false,
            "avisoDisponibilidad", true))
        .when()
        .put(RUTA)
        .then()
        .statusCode(400)
        .body("codigo", equalTo("DATOS_NO_VALIDOS"));

    given()
        .port(puerto)
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body(Map.of("ubicacion", true))
        .when()
        .put(RUTA)
        .then()
        .statusCode(400);

    Cuenta cuenta = cuentas.findByEmail("acepta_b@example.com").orElseThrow();

    assertNull(cuenta.getUbicacionAceptadaEn());
    assertNull(cuenta.getAvisoDisponibilidadAceptadoEn());
  }

  private String registrar(String alias, String email) {
    return given()
        .port(puerto)
        .contentType(ContentType.JSON)
        .body(Map.of(
            "alias", alias,
            "email", email,
            "contrasena", "fantasma2026"))
        .when()
        .post("/api/v1/cuentas")
        .then()
        .statusCode(201)
        .extract()
        .path("tokenAcceso");
  }
}