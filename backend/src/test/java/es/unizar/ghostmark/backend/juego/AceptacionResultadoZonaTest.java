package es.unizar.ghostmark.backend.juego;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import es.unizar.ghostmark.backend.TestcontainersConfiguration;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DisplayName("PBI 10b - aceptacion del resultado de zona al eliminar")
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AceptacionResultadoZonaTest {

    private static final String RUTA_ELIMINAR = "/api/v1/partidas/{id}/eliminaciones";
    private static final String RUTA_CARTA = "/api/v1/partidas/{id}/yo/carta";
    private static final String RUTA_CODIGO = "/api/v1/partidas/{id}/yo/codigo";
    private static final Set<String> CAMPOS_EVENTO = Set.of("cazadorId", "victimaId",
        "misionId", "objetivoHeredadoId", "misionHeredadaId", "resultadoZona",
        "origen");

    @LocalServerPort
    int puerto;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoBean
    Clock reloj;

    @BeforeEach
    void fijarReloj() {
        when(reloj.instant()).thenReturn(Instant.now().truncatedTo(ChronoUnit.MINUTES));
        when(reloj.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @ParameterizedTest(name = "Criterio 1: {0} no bloquea la eliminacion")
    @ValueSource(strings = {"DENTRO", "FUERA", "NO_CONCLUYENTE"})
    void criterio1CadaResultadoSeGuardaSinCoordenadasYSeAcepta(String resultado) {
        Mesa mesa = crearPartidaEnJuego();
        Persona cazador = mesa.jugadores().get(0);
        Persona victima = objetivoDe(mesa, cazador);
        String codigo = codigoDe(victima, mesa.partida());

        Response respuesta = eliminar(cazador, mesa.partida(),
                Map.of("codigo", codigo, "resultadoZona", resultado));

        respuesta.then().statusCode(200)
                .body("aliasVictima", equalTo(victima.alias()))
                .body("carta.eliminaciones", equalTo(1));
        assertEquals(Set.of("aliasVictima", "carta"), respuesta.jsonPath().getMap("").keySet());
        assertEquals("ELIMINADO", carta(victima, mesa.partida()).path("estado"));

        String datos = jdbc.queryForObject("select datos::text from evento"
                + " where partida_id = ? and tipo = 'JUGADOR_ELIMINADO'",
                String.class, mesa.partida());
        Map<String, Object> evento = JsonPath.from(datos).getMap("");
        assertEquals(CAMPOS_EVENTO, evento.keySet());
        assertEquals("CODIGO", evento.get("origen"));
        assertEquals(resultado, evento.get("resultadoZona"));
        assertEquals(1, eventosEliminacion(mesa.partida()));
    }

    @Test
    void criterio1LaApiRechazaCoordenadasSinGuardarUnaEliminacion() {
        Mesa mesa = crearPartidaEnJuego();
        Persona cazador = mesa.jugadores().get(0);
        Persona victima = objetivoDe(mesa, cazador);
        UUID cazadorId = inscripcion(mesa.partida(), cazador);

        eliminar(cazador, mesa.partida(), Map.of("codigo", codigoDe(victima, mesa.partida()),
                "resultadoZona", "DENTRO", "latitud", 41.6835,
                "longitud", -0.8885, "precision", 8)).then().statusCode(400)
                .body("codigo", equalTo("DATOS_NO_VALIDOS"))
                .body("errores.campo", hasItems("latitud", "longitud", "precision"));

        assertEquals("VIVO", carta(victima, mesa.partida()).path("estado"));
        assertEquals(0, intentosFallidos(cazadorId));
        assertEquals(0, eventosEliminacion(mesa.partida()));
    }

    @Test
    void criterio1UnResultadoAusenteONoValidoSeRechazaSinGastarIntentos() {
        Mesa mesa = crearPartidaEnJuego();
        Persona cazador = mesa.jugadores().get(0);
        Persona victima = objetivoDe(mesa, cazador);
        UUID cazadorId = inscripcion(mesa.partida(), cazador);
        String codigo = codigoDe(victima, mesa.partida());

        for (Map<String, Object> cuerpo : List.of(Map.<String, Object>of("codigo", codigo),
                Map.<String, Object>of("codigo", codigo, "resultadoZona", "CERCA"))) {
            eliminar(cazador, mesa.partida(), cuerpo).then().statusCode(400)
                    .body("codigo", equalTo("DATOS_NO_VALIDOS"))
                    .body("errores.campo", hasItems("resultadoZona"));
        }
        assertEquals("VIVO", carta(victima, mesa.partida()).path("estado"));
        assertEquals(0, intentosFallidos(cazadorId));
        assertEquals(0, eventosEliminacion(mesa.partida()));
    }

    private Mesa crearPartidaEnJuego() {
        Persona master = registrar();
        Instant inicio = reloj.instant().plus(Duration.ofDays(1));
        Response partida = given().port(puerto).auth().oauth2(master.token())
                .contentType(ContentType.JSON).body(Map.of("nombre", "Partida zona PBI10b",
                        "inicioEn", inicio.toString(),
                        "finEn", inicio.plus(Duration.ofDays(7)).toString(),
                        "tipoMisiones", "BIBLIOTECA", "plazoDisputaHoras", 6,
                        "entradasTardias", false, "masterJuega", false))
                .post("/api/v1/partidas").then().statusCode(201).extract().response();
        UUID partidaId = UUID.fromString(partida.path("id"));
        List<Persona> jugadores = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Persona jugador = registrar();
            given().port(puerto).auth().oauth2(jugador.token())
                    .contentType(ContentType.JSON)
                    .body(Map.of("codigo", partida.<String>path("codigoInvitacion"),
                            "aceptaNormas", true))
                    .post("/api/v1/inscripciones").then().statusCode(201);
            jugadores.add(jugador);
        }
        given().port(puerto).auth().oauth2(master.token())
                .post("/api/v1/partidas/{id}/arranque", partidaId)
                .then().statusCode(200);
        return new Mesa(partidaId, jugadores);
    }

    private Persona registrar() {
        String alias = "zb_" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 16);
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

    private UUID inscripcion(UUID partidaId, Persona jugador) {
        return jdbc.queryForObject("select id from inscripcion"
                + " where partida_id = ? and cuenta_id = ?", UUID.class,
                partidaId, jugador.id());
    }

    private int intentosFallidos(UUID inscripcionId) {
        return jdbc.queryForObject("select intentos_fallidos from jugador_partida"
                + " where inscripcion_id = ?", Integer.class, inscripcionId);
    }

    private int eventosEliminacion(UUID partidaId) {
        return jdbc.queryForObject("select count(*) from evento"
                + " where partida_id = ? and tipo = 'JUGADOR_ELIMINADO'",
                Integer.class, partidaId);
    }

    private String codigoDe(Persona jugador, UUID partidaId) {
        return given().port(puerto).auth().oauth2(jugador.token())
                .get(RUTA_CODIGO, partidaId).then().statusCode(200)
                .extract().path("codigo");
    }

    private Response carta(Persona jugador, UUID partidaId) {
        return given().port(puerto).auth().oauth2(jugador.token())
                .get(RUTA_CARTA, partidaId);
    }

    private Response eliminar(Persona jugador, UUID partidaId, Map<String, Object> cuerpo) {
        return given().port(puerto).auth().oauth2(jugador.token())
                .contentType(ContentType.JSON).body(cuerpo)
                .post(RUTA_ELIMINAR, partidaId);
    }

    private record Persona(UUID id, String alias, String token) {
    }

    private record Mesa(UUID partida, List<Persona> jugadores) {
    }
}
