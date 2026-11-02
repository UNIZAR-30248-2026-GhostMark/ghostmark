package es.unizar.ghostmark.backend.juego;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CodigoVidaTest {

    private static final String RUTA = "/api/v1/partidas/{id}/yo/codigo";

    @LocalServerPort
    int puerto;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoBean
    Clock reloj;

    @BeforeEach
    void fijarReloj() {
        when(reloj.instant()).thenReturn(Instant.now());
        when(reloj.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @Test
    void jugadorVivoSoloObtieneSuCodigoAunqueIntenteElegirOtro() {
        Mesa mesa = preparar(false);
        arrancar(mesa);
        Persona jugador = mesa.jugadores().get(0);
        Persona otro = mesa.jugadores().get(1);
        Response respuesta = codigo(jugador, mesa.partida()).then().statusCode(200)
                .header("Cache-Control", equalTo("no-store")).extract().response();
        assertEquals(Set.of("codigo", "caducaEn"), respuesta.jsonPath().getMap("").keySet());
        assertTrue(respuesta.<String>path("codigo").matches("[0-9]{6}"));
        byte[] secreto = jdbc.queryForObject("select secreto_codigo from jugador_partida"
                + " where inscripcion_id = ?", byte[].class, inscripcion(jugador, mesa));
        CodigoVida esperado = new GeneradorCodigoVida().generar(secreto, reloj.instant());
        assertEquals(esperado.codigo(), respuesta.path("codigo"));
        assertEquals(esperado.caducaEn(), Instant.parse(respuesta.path("caducaEn")));
        given().port(puerto).auth().oauth2(jugador.token())
                .queryParam("inscripcionId", inscripcion(otro, mesa))
                .queryParam("cuentaId", otro.id()).get(RUTA, mesa.partida())
                .then().statusCode(200).body("codigo", equalTo(esperado.codigo()));
        given().port(puerto).auth().oauth2(jugador.token())
                .get("/api/v1/partidas/{id}/inscritos/{otro}/codigo",
                        mesa.partida(), inscripcion(otro, mesa)).then().statusCode(404);
    }

    @Test
    void masterQueNoJuegaNoTieneCodigoNiPuedeElegirUnJugador() {
        Mesa mesa = preparar(false);
        codigo(mesa.master(), mesa.partida()).then().statusCode(404)
                .body("codigo", equalTo("NO_ENCONTRADA"));
        arrancar(mesa);
        given().port(puerto).auth().oauth2(mesa.master().token())
                .queryParam("inscripcionId", inscripcion(mesa.jugadores().get(0), mesa))
                .get(RUTA, mesa.partida()).then().statusCode(404)
                .body("codigo", equalTo("NO_ENCONTRADA"));
    }

    @Test
    void eliminadoNoObtieneSuCodigoNiElDeOtro() {
        Mesa mesa = preparar(false);
        arrancar(mesa);
        Persona eliminado = mesa.jugadores().get(0);
        // La eliminación se simula en BD porque implementarla pertenece a la 7a.3
        jdbc.update("update jugador_partida set estado = 'ELIMINADO', objetivo_id = null,"
                + " mision_id = null, eliminado_en = now() where inscripcion_id = ?",
                inscripcion(eliminado, mesa));
        codigo(eliminado, mesa.partida()).then().statusCode(409)
                .body("codigo", equalTo("JUGADOR_ELIMINADO"));
        given().port(puerto).auth().oauth2(eliminado.token())
                .queryParam("inscripcionId", inscripcion(mesa.jugadores().get(1), mesa))
                .get(RUTA, mesa.partida()).then().statusCode(409)
                .body("codigo", equalTo("JUGADOR_ELIMINADO"));
        codigo(mesa.jugadores().get(1), mesa.partida()).then().statusCode(200);
    }

    @Test
    void masterQueJuegaObtieneSuCodigoComoCualquierJugador() {
        Mesa mesa = preparar(true);
        arrancar(mesa);
        codigo(mesa.master(), mesa.partida()).then().statusCode(200);
    }

    @Test
    void ajenoPartidaInexistenteYSinSesionSeRechazan() {
        Mesa mesa = preparar(false);
        Persona ajeno = registrar();
        codigo(ajeno, mesa.partida()).then().statusCode(404)
                .body("codigo", equalTo("NO_ENCONTRADA"));
        arrancar(mesa);
        codigo(ajeno, mesa.partida()).then().statusCode(404)
                .body("codigo", equalTo("NO_ENCONTRADA"));
        codigo(mesa.jugadores().get(0), UUID.randomUUID()).then().statusCode(404)
                .body("codigo", equalTo("NO_ENCONTRADA"));
        given().port(puerto).get(RUTA, mesa.partida()).then().statusCode(401)
                .body("codigo", equalTo("NO_AUTENTICADO"));
        given().port(puerto).auth().oauth2("token-invalido").get(RUTA, mesa.partida())
                .then().statusCode(401).body("codigo", equalTo("NO_AUTENTICADO"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"EN_INSCRIPCION", "CANCELADA", "CERRADA"})
    void inscritoFueraDeJuegoNoObtieneCodigo(String estado) {
        Mesa mesa = preparar(true);
        jdbc.update("update partida set estado = ? where id = ?", estado, mesa.partida());
        codigo(mesa.master(), mesa.partida()).then().statusCode(409)
                .body("codigo", equalTo("PARTIDA_NO_EN_JUEGO"));
    }

    @Test
    void rotaEnElLimiteDelMinutoSinEsperasReales() {
        Mesa mesa = preparar(true);
        arrancar(mesa);
        jdbc.update("update jugador_partida set secreto_codigo = ? where inscripcion_id = ?",
                "12345678901234567890".getBytes(StandardCharsets.US_ASCII),
                inscripcion(mesa.master(), mesa));
        Instant limite = Instant.ofEpochSecond(120);
        when(reloj.instant()).thenReturn(limite.minusSeconds(30));
        String primero = codigo(mesa.master(), mesa.partida()).then().statusCode(200)
                .extract().path("codigo");
        assertEquals("287082", primero);
        when(reloj.instant()).thenReturn(limite.minusNanos(1));
        Response antes = codigo(mesa.master(), mesa.partida()).then().statusCode(200)
                .body("codigo", equalTo(primero)).extract().response();
        assertEquals(limite, Instant.parse(antes.path("caducaEn")));
        when(reloj.instant()).thenReturn(limite);
        Response despues = codigo(mesa.master(), mesa.partida()).then().statusCode(200)
                .extract().response();
        assertNotEquals(primero, despues.path("codigo"));
        assertEquals("359152", despues.path("codigo"));
        assertEquals(limite.plusSeconds(60), Instant.parse(despues.path("caducaEn")));
    }

    @Test
    void arranqueCreaEstadoSecretosYAsignacionesUnaSolaVez() {
        Mesa mesa = preparar(true);
        arrancar(mesa);
        assertEquals(3, jdbc.queryForObject("select count(*) from jugador_partida"
                + " where partida_id = ? and estado = 'VIVO' and eliminaciones = 0"
                + " and intentos_fallidos = 0 and bloqueado_hasta is null"
                + " and eliminado_en is null and octet_length(secreto_codigo) = 20",
                Integer.class, mesa.partida()));
        assertEquals(3, jdbc.queryForObject("select count(distinct secreto_codigo)"
                + " from jugador_partida where partida_id = ?", Integer.class, mesa.partida()));
        for (Persona persona : mesa.jugadores()) {
            UUID id = inscripcion(persona, mesa);
            String alias = jdbc.queryForObject("select c.alias from jugador_partida j"
                    + " join inscripcion i on i.id = j.objetivo_id"
                    + " join cuenta c on c.id = i.cuenta_id where j.inscripcion_id = ?",
                    String.class, id);
            String mision = jdbc.queryForObject("select m.texto from jugador_partida j"
                    + " join mision m on m.id = j.mision_id where j.inscripcion_id = ?",
                    String.class, id);
            given().port(puerto).auth().oauth2(persona.token())
                    .get("/api/v1/partidas/{id}/yo/carta", mesa.partida()).then().statusCode(200)
                    .body("aliasObjetivo", equalTo(alias)).body("mision", equalTo(mision));
        }
        List<Map<String, Object>> antes = filas(mesa);
        given().port(puerto).auth().oauth2(mesa.master().token())
                .post("/api/v1/partidas/{id}/arranque", mesa.partida()).then().statusCode(409);
        assertEquals(antes, filas(mesa));
    }

    @Test
    void consultaAplicaElArranqueProgramadoPendiente() {
        Mesa mesa = preparar(false);
        jdbc.update("update partida set inicio_en = now() - interval '1 minute' where id = ?",
                mesa.partida());
        codigo(mesa.jugadores().get(0), mesa.partida()).then().statusCode(200);
        assertEquals(3, filas(mesa).size());
    }

    @Test
    void borradoDeCuentaConservaJugadorEventosYAliasNulo() {
        Mesa mesa = preparar(true);
        arrancar(mesa);
        Persona victima = mesa.jugadores().get(1);
        UUID id = inscripcion(victima, mesa);
        UUID cazadorCuenta = jdbc.queryForObject("select i.cuenta_id from jugador_partida j"
                + " join inscripcion i on i.id = j.inscripcion_id where j.objetivo_id = ?",
                UUID.class, id);
        Persona cazador = mesa.jugadores().stream().filter(p -> p.id().equals(cazadorCuenta))
                .findFirst().orElseThrow();
        List<Map<String, Object>> eventos = jdbc.queryForList("select * from evento"
                + " where partida_id = ? order by secuencia", mesa.partida());
        List<Map<String, Object>> jugadores = filas(mesa);
        given().port(puerto).auth().oauth2(victima.token()).delete("/api/v1/cuentas/yo")
                .then().statusCode(204);
        assertEquals(jugadores, filas(mesa));
        assertEquals(eventos, jdbc.queryForList("select * from evento"
                + " where partida_id = ? order by secuencia", mesa.partida()));
        given().port(puerto).auth().oauth2(cazador.token())
                .get("/api/v1/partidas/{id}/yo/carta", mesa.partida()).then().statusCode(200)
                .body("aliasObjetivo", org.hamcrest.Matchers.nullValue());
        codigo(victima, mesa.partida()).then().statusCode(401)
                .body("codigo", equalTo("NO_AUTENTICADO"));
    }

    private List<Map<String, Object>> filas(Mesa mesa) {
        return jdbc.queryForList("select inscripcion_id, objetivo_id, mision_id, estado,"
                + " encode(secreto_codigo, 'hex') as secreto from jugador_partida"
                + " where partida_id = ? order by inscripcion_id", mesa.partida());
    }

    private Mesa preparar(boolean masterJuega) {
        Persona master = registrar();
        Instant inicio = reloj.instant().plus(Duration.ofDays(1));
        Response respuesta = given().port(puerto).auth().oauth2(master.token())
                .contentType(ContentType.JSON).body(Map.of("nombre", "Partida Codigo",
                        "inicioEn", inicio.toString(), "finEn", inicio.plus(Duration.ofDays(7))
                                .toString(), "tipoMisiones", "BIBLIOTECA", "plazoDisputaHoras", 6,
                        "entradasTardias", false, "masterJuega", masterJuega))
                .post("/api/v1/partidas").then().statusCode(201).extract().response();
        UUID partida = UUID.fromString(respuesta.path("id"));
        List<Persona> jugadores = new ArrayList<>();
        if (masterJuega) {
            jugadores.add(master);
        }
        while (jugadores.size() < 3) {
            Persona jugador = registrar();
            given().port(puerto).auth().oauth2(jugador.token()).contentType(ContentType.JSON)
                    .body(Map.of("codigo", respuesta.<String>path("codigoInvitacion"),
                            "aceptaNormas", true))
                    .post("/api/v1/inscripciones").then().statusCode(201);
            jugadores.add(jugador);
        }
        return new Mesa(master, partida, jugadores);
    }

    private Persona registrar() {
        String alias = "cv_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Response respuesta = given().port(puerto).contentType(ContentType.JSON)
                .body(Map.of("alias", alias, "email", alias + "@example.com",
                        "contrasena", "fantasma2026"))
                .post("/api/v1/cuentas").then().statusCode(201).extract().response();
        return new Persona(UUID.fromString(respuesta.path("cuenta.id")),
                respuesta.path("tokenAcceso"));
    }

    private void arrancar(Mesa mesa) {
        given().port(puerto).auth().oauth2(mesa.master().token())
                .post("/api/v1/partidas/{id}/arranque", mesa.partida()).then().statusCode(200);
    }

    private Response codigo(Persona persona, UUID partida) {
        return given().port(puerto).auth().oauth2(persona.token()).get(RUTA, partida);
    }

    private UUID inscripcion(Persona persona, Mesa mesa) {
        return jdbc.queryForObject("select id from inscripcion where partida_id = ?"
                + " and cuenta_id = ?", UUID.class, mesa.partida(), persona.id());
    }

    private record Persona(UUID id, String token) {
    }

    private record Mesa(Persona master, UUID partida, List<Persona> jugadores) {
    }
}
