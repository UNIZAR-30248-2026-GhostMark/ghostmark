package es.unizar.ghostmark.backend.juego;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import es.unizar.ghostmark.backend.TestcontainersConfiguration;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FeedTest {

    private static final String RUTA_FEED = "/api/v1/partidas/{id}/feed";
    private static final String RUTA_ELIMINAR = "/api/v1/partidas/{id}/eliminaciones";

    @LocalServerPort
    int puerto;

    @Autowired
    JdbcTemplate jdbc;

    private final GeneradorCodigoVida generador = new GeneradorCodigoVida();

    @Test
    void listaLasEliminacionesSinDecirQuienLasHizo() {
        Mesa mesa = preparar(4, true);
        arrancar(mesa).then().statusCode(200);
        Persona cazador = mesa.jugadores().get(0);
        UUID cazadorId = inscripcion(cazador, mesa.juego());

        UUID primera = objetivo(mesa.juego(), cazadorId);
        eliminar(cazador, mesa.juego(), primera);
        UUID segunda = objetivo(mesa.juego(), cazadorId);
        eliminar(cazador, mesa.juego(), segunda);

        List<Map<String, Object>> feed = feed(cazador, mesa.juego()).then().statusCode(200)
                .extract().jsonPath().getList("");
        assertEquals(2, feed.size());
        assertEquals(alias(segunda), feed.get(0).get("alias"));
        assertEquals(alias(primera), feed.get(1).get("alias"));
        assertEquals(Set.of("tipo", "alias", "ocurridoEn"), feed.get(0).keySet());
        assertEquals("ELIMINACION", feed.get(0).get("tipo"));
    }

    @Test
    void loVenElEliminadoYElMasterQueNoJuega() {
        Mesa mesa = preparar(3, false);
        arrancar(mesa).then().statusCode(200);
        Persona cazador = mesa.jugadores().get(0);
        UUID victimaId = objetivo(mesa.juego(), inscripcion(cazador, mesa.juego()));
        eliminar(cazador, mesa.juego(), victimaId);
        Persona eliminado = mesa.jugadores().stream()
                .filter(persona -> victimaId.equals(inscripcion(persona, mesa.juego())))
                .findFirst().orElseThrow();

        feed(eliminado, mesa.juego()).then().statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].alias", equalTo(alias(victimaId)));
        feed(mesa.master(), mesa.juego()).then().statusCode(200).body("size()", equalTo(1));
    }

    @Test
    void elAjenoRecibe404YFueraDeJuego409() {
        Mesa mesa = preparar(3, true);
        Persona ajeno = registrar();
        feed(ajeno, mesa.juego()).then().statusCode(404).body("codigo", equalTo("NO_ENCONTRADA"));
        feed(mesa.jugadores().get(0), new Juego(UUID.randomUUID(), ""))
                .then().statusCode(404).body("codigo", equalTo("NO_ENCONTRADA"));

        jdbc.update("update partida set estado = 'EN_INSCRIPCION' where id = ?",
                mesa.juego().id());
        feed(mesa.jugadores().get(0), mesa.juego()).then().statusCode(409)
                .body("codigo", equalTo("PARTIDA_NO_EN_JUEGO"));
        feed(ajeno, mesa.juego()).then().statusCode(404);
    }

    @Test
    void elAliasEsNuloSiLaVictimaBorroSuCuenta() {
        Mesa mesa = preparar(3, true);
        arrancar(mesa).then().statusCode(200);
        Persona cazador = mesa.jugadores().get(0);
        UUID victimaId = objetivo(mesa.juego(), inscripcion(cazador, mesa.juego()));
        eliminar(cazador, mesa.juego(), victimaId);
        Persona eliminado = mesa.jugadores().stream()
                .filter(persona -> victimaId.equals(inscripcion(persona, mesa.juego())))
                .findFirst().orElseThrow();

        given().port(puerto).auth().oauth2(eliminado.token()).delete("/api/v1/cuentas/yo")
                .then().statusCode(204);
        feed(cazador, mesa.juego()).then().statusCode(200).body("[0].alias", nullValue());
    }

    @Test
    void sinAutenticacionNoHayFeed() {
        Mesa mesa = preparar(3, true);
        given().port(puerto).get(RUTA_FEED, mesa.juego().id()).then().statusCode(401)
                .body("codigo", equalTo("NO_AUTENTICADO"));
    }

    private Mesa preparar(int jugadores, boolean masterJuega) {
        Persona master = registrar();
        Instant inicio = Instant.now().plus(Duration.ofDays(1));
        Response respuesta = given().port(puerto).auth().oauth2(master.token())
                .contentType(ContentType.JSON).body(Map.of("nombre", "Partida Feed",
                        "inicioEn", inicio.toString(), "finEn", inicio.plus(Duration.ofDays(7))
                                .toString(), "tipoMisiones", "BIBLIOTECA", "plazoDisputaHoras", 6,
                        "entradasTardias", false, "masterJuega", masterJuega))
                .post("/api/v1/partidas").then().statusCode(201).extract().response();
        Juego juego = new Juego(UUID.fromString(respuesta.path("id")),
                respuesta.path("codigoInvitacion"));
        List<Persona> inscritos = new ArrayList<>();
        if (masterJuega) {
            inscritos.add(master);
        }
        while (inscritos.size() < jugadores) {
            Persona jugador = registrar();
            given().port(puerto).auth().oauth2(jugador.token()).contentType(ContentType.JSON)
                    .body(Map.of("codigo", juego.codigo(), "aceptaNormas", true))
                    .post("/api/v1/inscripciones").then().statusCode(201);
            inscritos.add(jugador);
        }
        return new Mesa(master, juego, inscritos);
    }

    private Persona registrar() {
        String alias = "f_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Response respuesta = given().port(puerto).contentType(ContentType.JSON)
                .body(Map.of("alias", alias, "email", alias + "@example.com",
                        "contrasena", "fantasma2026"))
                .post("/api/v1/cuentas").then().statusCode(201).extract().response();
        return new Persona(UUID.fromString(respuesta.path("cuenta.id")),
                respuesta.path("tokenAcceso"));
    }

    private Response arrancar(Mesa mesa) {
        return given().port(puerto).auth().oauth2(mesa.master().token())
                .post("/api/v1/partidas/{id}/arranque", mesa.juego().id());
    }

    private Response eliminar(Persona persona, Juego juego, UUID victimaId) {
        byte[] secreto = jdbc.queryForObject("select secreto_codigo from jugador_partida"
                + " where partida_id = ? and inscripcion_id = ?", byte[].class,
                juego.id(), victimaId);
        String codigo = generador.generar(secreto, Instant.now()).codigo();
        return given().port(puerto).auth().oauth2(persona.token()).contentType(ContentType.JSON)
                .body(Map.of("codigo", codigo)).post(RUTA_ELIMINAR, juego.id())
                .then().statusCode(200).extract().response();
    }

    private Response feed(Persona persona, Juego juego) {
        return given().port(puerto).auth().oauth2(persona.token()).get(RUTA_FEED, juego.id());
    }

    private UUID inscripcion(Persona persona, Juego juego) {
        return jdbc.queryForObject("select id from inscripcion where partida_id = ?"
                + " and cuenta_id = ?", UUID.class, juego.id(), persona.id());
    }

    private UUID objetivo(Juego juego, UUID inscripcionId) {
        return jdbc.queryForObject("select objetivo_id from jugador_partida"
                + " where partida_id = ? and inscripcion_id = ?", UUID.class,
                juego.id(), inscripcionId);
    }

    private String alias(UUID inscripcionId) {
        return jdbc.queryForObject("select c.alias from inscripcion i"
                + " join cuenta c on c.id = i.cuenta_id where i.id = ?", String.class,
                inscripcionId);
    }

    private record Persona(UUID id, String token) {
    }

    private record Juego(UUID id, String codigo) {
    }

    private record Mesa(Persona master, Juego juego, List<Persona> jugadores) {
    }
}
