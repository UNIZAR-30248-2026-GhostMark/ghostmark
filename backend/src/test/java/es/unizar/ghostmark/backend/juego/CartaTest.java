package es.unizar.ghostmark.backend.juego;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CartaTest {

    private static final String RUTA_CARTA = "/api/v1/partidas/{id}/yo/carta";

    @LocalServerPort
    int puerto;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ServicioArranque arranque;

    @ParameterizedTest
    @ValueSource(strings = {"BIBLIOTECA", "IA"})
    void reparteAlArrancarYDevuelveLaCartaPropia(String tipoMisiones) {
        Mesa mesa = preparar(4, true, tipoMisiones);
        arrancar(mesa).then().statusCode(200);
        List<Map<String, Object>> reparto = asignaciones(mesa.juego());
        List<UUID> orden = orden(mesa.juego());
        assertEquals(4, reparto.size());
        assertEquals(new HashSet<>(orden), new HashSet<>(reparto.stream()
                .map(fila -> (UUID) fila.get("inscripcion_id")).toList()));
        assertEquals(4, reparto.stream().map(fila -> fila.get("mision_id")).distinct().count());
        assertTrue(reparto.stream().allMatch(fila -> Boolean.TRUE.equals(fila.get("activa"))));
        for (Persona jugador : mesa.jugadores()) {
            UUID inscripcionId = inscripcion(jugador, mesa.juego());
            UUID objetivoId = orden.get((orden.indexOf(inscripcionId) + 1) % orden.size());
            String aliasObjetivo = jdbc.queryForObject("select c.alias from inscripcion i"
                    + " join cuenta c on c.id = i.cuenta_id where i.id = ?", String.class,
                    objetivoId);
            String mision = reparto.stream().filter(fila -> inscripcionId.equals(
                    fila.get("inscripcion_id"))).findFirst().orElseThrow().get("texto").toString();
            Response respuesta = carta(jugador, mesa.juego()).then().statusCode(200)
                    .body("aliasObjetivo", equalTo(aliasObjetivo)).body("mision", equalTo(mision))
                    .extract().response();
            assertEquals(Set.of("aliasObjetivo", "mision"),
                    respuesta.jsonPath().getMap("").keySet());
            assertEquals(respuesta.asString(), carta(jugador, mesa.juego()).asString());
        }
        List<Map<String, Object>> eventos = jdbc.queryForList("select tipo, secuencia, ocurrido_en"
                + " from evento where partida_id = ? order by secuencia desc limit 3",
                mesa.juego().id());
        assertEquals(List.of("MISIONES_REPARTIDAS", "CICLO_FORMADO", "PARTIDA_ARRANCADA"),
                eventos.stream().map(fila -> fila.get("tipo")).toList());
        assertEquals((Long) eventos.get(2).get("secuencia") + 1, eventos.get(1).get("secuencia"));
        assertEquals((Long) eventos.get(1).get("secuencia") + 1, eventos.get(0).get("secuencia"));
        assertEquals(1, eventos.stream().map(fila -> fila.get("ocurrido_en")).distinct().count());
        assertEquals(0, jdbc.queryForObject("select count(*) from evento e,"
                + " jsonb_array_elements(e.datos->'asignaciones') as a(valor)"
                + " where e.partida_id = ? and e.tipo = 'MISIONES_REPARTIDAS'"
                + " and (select count(*) from jsonb_object_keys(a.valor)) <> 2",
                Integer.class, mesa.juego().id()));
    }

    @Test
    void noRepiteHastaAgotarLasActivasYDespuesVuelveARepartir() {
        int disponibles = jdbc.queryForObject("select count(*) from mision where activa",
                Integer.class) - 1;
        UUID retirada = jdbc.queryForObject(
                "select id from mision where activa order by id limit 1", UUID.class);
        Mesa mesa = preparar(disponibles + 3, true, "BIBLIOTECA");
        jdbc.update("update mision set activa = false where id = ?", retirada);
        try {
            arrancar(mesa).then().statusCode(200);
            List<Map<String, Object>> reparto = asignaciones(mesa.juego());
            assertEquals(disponibles + 3, reparto.size());
            assertEquals(disponibles, reparto.subList(0, disponibles).stream()
                    .map(fila -> fila.get("mision_id")).distinct().count());
            assertEquals(Math.min(3, disponibles),
                    reparto.subList(disponibles, reparto.size()).stream()
                            .map(fila -> fila.get("mision_id")).distinct().count());
            assertTrue(reparto.stream().allMatch(fila -> Boolean.TRUE.equals(fila.get("activa"))));
            assertFalse(reparto.stream().anyMatch(fila -> retirada.equals(fila.get("mision_id"))));
        } finally {
            jdbc.update("update mision set activa = true where id = ?", retirada);
        }
    }

    @Test
    void consultarLaCartaAplicaElArranqueProgramadoPendiente() {
        Mesa mesa = preparar(3, true, "BIBLIOTECA");
        jdbc.update("update partida set inicio_en = now() - interval '1 minute' where id = ?",
                mesa.juego().id());
        carta(mesa.jugadores().get(0), mesa.juego()).then().statusCode(200);
        assertEquals(3, asignaciones(mesa.juego()).size());
        assertEquals("PROGRAMADO", jdbc.queryForObject("select datos->>'modo' from evento"
                + " where partida_id = ? and tipo = 'PARTIDA_ARRANCADA'", String.class,
                mesa.juego().id()));
    }

    @Test
    void dosArranquesSimultaneosSoloRepartenUnaVez() throws Exception {
        Mesa mesa = preparar(3, true, "BIBLIOTECA");
        CyclicBarrier barrera = new CyclicBarrier(2);
        Callable<Integer> operacion = () -> {
            barrera.await();
            return arrancar(mesa).statusCode();
        };
        List<Integer> codigos = new ArrayList<>();
        try (var ejecutor = Executors.newFixedThreadPool(2)) {
            for (Future<Integer> resultado : ejecutor.invokeAll(List.of(operacion, operacion))) {
                codigos.add(resultado.get());
            }
        }
        assertEquals(List.of(200, 409), codigos.stream().sorted().toList());
        assertEquals(1, jdbc.queryForObject("select count(*) from evento where partida_id = ?"
                + " and tipo = 'MISIONES_REPARTIDAS'", Integer.class, mesa.juego().id()));
        assertEquals(3, asignaciones(mesa.juego()).size());
    }

    @Test
    void sinMisionesElArranqueSeRevierteCompleto() {
        Mesa mesa = preparar(3, true, "BIBLIOTECA");
        List<UUID> activas = jdbc.queryForList("select id from mision where activa", UUID.class);
        List<Map<String, Object>> antes = jdbc.queryForList("select * from evento"
                + " where partida_id = ? order by secuencia", mesa.juego().id());
        jdbc.update("update mision set activa = false where activa");
        try {
            assertThrows(IllegalStateException.class, () -> arranque.arrancarYa(
                    mesa.master().id(), mesa.juego().id(), Instant.now()));
            assertEquals("EN_INSCRIPCION", jdbc.queryForObject("select estado from partida"
                    + " where id = ?", String.class, mesa.juego().id()));
            assertEquals(antes, jdbc.queryForList("select * from evento"
                    + " where partida_id = ? order by secuencia", mesa.juego().id()));
        } finally {
            for (UUID id : activas) {
                jdbc.update("update mision set activa = true where id = ?", id);
            }
        }
    }

    @Test
    void sinAutenticacionNoHayCarta() {
        Mesa mesa = preparar(3, true, "BIBLIOTECA");
        arrancar(mesa).then().statusCode(200);
        given().port(puerto).get(RUTA_CARTA, mesa.juego().id()).then().statusCode(401)
                .body("codigo", equalTo("NO_AUTENTICADO"));
    }

    @Test
    void ajenoYMasterQueNoJuegaReciben404() {
        Mesa mesa = preparar(3, false, "BIBLIOTECA");
        Persona ajeno = registrar();
        // Se comprueba antes del estado para no revelar si una partida ajena ha arrancado.
        for (boolean arrancada : List.of(false, true)) {
            if (arrancada) {
                arrancar(mesa).then().statusCode(200);
            }
            for (Persona persona : List.of(ajeno, mesa.master())) {
                carta(persona, mesa.juego()).then().statusCode(404)
                        .body("codigo", equalTo("NO_ENCONTRADA"));
            }
        }
        carta(mesa.jugadores().get(0), new Juego(UUID.randomUUID(), "")).then().statusCode(404)
                .body("codigo", equalTo("NO_ENCONTRADA"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"EN_INSCRIPCION", "CANCELADA", "CERRADA"})
    void inscritoSinPartidaEnJuegoRecibe409(String estado) {
        Mesa mesa = preparar(3, true, "BIBLIOTECA");
        jdbc.update("update partida set estado = ? where id = ?", estado, mesa.juego().id());
        carta(mesa.master(), mesa.juego()).then().statusCode(409)
                .body("codigo", equalTo("PARTIDA_NO_EN_JUEGO"));
        assertEquals(List.of(), asignaciones(mesa.juego()));
    }

    @Test
    void elegirOtroJugadorEnLaPeticionNoCambiaLaCarta() {
        Mesa mesa = preparar(3, true, "BIBLIOTECA");
        arrancar(mesa).then().statusCode(200);
        for (Persona jugador : mesa.jugadores()) {
            Map<String, Object> propia = carta(jugador, mesa.juego()).then().statusCode(200)
                    .extract().jsonPath().getMap("");
            UUID otro = inscripcion(mesa.jugadores().get((mesa.jugadores().indexOf(jugador) + 1)
                    % mesa.jugadores().size()), mesa.juego());
            given().port(puerto).auth().oauth2(jugador.token()).queryParam("inscripcionId", otro)
                    .get(RUTA_CARTA, mesa.juego().id()).then().statusCode(200)
                    .body("aliasObjetivo", equalTo(propia.get("aliasObjetivo")))
                    .body("mision", equalTo(propia.get("mision")));
            given().port(puerto).auth().oauth2(jugador.token())
                    .get("/api/v1/partidas/{id}/inscritos/{otro}/carta", mesa.juego().id(), otro)
                    .then().statusCode(404);
        }
    }

    @Test
    void objetivoSinCuentaDaAliasNuloSinModificarLosEventos() {
        Mesa mesa = preparar(3, true, "BIBLIOTECA");
        arrancar(mesa).then().statusCode(200);
        Persona jugador = mesa.jugadores().get(0);
        List<UUID> orden = orden(mesa.juego());
        UUID objetivoId = orden.get((orden.indexOf(inscripcion(jugador, mesa.juego())) + 1)
                % orden.size());
        Persona objetivo = mesa.jugadores().stream().filter(persona -> objetivoId.equals(
                inscripcion(persona, mesa.juego()))).findFirst().orElseThrow();
        List<Map<String, Object>> antes = jdbc.queryForList("select * from evento"
                + " where partida_id = ? order by secuencia", mesa.juego().id());
        String mision = carta(jugador, mesa.juego()).then().statusCode(200)
                .extract().path("mision");
        given().port(puerto).auth().oauth2(objetivo.token()).delete("/api/v1/cuentas/yo")
                .then().statusCode(204);
        Response respuesta = carta(jugador, mesa.juego()).then().statusCode(200)
                .body("aliasObjetivo", nullValue()).body("mision", equalTo(mision))
                .extract().response();
        assertTrue(respuesta.jsonPath().getMap("").containsKey("aliasObjetivo"));
        assertEquals(antes, jdbc.queryForList("select * from evento"
                + " where partida_id = ? order by secuencia", mesa.juego().id()));
        carta(objetivo, mesa.juego()).then().statusCode(401)
                .body("codigo", equalTo("NO_AUTENTICADO"));
    }

    private Mesa preparar(int jugadores, boolean masterJuega, String tipoMisiones) {
        Persona master = registrar();
        Instant inicio = Instant.now().plus(Duration.ofDays(1));
        Response respuesta = given().port(puerto).auth().oauth2(master.token())
                .contentType(ContentType.JSON).body(Map.of("nombre", "Partida Carta",
                        "inicioEn", inicio.toString(), "finEn", inicio.plus(Duration.ofDays(7))
                                .toString(), "tipoMisiones", tipoMisiones, "plazoDisputaHoras", 6,
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
        String alias = "c_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
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

    private Response carta(Persona jugador, Juego juego) {
        return given().port(puerto).auth().oauth2(jugador.token()).get(RUTA_CARTA, juego.id());
    }

    private UUID inscripcion(Persona jugador, Juego juego) {
        return jdbc.queryForObject("select id from inscripcion where partida_id = ?"
                + " and cuenta_id = ?", UUID.class, juego.id(), jugador.id());
    }

    private List<UUID> orden(Juego juego) {
        return jdbc.queryForList("select jugador.id::uuid from evento e,"
                + " jsonb_array_elements_text(e.datos->'orden') with ordinality as jugador(id, pos)"
                + " where e.partida_id = ? and e.tipo = 'CICLO_FORMADO' order by jugador.pos",
                UUID.class, juego.id());
    }

    private List<Map<String, Object>> asignaciones(Juego juego) {
        return jdbc.queryForList("select (a.valor->>'inscripcionId')::uuid as inscripcion_id,"
                + " m.id as mision_id, m.texto, m.activa from evento e,"
                + " jsonb_array_elements(e.datos->'asignaciones') with ordinality as a(valor, pos),"
                + " mision m where e.partida_id = ? and e.tipo = 'MISIONES_REPARTIDAS'"
                + " and m.id = (a.valor->>'misionId')::uuid order by a.pos", juego.id());
    }

    private record Persona(UUID id, String token) {
    }

    private record Juego(UUID id, String codigo) {
    }

    private record Mesa(Persona master, Juego juego, List<Persona> jugadores) {
    }
}
