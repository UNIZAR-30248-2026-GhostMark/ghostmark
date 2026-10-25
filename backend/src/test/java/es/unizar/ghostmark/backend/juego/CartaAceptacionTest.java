package es.unizar.ghostmark.backend.juego;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import es.unizar.ghostmark.backend.TestcontainersConfiguration;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Aceptación de la PBI 5 (#31): los dos criterios y la privacidad por rol (#70).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CartaAceptacionTest {

    private static final String RUTA_CARTA = "/api/v1/partidas/{id}/yo/carta";

    // Textos de la biblioteca curada de la 5.1, revisados según su migración.
    // Una misión nueva o modificada debe revisarse antes de incorporarla a esta lista.
    private static final Set<String> MISIONES_REVISADAS = Set.of(
            "Que te preste un bolígrafo",
            "Que diga la palabra «compilador»",
            "Que te diga la hora",
            "Que se haga un selfie contigo",
            "Que te diga cuál es su comida favorita",
            "Que te diga cuál es su asignatura favorita",
            "Que te recomiende una canción",
            "Que te recomiende una serie o una película",
            "Que diga la palabra «algoritmo»",
            "Que te pregunte a ti qué hora es",
            "Que te diga cuál es su número de la suerte",
            "Que te haga un dibujo en un papel",
            "Que te diga cuál es el último libro que ha leído",
            "Que diga la palabra «fantasma»",
            "Que te recomiende un sitio para comer cerca del campus",
            "Que te cuente un chiste",
            "Que te diga qué emoji usa más",
            "Que te diga qué tiempo va a hacer mañana",
            "Que te diga cuál es su videojuego favorito",
            "Que te preste cualquier objeto que no sea su móvil",
            "Que diga una palabra que empiece por «z»",
            "Que te ofrezca un caramelo o un chicle",
            "Que te diga en qué ciudad nació",
            "Que te enseñe a decir una palabra en otro idioma");

    @LocalServerPort
    int puerto;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void criterio1CadaJugadorVeSoloSuObjetivoYSuMisionIncluidoElMaster() {
        Mesa mesa = preparar(true);
        arrancar(mesa);
        // Los eventos sirven de referencia interna; no se obtienen con el token del jugador.
        List<UUID> ciclo = jdbc.queryForList("select jugador.id::uuid from evento e,"
                + " jsonb_array_elements_text(e.datos->'orden') with ordinality as jugador(id, pos)"
                + " where e.partida_id = ? and e.tipo = 'CICLO_FORMADO' order by jugador.pos",
                UUID.class, mesa.id());
        assertEquals(mesa.jugadores().size(), ciclo.size());
        assertEquals(new HashSet<>(mesa.jugadores().stream()
                .map(jugador -> inscripcion(mesa, jugador)).toList()), new HashSet<>(ciclo));

        for (Persona jugador : mesa.jugadores()) {
            UUID propia = inscripcion(mesa, jugador);
            UUID objetivo = ciclo.get((ciclo.indexOf(propia) + 1) % ciclo.size());
            String alias = jdbc.queryForObject("select c.alias from inscripcion i"
                    + " join cuenta c on c.id = i.cuenta_id where i.id = ?",
                    String.class, objetivo);
            String mision = jdbc.queryForObject("select m.texto from evento e,"
                    + " jsonb_array_elements(e.datos->'asignaciones') as a(valor), mision m"
                    + " where e.partida_id = ? and e.tipo = 'MISIONES_REPARTIDAS'"
                    + " and m.id = (a.valor->>'misionId')::uuid"
                    + " and a.valor->>'inscripcionId' = ?",
                    String.class, mesa.id(), propia.toString());
            Map<String, Object> respuesta = carta(jugador, mesa).then().statusCode(200)
                    .body("aliasObjetivo", equalTo(alias)).body("mision", equalTo(mision))
                    .extract().jsonPath().getMap("");
            assertEquals(Set.of("aliasObjetivo", "mision"), respuesta.keySet());
        }
    }

    @Test
    void criterio2AlArrancarTodosRecibenUnaMisionDeLaBibliotecaCuradaSegura() {
        Set<String> biblioteca = new HashSet<>(jdbc.queryForList(
                "select texto from mision where activa", String.class));
        assertFalse(biblioteca.isEmpty());
        // Se fija el contenido revisado: buscar palabras prohibidas no verifica su significado.
        assertTrue(MISIONES_REVISADAS.containsAll(jdbc.queryForList(
                "select texto from mision", String.class)),
                "Hay textos sin revisar: comprobar contacto físico y objetos que simulen armas");

        Mesa mesa = preparar(true);
        for (Persona jugador : mesa.jugadores()) {
            carta(jugador, mesa).then().statusCode(409)
                    .body("codigo", equalTo("PARTIDA_NO_EN_JUEGO"));
        }
        arrancar(mesa);
        for (Persona jugador : mesa.jugadores()) {
            String mision = carta(jugador, mesa).then().statusCode(200)
                    .extract().path("mision");
            assertTrue(biblioteca.contains(mision),
                    "La misión debe proceder de la biblioteca activa");
            assertTrue(MISIONES_REVISADAS.contains(mision));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"JUGADOR", "MASTER_JUGADOR", "MASTER_NO_JUGADOR", "NO_INSCRITO"})
    void ningunRolObtieneElObjetivoNiLaMisionDeOtro(String rol) {
        Mesa mesa = preparar(!"MASTER_NO_JUGADOR".equals(rol));
        arrancar(mesa);
        Persona solicitante = switch (rol) {
            case "JUGADOR" -> mesa.jugadores().get(1);
            case "MASTER_JUGADOR", "MASTER_NO_JUGADOR" -> mesa.master();
            default -> registrar();
        };
        boolean juega = mesa.jugadores().contains(solicitante);
        Map<String, Object> propia = juega
                ? carta(solicitante, mesa).then().statusCode(200).extract().jsonPath().getMap("")
                : Map.of();
        if (!juega) {
            carta(solicitante, mesa).then().statusCode(404)
                    .body("codigo", equalTo("NO_ENCONTRADA"));
        }
        for (Persona otro : mesa.jugadores()) {
            if (otro.equals(solicitante)) {
                continue;
            }
            // La identidad procede del token, aunque se envíen identificadores ajenos.
            Response intento = given().port(puerto).auth().oauth2(solicitante.token())
                    .queryParam("cuentaId", otro.id())
                    .queryParam("inscripcionId", inscripcion(mesa, otro))
                    .get(RUTA_CARTA, mesa.id());
            if (juega) {
                assertEquals(propia, intento.then().statusCode(200)
                        .extract().jsonPath().getMap(""));
                Map<String, Object> ajena = carta(otro, mesa).then().statusCode(200)
                        .extract().jsonPath().getMap("");
                assertNotEquals(ajena.get("aliasObjetivo"), propia.get("aliasObjetivo"));
                assertNotEquals(ajena.get("mision"), propia.get("mision"));
            } else {
                intento.then().statusCode(404).body("codigo", equalTo("NO_ENCONTRADA"));
            }
            given().port(puerto).auth().oauth2(solicitante.token())
                    .get("/api/v1/partidas/{id}/inscritos/{otro}/carta",
                            mesa.id(), inscripcion(mesa, otro))
                    .then().statusCode(404);
        }
        Response inscritos = given().port(puerto).auth().oauth2(solicitante.token())
                .get("/api/v1/partidas/{id}/inscritos", mesa.id());
        if ("NO_INSCRITO".equals(rol)) {
            inscritos.then().statusCode(404);
        } else {
            List<Map<String, Object>> lista = inscritos.then().statusCode(200)
                    .extract().jsonPath().getList("");
            assertEquals(mesa.jugadores().size(), lista.size());
            for (Map<String, Object> inscrito : lista) {
                assertEquals(Set.of("id", "alias", "esMaster"), inscrito.keySet());
            }
        }
    }

    private Mesa preparar(boolean masterJuega) {
        Persona master = registrar();
        Instant inicio = Instant.now().plus(Duration.ofDays(1));
        Response respuesta = given().port(puerto).auth().oauth2(master.token())
                .contentType(ContentType.JSON).body(Map.of("nombre", "Aceptación PBI 5",
                        "inicioEn", inicio.toString(), "finEn", inicio.plus(Duration.ofDays(7))
                                .toString(), "tipoMisiones", "BIBLIOTECA", "plazoDisputaHoras", 6,
                        "entradasTardias", false, "masterJuega", masterJuega))
                .post("/api/v1/partidas").then().statusCode(201).extract().response();
        UUID id = UUID.fromString(respuesta.path("id"));
        String codigo = respuesta.path("codigoInvitacion");
        List<Persona> jugadores = new ArrayList<>();
        if (masterJuega) {
            jugadores.add(master);
        }
        while (jugadores.size() < 4) {
            Persona jugador = registrar();
            given().port(puerto).auth().oauth2(jugador.token()).contentType(ContentType.JSON)
                    .body(Map.of("codigo", codigo, "aceptaNormas", true))
                    .post("/api/v1/inscripciones").then().statusCode(201);
            jugadores.add(jugador);
        }
        return new Mesa(id, master, jugadores);
    }

    private Persona registrar() {
        String alias = "a5_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Response respuesta = given().port(puerto).contentType(ContentType.JSON)
                .body(Map.of("alias", alias, "email", alias + "@example.com",
                        "contrasena", "fantasma2026"))
                .post("/api/v1/cuentas").then().statusCode(201).extract().response();
        return new Persona(UUID.fromString(respuesta.path("cuenta.id")),
                respuesta.path("tokenAcceso"));
    }

    private void arrancar(Mesa mesa) {
        given().port(puerto).auth().oauth2(mesa.master().token())
                .post("/api/v1/partidas/{id}/arranque", mesa.id()).then().statusCode(200);
    }

    private Response carta(Persona persona, Mesa mesa) {
        return given().port(puerto).auth().oauth2(persona.token()).get(RUTA_CARTA, mesa.id());
    }

    private UUID inscripcion(Mesa mesa, Persona persona) {
        return jdbc.queryForObject("select id from inscripcion where partida_id = ?"
                + " and cuenta_id = ?", UUID.class, mesa.id(), persona.id());
    }

    private record Persona(UUID id, String token) {
    }

    private record Mesa(UUID id, Persona master, List<Persona> jugadores) {
    }
}
