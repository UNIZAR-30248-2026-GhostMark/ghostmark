package es.unizar.ghostmark.backend.inscripcion;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;

import es.unizar.ghostmark.backend.TestcontainersConfiguration;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("PBI 3 - aceptación de la inscripción mediante la API real")
class AceptacionInscripcionTest {

    @LocalServerPort
    int puerto;

    @ParameterizedTest(name = "CA2 - aceptación ausente o nula: {0}")
    @ValueSource(strings = {
        "{\"codigo\":\"%s\"}",
        "{\"codigo\":\"%s\",\"aceptaNormas\":null}"
    })
    void ca2SinAceptacionNoApareceEnNingunaLista(String cuerpo) {
        Persona master = registrar();
        Persona jugador = registrar();
        Juego juego = crear(master);

        peticion(jugador).body(cuerpo.formatted(juego.codigo()))
                .post("/api/v1/inscripciones").then().statusCode(400);

        inscritos(master, juego).then().statusCode(200).body("size()", equalTo(0));
        peticion(jugador).get("/api/v1/cuentas/yo/partidas").then()
                .statusCode(200).body("size()", equalTo(0));
        peticion(jugador).get("/api/v1/partidas/{id}/yo/horario", juego.id())
                .then().statusCode(404);
    }

    @Test
    void ca3CambiarElHorarioGeneralNoReescribeElDeLaPartida() {
        Persona jugador = registrar();
        guardarGeneral(jugador, 1, "08:00");
        Juego juego = crear(registrar());
        inscribir(jugador, juego);

        guardarGeneral(jugador, 3, "12:00");

        peticion(jugador).get("/api/v1/cuentas/yo/horario").then().statusCode(200)
                .body("franjas", equalTo(franjas(3, "12:00")));
        consultarHorario(jugador, juego).then().statusCode(200)
                .body("franjas", equalTo(franjas(1, "08:00")));
    }

    @Test
    void ca3RevisarYEditarHorarioAntesDelArranqueConservaLaInscripcion() {
        Persona master = registrar();
        Persona jugador = registrar();
        guardarGeneral(jugador, 1, "08:00");
        Juego juego = crear(master);
        String inscripcionId = inscribir(jugador, juego).path("id");

        consultarHorario(jugador, juego).then().statusCode(200)
                .body("franjas", equalTo(franjas(1, "08:00")));
        guardarHorario(jugador, juego, 2, "09:30").then().statusCode(200)
                .body("franjas", equalTo(franjas(2, "09:30")));
        consultarHorario(jugador, juego).then().statusCode(200)
                .body("franjas", equalTo(franjas(2, "09:30")));
        inscritos(master, juego).then().statusCode(200).body("size()", equalTo(1))
                .body("[0].id", equalTo(inscripcionId));
    }

    @Test
    void ca4HorarioIndependienteEntrePartidasYEntreJugadores() {
        Persona master = registrar();
        Persona jugador = registrar();
        Persona otro = registrar();
        guardarGeneral(jugador, 1, "08:00");
        guardarGeneral(otro, 4, "10:00");
        Juego primera = crear(master);
        Juego segunda = crear(master);
        String primeraId = inscribir(jugador, primera).path("id");
        String segundaId = inscribir(jugador, segunda).path("id");
        inscribir(otro, primera);

        guardarHorario(jugador, primera, 2, "09:30").then().statusCode(200);

        consultarHorario(jugador, primera).then().statusCode(200)
                .body("franjas", equalTo(franjas(2, "09:30")));
        consultarHorario(jugador, segunda).then().statusCode(200)
                .body("franjas", equalTo(franjas(1, "08:00")));
        consultarHorario(otro, primera).then().statusCode(200)
                .body("franjas", equalTo(franjas(4, "10:00")));
        peticion(jugador).get("/api/v1/cuentas/yo/horario").then().statusCode(200)
                .body("franjas", equalTo(franjas(1, "08:00")));
        peticion(jugador).get("/api/v1/cuentas/yo/partidas").then().statusCode(200)
                .body("size()", equalTo(2)).body("id", hasItems(primera.id(), segunda.id()));
        inscritos(master, primera).then().statusCode(200).body("size()", equalTo(2))
                .body("id", hasItems(primeraId));
        inscritos(master, segunda).then().statusCode(200).body("size()", equalTo(1))
                .body("[0].id", equalTo(segundaId));
    }

    @Test
    void rolAjenoNoVeLaPartidaEnSusPartidasNiAccedeADatosPrivados() {
        Persona master = registrar();
        Persona jugador = registrar();
        Persona ajeno = registrar();
        Juego juego = crear(master);
        inscribir(jugador, juego);

        peticion(ajeno).get("/api/v1/cuentas/yo/partidas").then().statusCode(200)
                .body("size()", equalTo(0));
        inscritos(ajeno, juego).then().statusCode(404)
                .body("codigo", equalTo("NO_ENCONTRADA"));
        consultarHorario(ajeno, juego).then().statusCode(404)
                .body("codigo", equalTo("NO_ENCONTRADA"));
        guardarHorario(ajeno, juego, 2, "09:30").then().statusCode(404);

        // La invitación es pública para una cuenta autenticada; no implica inscripción.
        peticion(ajeno).get("/api/v1/partidas/codigo/{codigo}", juego.codigo())
                .then().statusCode(200);
        inscritos(master, juego).then().statusCode(200).body("size()", equalTo(1))
                .body("[0].alias", equalTo(jugador.alias()));
    }

    private RequestSpecification peticion(Persona persona) {
        return given().port(puerto).auth().oauth2(persona.token()).contentType(ContentType.JSON);
    }

    private Persona registrar() {
        String alias = "a_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Response respuesta = given().port(puerto).contentType(ContentType.JSON)
                .body(Map.of("alias", alias, "email", alias + "@example.com",
                        "contrasena", "fantasma2026"))
                .post("/api/v1/cuentas").then().statusCode(201).extract().response();
        return new Persona(respuesta.path("tokenAcceso"), alias);
    }

    private Juego crear(Persona master) {
        Instant inicio = Instant.now().plus(Duration.ofDays(2));
        Response respuesta = peticion(master).body(Map.of(
                "nombre", "Aceptacion PBI 3", "inicioEn", inicio.toString(),
                "finEn", inicio.plus(Duration.ofDays(7)).toString(),
                "tipoMisiones", "BIBLIOTECA", "plazoDisputaHoras", 6,
                "entradasTardias", false, "masterJuega", false))
                .post("/api/v1/partidas").then().statusCode(201).extract().response();
        return new Juego(respuesta.path("id"), respuesta.path("codigoInvitacion"));
    }

    private Response inscribir(Persona jugador, Juego juego) {
        return peticion(jugador).body(Map.of("codigo", juego.codigo(), "aceptaNormas", true))
                .post("/api/v1/inscripciones").then().statusCode(201).extract().response();
    }

    private Response inscritos(Persona master, Juego juego) {
        return peticion(master).get("/api/v1/partidas/{id}/inscritos", juego.id());
    }

    private Response consultarHorario(Persona jugador, Juego juego) {
        return peticion(jugador).get("/api/v1/partidas/{id}/yo/horario", juego.id());
    }

    private Response guardarHorario(Persona jugador, Juego juego, int dia, String hora) {
        return peticion(jugador).body(Map.of("franjas", franjas(dia, hora)))
                .put("/api/v1/partidas/{id}/yo/horario", juego.id());
    }

    private void guardarGeneral(Persona jugador, int dia, String hora) {
        peticion(jugador).body(Map.of("franjas", franjas(dia, hora)))
                .put("/api/v1/cuentas/yo/horario").then().statusCode(200);
    }

    private List<Map<String, Object>> franjas(int dia, String hora) {
        return List.of(Map.of("dia", dia, "horaInicio", hora));
    }

    private record Persona(String token, String alias) {
    }

    private record Juego(String id, String codigo) {
    }
}
