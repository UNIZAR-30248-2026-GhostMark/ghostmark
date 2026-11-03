package es.unizar.ghostmark.backend.juego;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import es.unizar.ghostmark.backend.TestcontainersConfiguration;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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

@DisplayName("PBI 7a - aceptación del código de vida y la eliminación")
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EliminacionAceptacionTest {

    private static final String RUTA_CODIGO = "/api/v1/partidas/{id}/yo/codigo";
    private static final String RUTA_CARTA = "/api/v1/partidas/{id}/yo/carta";
    private static final String RUTA_FEED = "/api/v1/partidas/{id}/feed";
    private static final String RUTA_ELIMINAR = "/api/v1/partidas/{id}/eliminaciones";

    @LocalServerPort
    int puerto;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoBean
    Clock reloj;

    @BeforeEach
    void fijarReloj() {
        when(reloj.instant()).thenReturn(
                Instant.now().truncatedTo(ChronoUnit.MINUTES).plusSeconds(30));
        when(reloj.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void criterio1CadaVivoObtieneSoloSuCodigoYEsteRotaCada60Segundos(boolean masterJuega) {
        Mesa mesa = preparar(3, masterJuega);
        for (Persona jugador : mesa.jugadores()) {
            Response respuesta = codigo(jugador, mesa.partida()).then().statusCode(200)
                    .extract().response();
            String valor = respuesta.path("codigo");
            assertTrue(valor.matches("[0-9]{6}"));
            assertEquals(Set.of("codigo", "caducaEn"), respuesta.jsonPath().getMap("").keySet());
            Instant caduca = Instant.parse(respuesta.path("caducaEn"));
            assertEquals(reloj.instant().truncatedTo(ChronoUnit.MINUTES).plusSeconds(60), caduca);
        }
        Persona ajeno = registrar();
        codigo(ajeno, mesa.partida()).then().statusCode(404)
                .body("codigo", equalTo("NO_ENCONTRADA"));
        if (!masterJuega) {
            codigo(mesa.master(), mesa.partida()).then().statusCode(404)
                    .body("codigo", equalTo("NO_ENCONTRADA"));
        }
        given().port(puerto).get(RUTA_CODIGO, mesa.partida()).then().statusCode(401)
                .body("codigo", equalTo("NO_AUTENTICADO"));

        Persona jugador = mesa.jugadores().get(0);
        Persona otro = mesa.jugadores().get(1);
        List<Map<String, Object>> inscritos = given().port(puerto)
                .auth().oauth2(mesa.master().token())
                .get("/api/v1/partidas/{id}/inscritos", mesa.partida()).then().statusCode(200)
                .extract().jsonPath().getList("");
        String otroId = inscritos.stream().filter(fila -> otro.alias().equals(fila.get("alias")))
                .findFirst().orElseThrow().get("id").toString();

        // Único dato fijado en BD: secreto del vector conocido, para evitar colisiones aleatorias
        assertEquals(1, jdbc.update("update jugador_partida set secreto_codigo = ?"
                        + " where inscripcion_id = (select id from inscripcion"
                        + " where partida_id = ? and cuenta_id = ?)",
                "12345678901234567890".getBytes(StandardCharsets.US_ASCII),
                mesa.partida(), jugador.id()));
        when(reloj.instant()).thenReturn(Instant.ofEpochSecond(90));
        codigo(jugador, mesa.partida()).then().statusCode(200)
                .body("codigo", equalTo("287082"));
        given().port(puerto).auth().oauth2(jugador.token()).queryParam("inscripcionId", otroId)
                .queryParam("cuentaId", otro.id()).get(RUTA_CODIGO, mesa.partida())
                .then().statusCode(200).body("codigo", equalTo("287082"));
        given().port(puerto).auth().oauth2(jugador.token())
                .get("/api/v1/partidas/{id}/inscritos/{otro}/codigo", mesa.partida(), otroId)
                .then().statusCode(404);
        when(reloj.instant()).thenReturn(Instant.ofEpochSecond(120).minusNanos(1));
        Response antes = codigo(jugador, mesa.partida()).then().statusCode(200)
                .body("codigo", equalTo("287082")).extract().response();
        assertEquals(Instant.ofEpochSecond(120), Instant.parse(antes.path("caducaEn")));
        when(reloj.instant()).thenReturn(Instant.ofEpochSecond(120));
        Response despues = codigo(jugador, mesa.partida()).then().statusCode(200)
                .body("codigo", equalTo("359152")).extract().response();
        assertEquals(Instant.ofEpochSecond(180), Instant.parse(despues.path("caducaEn")));
    }

    @Test
    void criterio2ElCodigoDelObjetivoEliminaSumaYPublicaUnFeedSinCazador() {
        Mesa mesa = preparar(3, false);
        Persona cazador = mesa.jugadores().get(0);
        Persona victima = persona(mesa,
                (String) carta(cazador, mesa.partida()).get("aliasObjetivo"));
        String valido = codigo(victima, mesa.partida()).then().statusCode(200)
                .extract().path("codigo");

        Response resultado = eliminar(cazador, mesa.partida(), valido).then().statusCode(200)
                .body("aliasVictima", equalTo(victima.alias()))
                .body("carta.estado", equalTo("VIVO"))
                .body("carta.eliminaciones", equalTo(1)).extract().response();
        assertEquals(carta(cazador, mesa.partida()), resultado.jsonPath().getMap("carta"));
        Map<String, Object> eliminada = carta(victima, mesa.partida());
        assertEquals("ELIMINADO", eliminada.get("estado"));
        assertNull(eliminada.get("aliasObjetivo"));
        assertNull(eliminada.get("mision"));
        codigo(victima, mesa.partida()).then().statusCode(409)
                .body("codigo", equalTo("JUGADOR_ELIMINADO"));
        eliminar(victima, mesa.partida(), valido).then().statusCode(409)
                .body("codigo", equalTo("JUGADOR_ELIMINADO"));

        List<Persona> lectores = new ArrayList<>(mesa.jugadores());
        lectores.add(mesa.master());
        for (Persona lector : lectores) {
            List<Map<String, Object>> feed = feed(lector, mesa.partida());
            assertEquals(1, feed.size());
            Map<String, Object> entrada = feed.get(0);
            assertEquals(Set.of("tipo", "alias", "ocurridoEn"), entrada.keySet());
            assertEquals("ELIMINACION", entrada.get("tipo"));
            assertEquals(victima.alias(), entrada.get("alias"));
            assertEquals(reloj.instant(), Instant.parse((String) entrada.get("ocurridoEn")));
        }
        Persona ajeno = registrar();
        given().port(puerto).auth().oauth2(ajeno.token()).get(RUTA_FEED, mesa.partida())
                .then().statusCode(404).body("codigo", equalTo("NO_ENCONTRADA"));
        given().port(puerto).auth().oauth2(mesa.master().token())
                .contentType(ContentType.JSON).body(Map.of("codigo", valido))
                .post(RUTA_ELIMINAR, mesa.partida()).then().statusCode(404)
                .body("codigo", equalTo("NO_ENCONTRADA"));
    }

    @Test
    void criterio3HeredaObjetivoYMisionYConservaUnSoloCicloHastaDosVivos() {
        Mesa mesa = preparar(4, true);
        List<Persona> vivos = new ArrayList<>(mesa.jugadores());
        Persona cazador = vivos.get(0);
        List<String> eliminados = new ArrayList<>();
        comprobarCiclo(mesa.partida(), vivos);
        while (vivos.size() > 2) {
            Map<String, Object> antes = carta(cazador, mesa.partida());
            Persona victima = persona(mesa, (String) antes.get("aliasObjetivo"));
            Map<String, Object> heredada = carta(victima, mesa.partida());
            String valido = codigo(victima, mesa.partida()).then().statusCode(200)
                    .extract().path("codigo");
            Response resultado = eliminar(cazador, mesa.partida(), valido).then().statusCode(200)
                    .extract().response();
            Map<String, Object> despues = carta(cazador, mesa.partida());
            assertEquals(heredada.get("aliasObjetivo"), despues.get("aliasObjetivo"));
            assertEquals(heredada.get("mision"), despues.get("mision"));
            assertEquals(((Number) antes.get("eliminaciones")).intValue() + 1,
                    despues.get("eliminaciones"));
            assertEquals(despues, resultado.jsonPath().getMap("carta"));
            given().port(puerto).auth().oauth2(victima.token())
                    .get(RUTA_CARTA, mesa.partida()).then().statusCode(200)
                    .body("estado", equalTo("ELIMINADO"))
                    .body("aliasObjetivo", nullValue()).body("mision", nullValue());
            vivos.remove(victima);
            eliminados.add(0, victima.alias());
            comprobarCiclo(mesa.partida(), vivos);
        }
        assertEquals(vivos.get(1).alias(),
                carta(vivos.get(0), mesa.partida()).get("aliasObjetivo"));
        assertEquals(vivos.get(0).alias(),
                carta(vivos.get(1), mesa.partida()).get("aliasObjetivo"));
        assertEquals(eliminados, feed(cazador, mesa.partida()).stream()
                .map(entrada -> entrada.get("alias")).toList());
        // La última eliminación y el cierre pertenecen a la PBI 8
    }

    private void comprobarCiclo(UUID partidaId, List<Persona> vivos) {
        Set<String> esperados = vivos.stream().map(Persona::alias).collect(Collectors.toSet());
        Map<String, String> objetivos = new HashMap<>();
        for (Persona vivo : vivos) {
            Map<String, Object> carta = carta(vivo, partidaId);
            assertEquals("VIVO", carta.get("estado"));
            String objetivo = (String) carta.get("aliasObjetivo");
            assertTrue(esperados.contains(objetivo), "El objetivo debe seguir vivo");
            assertNotNull(carta.get("mision"));
            objetivos.put(vivo.alias(), objetivo);
        }
        assertEquals(esperados, new HashSet<>(objetivos.values()));
        String inicio = vivos.get(0).alias();
        String actual = inicio;
        Set<String> visitados = new HashSet<>();
        while (visitados.add(actual)) {
            actual = objetivos.get(actual);
            assertNotNull(actual);
        }
        assertEquals(inicio, actual);
        assertEquals(esperados, visitados, "Todos los vivos deben formar un único ciclo");
    }

    private Mesa preparar(int numero, boolean masterJuega) {
        Persona master = registrar();
        Instant inicio = reloj.instant().plus(Duration.ofDays(1));
        Response respuesta = given().port(puerto).auth().oauth2(master.token())
                .contentType(ContentType.JSON).body(Map.of("nombre", "Aceptacion PBI 7a",
                        "inicioEn", inicio.toString(), "finEn", inicio.plus(Duration.ofDays(7))
                                .toString(), "tipoMisiones", "BIBLIOTECA", "plazoDisputaHoras", 6,
                        "entradasTardias", false, "masterJuega", masterJuega))
                .post("/api/v1/partidas").then().statusCode(201).extract().response();
        UUID partida = UUID.fromString(respuesta.path("id"));
        List<Persona> jugadores = new ArrayList<>();
        if (masterJuega) {
            jugadores.add(master);
        }
        while (jugadores.size() < numero) {
            Persona jugador = registrar();
            given().port(puerto).auth().oauth2(jugador.token()).contentType(ContentType.JSON)
                    .body(Map.of("codigo", respuesta.<String>path("codigoInvitacion"),
                            "aceptaNormas", true))
                    .post("/api/v1/inscripciones").then().statusCode(201);
            jugadores.add(jugador);
        }
        given().port(puerto).auth().oauth2(master.token())
                .post("/api/v1/partidas/{id}/arranque", partida).then().statusCode(200);
        return new Mesa(master, partida, jugadores);
    }

    private Persona registrar() {
        String alias = "a7_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Response respuesta = given().port(puerto).contentType(ContentType.JSON)
                .body(Map.of("alias", alias, "email", alias + "@example.com",
                        "contrasena", "fantasma2026"))
                .post("/api/v1/cuentas").then().statusCode(201).extract().response();
        return new Persona(UUID.fromString(respuesta.path("cuenta.id")), alias,
                respuesta.path("tokenAcceso"));
    }

    private Persona persona(Mesa mesa, String alias) {
        return mesa.jugadores().stream().filter(jugador -> jugador.alias().equals(alias))
                .findFirst().orElseThrow();
    }

    private Response codigo(Persona persona, UUID partidaId) {
        return given().port(puerto).auth().oauth2(persona.token()).get(RUTA_CODIGO, partidaId);
    }

    private Map<String, Object> carta(Persona persona, UUID partidaId) {
        return given().port(puerto).auth().oauth2(persona.token()).get(RUTA_CARTA, partidaId)
                .then().statusCode(200).extract().jsonPath().getMap("");
    }

    private Response eliminar(Persona persona, UUID partidaId, String codigo) {
        return given().port(puerto).auth().oauth2(persona.token()).contentType(ContentType.JSON)
                .body(Map.of("codigo", codigo)).post(RUTA_ELIMINAR, partidaId);
    }

    private List<Map<String, Object>> feed(Persona persona, UUID partidaId) {
        return given().port(puerto).auth().oauth2(persona.token()).get(RUTA_FEED, partidaId)
                .then().statusCode(200).extract().jsonPath().getList("");
    }

    private record Persona(UUID id, String alias, String token) {
    }

    private record Mesa(Persona master, UUID partida, List<Persona> jugadores) {
    }
}
