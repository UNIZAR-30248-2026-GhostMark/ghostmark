package es.unizar.ghostmark.backend.partida;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import es.unizar.ghostmark.backend.TestcontainersConfiguration;
import io.restassured.http.ContentType;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PartidaCodigoTest {

    @LocalServerPort
    int puerto;

    @Test
    void jugadorAutenticadoConsultaPartidaPorCodigo() {
        String master = registrar("master_codigo");
        String jugador = registrar("jugador_codigo");
        Instant inicio = Instant.now().plus(Duration.ofDays(1)).truncatedTo(ChronoUnit.SECONDS);
        String codigo = crear(master, inicio);

        given()
                .port(puerto)
                .auth().oauth2(jugador)
                .when()
                .get("/api/v1/partidas/codigo/{codigo}", codigo)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("size()", equalTo(6))
                .body("nombre", equalTo("Killer Campus"))
                .body("estado", equalTo("EN_INSCRIPCION"))
                .body("inicioEn", equalTo(inicio.toString()))
                .body("finEn", equalTo(inicio.plus(Duration.ofDays(7)).toString()))
                .body("aliasMaster", equalTo("master_codigo"))
                .body("numInscritos", equalTo(0));
    }

    @Test
    void codigoDesconocidoDa404() {
        String jugador = registrar("jugador_404");

        given()
                .port(puerto)
                .auth().oauth2(jugador)
                .when()
                .get("/api/v1/partidas/codigo/{codigo}", "ZZZZZZZZ")
                .then()
                .statusCode(404)
                .contentType("application/problem+json")
                .body("codigo", equalTo("NO_ENCONTRADA"));
    }

    @Test
    void consultaSinTokenDa401() {
        given()
                .port(puerto)
                .when()
                .get("/api/v1/partidas/codigo/{codigo}", "ZZZZZZZZ")
                .then()
                .statusCode(401)
                .body("codigo", equalTo("NO_AUTENTICADO"));
    }

    @Test
    void masterBorradoSeMuestraComoNulo() {
        String master = registrar("master_borrado");
        String jugador = registrar("jugador_borrado");
        String codigo = crear(master);

        given()
                .port(puerto)
                .auth().oauth2(master)
                .when()
                .delete("/api/v1/cuentas/yo")
                .then()
                .statusCode(204);

        given()
                .port(puerto)
                .auth().oauth2(jugador)
                .when()
                .get("/api/v1/partidas/codigo/{codigo}", codigo)
                .then()
                .statusCode(200)
                .body("estado", equalTo("CANCELADA"))
                .body("aliasMaster", nullValue());
    }

    private String crear(String token) {
        return crear(token, Instant.now().plus(Duration.ofDays(1)));
    }

    private String crear(String token, Instant inicio) {
        return given()
                .port(puerto)
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "nombre", "Killer Campus",
                        "inicioEn", inicio.toString(),
                        "finEn", inicio.plus(Duration.ofDays(7)).toString(),
                        "tipoMisiones", "BIBLIOTECA",
                        "plazoDisputaHoras", 6,
                        "entradasTardias", false,
                        "masterJuega", true))
                .when()
                .post("/api/v1/partidas")
                .then()
                .statusCode(201)
                .extract()
                .path("codigoInvitacion");
    }

    private String registrar(String alias) {
        String email = UUID.randomUUID() + "@example.com";
        return given()
                .port(puerto)
                .contentType(ContentType.JSON)
                .body(Map.of("alias", alias, "email", email,
                        "contrasena", "fantasma2026"))
                .when()
                .post("/api/v1/cuentas")
                .then()
                .statusCode(201)
                .extract()
                .path("tokenAcceso");
    }
}
