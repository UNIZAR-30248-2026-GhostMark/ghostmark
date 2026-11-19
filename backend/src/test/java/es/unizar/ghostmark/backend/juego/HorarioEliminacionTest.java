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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HorarioEliminacionTest {

    private static final String RUTA_ELIMINAR = "/api/v1/partidas/{id}/eliminaciones";
    private static final String RUTA_CARTA = "/api/v1/partidas/{id}/yo/carta";
    private static final List<Map<String, Object>> CLASE_MARTES = List.of(
            Map.of("dia", 2, "horaInicio", "10:00"), Map.of("dia", 2, "horaInicio", "10:30"));

    @LocalServerPort
    int puerto;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoBean
    Clock reloj;

    private final GeneradorCodigoVida generador = new GeneradorCodigoVida();
    private ZonedDateTime martes;

    @BeforeEach
    void fijarReloj() {
        martes = ZonedDateTime.now(HorarioLectivo.ZONA)
                .with(TemporalAdjusters.next(DayOfWeek.TUESDAY))
                .withHour(10).withMinute(10).withSecond(30).withNano(0);
        when(reloj.instant()).thenReturn(martes.toInstant());
        when(reloj.getZone()).thenReturn(ZoneOffset.UTC);
    }

    private Instant hora(int hora, int minuto) {
        return martes.withHour(hora).withMinute(minuto).withSecond(0).toInstant();
    }

    @Test
    void conElObjetivoEnClaseNoSeEliminaNiSeGastanIntentos() {
        Mesa mesa = preparar(3);
        UUID enClase = inscripcion(mesa.enClase(), mesa.partida());
        UUID cazadorId = cazadorDe(mesa.partida(), enClase);
        Persona cazador = persona(mesa, cazadorId);
        String correcto = codigoDe(mesa.partida(), enClase, reloj.instant());

        for (String codigo : List.of(correcto, correcto.equals("123456") ? "654321" : "123456")) {
            eliminar(cazador, mesa.partida(), codigo).then().statusCode(409)
                    .body("codigo", equalTo("HORARIO_LECTIVO"))
                    .body("motivo", equalTo("OBJETIVO"))
                    .body("disponibleEn", equalTo(hora(11, 0).toString()));
        }
        assertEquals(0, jdbc.queryForObject("select intentos_fallidos from jugador_partida"
                + " where inscripcion_id = ?", Integer.class, cazadorId));
        assertEquals("VIVO", estado(enClase));
        carta(cazador, mesa.partida()).then().statusCode(200)
                .body("noDisponible.motivo", equalTo("OBJETIVO"))
                .body("noDisponible.disponibleEn", equalTo(hora(11, 0).toString()));

        when(reloj.instant()).thenReturn(hora(11, 0));
        carta(cazador, mesa.partida()).then().statusCode(200)
                .body("noDisponible", nullValue());
        eliminar(cazador, mesa.partida(), codigoDe(mesa.partida(), enClase, hora(11, 0)))
                .then().statusCode(200);
        assertEquals("ELIMINADO", estado(enClase));
    }

    @Test
    void conElCazadorEnClaseElMotivoEsElCazador() {
        Mesa mesa = preparar(3);
        UUID cazadorId = inscripcion(mesa.enClase(), mesa.partida());
        UUID objetivo = jdbc.queryForObject("select objetivo_id from jugador_partida"
                + " where inscripcion_id = ?", UUID.class, cazadorId);

        eliminar(mesa.enClase(), mesa.partida(),
                codigoDe(mesa.partida(), objetivo, reloj.instant()))
                .then().statusCode(409)
                .body("codigo", equalTo("HORARIO_LECTIVO"))
                .body("motivo", equalTo("CAZADOR"))
                .body("disponibleEn", equalTo(hora(11, 0).toString()));
        carta(mesa.enClase(), mesa.partida()).then().statusCode(200)
                .body("noDisponible.motivo", equalTo("CAZADOR"));
        assertEquals("VIVO", estado(objetivo));
    }

    @Test
    void laCartaDeLaEliminacionAvisaSiElObjetivoHeredadoEstaEnClase() {
        Mesa mesa = preparar(4);
        UUID enClase = inscripcion(mesa.enClase(), mesa.partida());
        UUID victima = cazadorDe(mesa.partida(), enClase);
        Persona cazador = persona(mesa, cazadorDe(mesa.partida(), victima));

        eliminar(cazador, mesa.partida(), codigoDe(mesa.partida(), victima, reloj.instant()))
                .then().statusCode(200)
                .body("carta.aliasObjetivo", equalTo(mesa.enClase().alias()))
                .body("carta.noDisponible.motivo", equalTo("OBJETIVO"))
                .body("carta.noDisponible.disponibleEn", equalTo(hora(11, 0).toString()));
    }

    private Mesa preparar(int jugadores) {
        Persona master = registrar();
        Instant inicio = reloj.instant().plus(Duration.ofDays(1));
        Response partida = given().port(puerto).auth().oauth2(master.token())
                .contentType(ContentType.JSON).body(Map.of("nombre", "Partida Horario",
                        "inicioEn", inicio.toString(), "finEn", inicio.plus(Duration.ofDays(7))
                                .toString(), "tipoMisiones", "BIBLIOTECA", "plazoDisputaHoras", 6,
                        "entradasTardias", false, "masterJuega", false))
                .post("/api/v1/partidas").then().statusCode(201).extract().response();
        UUID partidaId = UUID.fromString(partida.path("id"));
        List<Persona> inscritos = new ArrayList<>();
        for (int i = 0; i < jugadores; i++) {
            Persona jugador = registrar();
            if (i == 0) {
                given().port(puerto).auth().oauth2(jugador.token()).contentType(ContentType.JSON)
                        .body(Map.of("franjas", CLASE_MARTES)).put("/api/v1/cuentas/yo/horario")
                        .then().statusCode(200);
            }
            given().port(puerto).auth().oauth2(jugador.token()).contentType(ContentType.JSON)
                    .body(Map.of("codigo", partida.<String>path("codigoInvitacion"),
                            "aceptaNormas", true))
                    .post("/api/v1/inscripciones").then().statusCode(201);
            inscritos.add(jugador);
        }
        given().port(puerto).auth().oauth2(master.token())
                .post("/api/v1/partidas/{id}/arranque", partidaId).then().statusCode(200);
        return new Mesa(partidaId, inscritos);
    }

    private Persona registrar() {
        String alias = "h_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Response respuesta = given().port(puerto).contentType(ContentType.JSON)
                .body(Map.of("alias", alias, "email", alias + "@example.com",
                        "contrasena", "fantasma2026"))
                .post("/api/v1/cuentas").then().statusCode(201).extract().response();
        return new Persona(UUID.fromString(respuesta.path("cuenta.id")), alias,
                respuesta.path("tokenAcceso"));
    }

    private UUID inscripcion(Persona persona, UUID partidaId) {
        return jdbc.queryForObject("select id from inscripcion where partida_id = ?"
                + " and cuenta_id = ?", UUID.class, partidaId, persona.id());
    }

    private UUID cazadorDe(UUID partidaId, UUID objetivo) {
        return jdbc.queryForObject("select inscripcion_id from jugador_partida"
                + " where partida_id = ? and objetivo_id = ?", UUID.class, partidaId, objetivo);
    }

    private Persona persona(Mesa mesa, UUID inscripcionId) {
        return mesa.jugadores().stream()
                .filter(jugador -> inscripcionId.equals(inscripcion(jugador, mesa.partida())))
                .findFirst().orElseThrow();
    }

    private String estado(UUID inscripcionId) {
        return jdbc.queryForObject("select estado from jugador_partida where inscripcion_id = ?",
                String.class, inscripcionId);
    }

    private String codigoDe(UUID partidaId, UUID inscripcionId, Instant cuando) {
        byte[] secreto = jdbc.queryForObject("select secreto_codigo from jugador_partida"
                + " where partida_id = ? and inscripcion_id = ?", byte[].class,
                partidaId, inscripcionId);
        return generador.generar(secreto, cuando).codigo();
    }

    private Response eliminar(Persona persona, UUID partidaId, String codigo) {
        return given().port(puerto).auth().oauth2(persona.token()).contentType(ContentType.JSON)
                .body(Map.of("codigo", codigo, "resultadoZona", "FUERA"))
                .post(RUTA_ELIMINAR, partidaId);
    }

    private Response carta(Persona persona, UUID partidaId) {
        return given().port(puerto).auth().oauth2(persona.token()).get(RUTA_CARTA, partidaId);
    }

    private record Persona(UUID id, String alias, String token) {
    }

    private record Mesa(UUID partida, List<Persona> jugadores) {

        Persona enClase() {
            return jugadores.get(0);
        }
    }
}
