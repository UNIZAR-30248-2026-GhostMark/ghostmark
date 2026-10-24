package es.unizar.ghostmark.backend.cuenta;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import es.unizar.ghostmark.backend.TestcontainersConfiguration;
import io.restassured.http.ContentType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

/**
 * Pruebas de integración del horario de un usuario.
 * guardarYConsultarHorario: comprueba que se puede guardar un horario y luego consultarlo.
 * sustituirYVaciarHorario: comprueba que se puede sustituir un horario por otro y luego vaciarlo.
 * rechazarHorarioInvalidoSinModificarAnterior: comprueba que si se intenta guardar un horario inválido, no se modifica el horario anterior.
 * cadaUsuarioTieneSuPropioHorario: comprueba que cada usuario tiene su propio horario y no se mezclan.
 * cuerpoNoValidoDa400: comprueba que un JSON que no es un horario da 400 y no 401.
 * cuentaBorradaDa401ConCodigo: comprueba que con la cuenta borrada el 401 lleva su código.
 */

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HorarioTest {

  private static final String RUTA = "/api/v1/cuentas/yo/horario";

  @LocalServerPort
  int puerto;

  @Test
  void guardarYConsultarHorario() {
    String token = registrar("horario_a", "horario_a@example.com");

    given()
        .port(puerto)
        .header("Authorization", "Bearer " + token)
        .when()
        .get(RUTA)
        .then()
        .statusCode(200)
        .body("franjas.size()", equalTo(0));

    given()
        .port(puerto)
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body(Map.of("franjas", List.of(
            Map.of("dia", 3, "horaInicio", "15:00"),
            Map.of("dia", 1, "horaInicio", "08:30"))))
        .when()
        .put(RUTA)
        .then()
        .statusCode(200)
        .body("franjas.size()", equalTo(2))
        .body("franjas[0].dia", equalTo(1))
        .body("franjas[0].horaInicio", equalTo("08:30"));

    given()
        .port(puerto)
        .header("Authorization", "Bearer " + token)
        .when()
        .get(RUTA)
        .then()
        .statusCode(200)
        .body("franjas.size()", equalTo(2));
  }

  @Test
  void sustituirYVaciarHorario() {
    String token = registrar("horario_b", "horario_b@example.com");

    given()
        .port(puerto)
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body(Map.of("franjas", List.of(
            Map.of("dia", 1, "horaInicio", "08:00"),
            Map.of("dia", 1, "horaInicio", "08:30"))))
        .when()
        .put(RUTA)
        .then()
        .statusCode(200);

    given()
        .port(puerto)
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body(Map.of("franjas", List.of(
            Map.of("dia", 5, "horaInicio", "20:30"))))
        .when()
        .put(RUTA)
        .then()
        .statusCode(200)
        .body("franjas.size()", equalTo(1))
        .body("franjas[0].dia", equalTo(5));

    given()
        .port(puerto)
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body(Map.of("franjas", List.of()))
        .when()
        .put(RUTA)
        .then()
        .statusCode(200)
        .body("franjas.size()", equalTo(0));
  }

  @Test
  void rechazarHorarioInvalidoSinModificarAnterior() {
    String token = registrar("horario_c", "horario_c@example.com");

    given()
        .port(puerto)
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body(Map.of("franjas", List.of(
            Map.of("dia", 1, "horaInicio", "08:00"))))
        .when()
        .put(RUTA)
        .then()
        .statusCode(200);

    given()
        .port(puerto)
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body(Map.of("franjas", List.of(
            Map.of("dia", 6, "horaInicio", "09:15"),
            Map.of("dia", 1, "horaInicio", "08:30"),
            Map.of("dia", 1, "horaInicio", "08:30"))))
        .when()
        .put(RUTA)
        .then()
        .statusCode(400)
        .body("codigo", equalTo("DATOS_NO_VALIDOS"));

    given()
        .port(puerto)
        .header("Authorization", "Bearer " + token)
        .when()
        .get(RUTA)
        .then()
        .statusCode(200)
        .body("franjas.size()", equalTo(1))
        .body("franjas[0].horaInicio", equalTo("08:00"));
  }

  @Test
  void cadaUsuarioTieneSuPropioHorario() {
    String tokenA = registrar("horario_d", "horario_d@example.com");
    String tokenB = registrar("horario_e", "horario_e@example.com");

    given()
        .port(puerto)
        .header("Authorization", "Bearer " + tokenA)
        .contentType(ContentType.JSON)
        .body(Map.of("franjas", List.of(
            Map.of("dia", 2, "horaInicio", "10:00"))))
        .when()
        .put(RUTA)
        .then()
        .statusCode(200);

    given()
        .port(puerto)
        .header("Authorization", "Bearer " + tokenB)
        .when()
        .get(RUTA)
        .then()
        .statusCode(200)
        .body("franjas.size()", equalTo(0));
  }

  @Test
  void cuerpoNoValidoDa400() {
    String token = registrar("horario_g", "horario_g@example.com");

    given()
        .port(puerto)
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body("{\"franjas\": 3}")
        .when()
        .put(RUTA)
        .then()
        .statusCode(400)
        .body("codigo", equalTo("DATOS_NO_VALIDOS"));
  }

  @Test
  void cuentaBorradaDa401ConCodigo() {
    String token = registrar("horario_h", "horario_h@example.com");

    given()
        .port(puerto)
        .header("Authorization", "Bearer " + token)
        .when()
        .delete("/api/v1/cuentas/yo")
        .then()
        .statusCode(204);

    given()
        .port(puerto)
        .header("Authorization", "Bearer " + token)
        .when()
        .get(RUTA)
        .then()
        .statusCode(401)
        .body("codigo", equalTo("NO_AUTENTICADO"));
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
