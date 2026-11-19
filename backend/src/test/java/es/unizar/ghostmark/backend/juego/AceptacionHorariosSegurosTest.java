package es.unizar.ghostmark.backend.juego;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import es.unizar.ghostmark.backend.TestcontainersConfiguration;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DisplayName("PBI 9 - aceptación de horarios seguros")
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AceptacionHorariosSegurosTest {

    private static final String RUTA_ELIMINAR = "/api/v1/partidas/{id}/eliminaciones";
    private static final String RUTA_CARTA = "/api/v1/partidas/{id}/yo/carta";
    private static final String RUTA_CODIGO = "/api/v1/partidas/{id}/yo/codigo";
    private static final List<Map<String, Object>> CLASE_MARTES = List.of(
            Map.of("dia", 2, "horaInicio", "10:00"),
            Map.of("dia", 2, "horaInicio", "10:30"));

    @LocalServerPort
    int puerto;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoBean
    Clock reloj;

    private ZonedDateTime martes;

    @BeforeEach
    void prepararReloj() {
        martes = ZonedDateTime.now(HorarioLectivo.ZONA)
                .with(TemporalAdjusters.next(DayOfWeek.TUESDAY))
                .withHour(10).withMinute(10).withSecond(30).withNano(0);
        fijar(hora(10, 10).plusSeconds(30));
        when(reloj.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @Test
    void criterio1SeRechazaLaEliminacionSiElCazadorEstaEnClase() {
        Mesa mesa = crearPartidaEnJuego();
        Persona cazador = mesa.enClase();
        Persona victima = objetivoDe(mesa, cazador);
        UUID cazadorId = inscripcion(mesa.partida(), cazador);

        for (String codigo : codigoCorrectoEIncorrecto(victima, mesa.partida())) {
            eliminar(cazador, mesa.partida(), codigo)
                    .then().statusCode(409)
                    .body("codigo", equalTo("HORARIO_LECTIVO"))
                    .body("motivo", equalTo("CAZADOR"))
                    .body("disponibleEn", equalTo(hora(11, 0).toString()));
        }
        carta(cazador, mesa.partida()).then().statusCode(200)
                .body("noDisponible.motivo", equalTo("CAZADOR"))
                .body("noDisponible.disponibleEn", equalTo(hora(11, 0).toString()));
        assertEquals("VIVO", carta(victima, mesa.partida()).path("estado"));
        assertEquals(0, intentosFallidos(cazadorId));
    }

    @Test
    void criterio1SeRechazaLaEliminacionSiLaVictimaEstaEnClase() {
        Mesa mesa = crearPartidaEnJuego();
        Persona victima = mesa.enClase();
        Persona cazador = cazadorDe(mesa, victima);
        UUID cazadorId = inscripcion(mesa.partida(), cazador);

        for (String codigo : codigoCorrectoEIncorrecto(victima, mesa.partida())) {
            eliminar(cazador, mesa.partida(), codigo)
                    .then().statusCode(409)
                    .body("codigo", equalTo("HORARIO_LECTIVO"))
                    .body("motivo", equalTo("OBJETIVO"))
                    .body("disponibleEn", equalTo(hora(11, 0).toString()));
        }
        carta(cazador, mesa.partida()).then().statusCode(200)
                .body("noDisponible.motivo", equalTo("OBJETIVO"));
        assertEquals("VIVO", carta(victima, mesa.partida()).path("estado"));
        assertEquals(0, intentosFallidos(cazadorId));
    }

    @Test
    void criterio1FueraDelHorarioLectivoSeAceptaLaEliminacion() {
        fijar(hora(10, 0).minusSeconds(1));
        Mesa mesa = crearPartidaEnJuego();
        Persona cazador = mesa.enClase();
        Persona victima = objetivoDe(mesa, cazador);

        carta(cazador, mesa.partida()).then().statusCode(200)
                .body("noDisponible", nullValue());
        eliminar(cazador, mesa.partida(), codigoDe(victima, mesa.partida()))
                .then().statusCode(200)
                .body("aliasVictima", equalTo(victima.alias()))
                .body("carta.eliminaciones", equalTo(1));
        assertEquals("ELIMINADO", carta(victima, mesa.partida()).path("estado"));
    }

    @Test
    void criterio2LaCartaCambiaAlEmpezarYTerminarLaClase() {
        fijar(hora(10, 0).minusSeconds(1));
        Mesa mesa = crearPartidaEnJuego();
        Persona victima = mesa.enClase();
        Persona cazador = cazadorDe(mesa, victima);

        carta(cazador, mesa.partida()).then().statusCode(200)
                .body("noDisponible", nullValue());
        fijar(hora(10, 0));
        carta(cazador, mesa.partida()).then().statusCode(200)
                .body("noDisponible.motivo", equalTo("OBJETIVO"))
                .body("noDisponible.disponibleEn", equalTo(hora(11, 0).toString()));
        fijar(hora(10, 30));
        carta(cazador, mesa.partida()).then().statusCode(200)
                .body("noDisponible.motivo", equalTo("OBJETIVO"))
                .body("noDisponible.disponibleEn", equalTo(hora(11, 0).toString()));
        fijar(hora(11, 0));
        carta(cazador, mesa.partida()).then().statusCode(200)
                .body("noDisponible", nullValue());
        eliminar(cazador, mesa.partida(), codigoDe(victima, mesa.partida()))
                .then().statusCode(200)
                .body("aliasVictima", equalTo(victima.alias()));
    }

    private Instant hora(int hora, int minuto) {
        return martes.withHour(hora).withMinute(minuto).withSecond(0).toInstant();
    }

    private void fijar(Instant instante) {
        when(reloj.instant()).thenReturn(instante);
    }

    private Mesa crearPartidaEnJuego() {
        Persona master = registrar();
        Instant inicio = reloj.instant().plus(Duration.ofDays(1));
        Response partida = given().port(puerto).auth().oauth2(master.token())
                .contentType(ContentType.JSON).body(Map.of("nombre", "Partida Horarios PBI9",
                        "inicioEn", inicio.toString(),
                        "finEn", inicio.plus(Duration.ofDays(7)).toString(),
                        "tipoMisiones", "BIBLIOTECA", "plazoDisputaHoras", 6,
                        "entradasTardias", false, "masterJuega", false))
                .post("/api/v1/partidas").then().statusCode(201).extract().response();
        UUID partidaId = UUID.fromString(partida.path("id"));
        List<Persona> jugadores = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Persona jugador = registrar();
            if (i == 0) {
                given().port(puerto).auth().oauth2(jugador.token())
                        .contentType(ContentType.JSON)
                        .body(Map.of("franjas", CLASE_MARTES))
                        .put("/api/v1/cuentas/yo/horario").then().statusCode(200);
            }
            given().port(puerto).auth().oauth2(jugador.token())
                    .contentType(ContentType.JSON)
                    .body(Map.of("codigo", partida.<String>path("codigoInvitacion"),
                            "aceptaNormas", true))
                    .post("/api/v1/inscripciones").then().statusCode(201);
            jugadores.add(jugador);
        }
        given().port(puerto).auth().oauth2(master.token())
                .post("/api/v1/partidas/{id}/arranque", partidaId).then().statusCode(200);
        return new Mesa(partidaId, jugadores);
    }

    private Persona registrar() {
        String alias = "hs_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Response respuesta = given().port(puerto).contentType(ContentType.JSON)
                .body(Map.of("alias", alias, "email", alias + "@example.com",
                        "contrasena", "fantasma2026"))
                .post("/api/v1/cuentas").then().statusCode(201).extract().response();
        return new Persona(UUID.fromString(respuesta.path("cuenta.id")), alias,
                respuesta.path("tokenAcceso"));
    }

    private Persona objetivoDe(Mesa mesa, Persona cazador) {
        String alias = carta(cazador, mesa.partida()).path("aliasObjetivo");
        return mesa.jugadores().stream().filter(p -> p.alias().equals(alias))
                .findFirst().orElseThrow();
    }

    private Persona cazadorDe(Mesa mesa, Persona victima) {
        return mesa.jugadores().stream()
                .filter(p -> victima.alias().equals(
                        carta(p, mesa.partida()).<String>path("aliasObjetivo")))
                .findFirst().orElseThrow();
    }

    private UUID inscripcion(UUID partidaId, Persona jugador) {
        return jdbc.queryForObject("select id from inscripcion"
                + " where partida_id = ? and cuenta_id = ?", UUID.class,
                partidaId, jugador.id());
    }

    private int intentosFallidos(UUID inscripcionId) {
        return jdbc.queryForObject("select intentos_fallidos from jugador_partida"
                + " where inscripcion_id = ?", Integer.class, inscripcionId);
    }

    private String codigoDe(Persona jugador, UUID partidaId) {
        return given().port(puerto).auth().oauth2(jugador.token())
                .get(RUTA_CODIGO, partidaId).then().statusCode(200)
                .extract().path("codigo");
    }

    private List<String> codigoCorrectoEIncorrecto(Persona victima, UUID partidaId) {
        String correcto = codigoDe(victima, partidaId);
        return List.of(correcto, correcto.equals("123456") ? "654321" : "123456");
    }

    private Response carta(Persona jugador, UUID partidaId) {
        return given().port(puerto).auth().oauth2(jugador.token())
                .get(RUTA_CARTA, partidaId);
    }

    private Response eliminar(Persona jugador, UUID partidaId, String codigo) {
        return given().port(puerto).auth().oauth2(jugador.token())
                .contentType(ContentType.JSON).body(Map.of("codigo", codigo, "resultadoZona", "FUERA"))
                .post(RUTA_ELIMINAR, partidaId);
    }

    private record Persona(UUID id, String alias, String token) {
    }

    private record Mesa(UUID partida, List<Persona> jugadores) {

        Persona enClase() {
            return jugadores.get(0);
        }
    }
}
