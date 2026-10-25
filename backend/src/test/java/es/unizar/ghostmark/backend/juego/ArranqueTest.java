package es.unizar.ghostmark.backend.juego;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import es.unizar.ghostmark.backend.TestcontainersConfiguration;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
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
class ArranqueTest {

    private static final String RUTA_ARRANQUE = "/api/v1/partidas/{id}/arranque";

    @LocalServerPort
    int puerto;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ServicioArranque arranque;

    @Autowired
    TareaArranque tarea;

    @Test
    void masterArrancaYaConTresJugadores() {
        Persona master = registrar();
        Juego juego = crear(master, true);
        apuntar(registrar(), juego);
        apuntar(registrar(), juego);
        arrancarYa(master, juego).then().statusCode(200)
                .body("id", equalTo(juego.id().toString()))
                .body("estado", equalTo("EN_JUEGO"))
                .body("soyMaster", equalTo(true))
                .body("numInscritos", equalTo(3));
        assertEquals("EN_JUEGO", estado(juego));
        assertEquals(List.of("ADELANTADO|3"), arranques(juego));
    }

    @Test
    void masterQueNoJuegaArrancaConTresInscritos() {
        Persona master = registrar();
        Juego juego = crear(master, false);
        apuntar(registrar(), juego);
        apuntar(registrar(), juego);
        apuntar(registrar(), juego);
        arrancarYa(master, juego).then().statusCode(200).body("estado", equalTo("EN_JUEGO"));
        assertEquals(List.of("ADELANTADO|3"), arranques(juego));
    }

    @Test
    void conMenosDeTresJugadoresDa409YNoArranca() {
        Persona master = registrar();
        Juego juego = crear(master, true);
        apuntar(registrar(), juego);
        arrancarYa(master, juego).then().statusCode(409)
                .body("codigo", equalTo("POCOS_JUGADORES"));
        assertEquals("EN_INSCRIPCION", estado(juego));
        assertEquals(List.of(), arranques(juego));
        assertEquals(List.of(), cancelaciones(juego));
    }

    @Test
    void soloElMasterPuedeArrancar() {
        Persona master = registrar();
        Persona jugador = registrar();
        Juego juego = crear(master, true);
        apuntar(jugador, juego);
        apuntar(registrar(), juego);
        arrancarYa(jugador, juego).then().statusCode(403)
                .body("codigo", equalTo("NO_ES_MASTER"));
        arrancarYa(registrar(), juego).then().statusCode(404)
                .body("codigo", equalTo("NO_ENCONTRADA"));
        given().port(puerto).post(RUTA_ARRANQUE, juego.id()).then().statusCode(401)
                .body("codigo", equalTo("NO_AUTENTICADO"));
        arrancarYa(master, new Juego(UUID.randomUUID(), "")).then().statusCode(404);
        assertEquals("EN_INSCRIPCION", estado(juego));
        assertEquals(List.of(), arranques(juego));
    }

    @Test
    void partidaYaArrancadaDa409() {
        Persona master = registrar();
        Juego juego = conTresJugadores(master);
        arrancarYa(master, juego).then().statusCode(200);
        arrancarYa(master, juego).then().statusCode(409)
                .body("codigo", equalTo("PARTIDA_NO_ABIERTA"));
        assertEquals(1, arranques(juego).size());
    }

    @Test
    void dosArranquesSimultaneosSoloArrancanUnaVez() throws Exception {
        Persona master = registrar();
        Juego juego = conTresJugadores(master);
        CyclicBarrier barrera = new CyclicBarrier(2);
        Callable<Integer> operacion = () -> {
            barrera.await();
            return arrancarYa(master, juego).statusCode();
        };
        List<Integer> codigos = new ArrayList<>();
        try (var ejecutor = Executors.newFixedThreadPool(2)) {
            for (Future<Integer> resultado : ejecutor.invokeAll(List.of(operacion, operacion))) {
                codigos.add(resultado.get());
            }
        }
        assertEquals(List.of(200, 409), codigos.stream().sorted().toList());
        assertEquals(1, arranques(juego).size());
        assertEquals(1, ciclos(juego));
    }

    @Test
    void arrancaSolaAlLlegarLaHoraDeInicio() {
        Juego juego = conTresJugadores(registrar());
        assertFalse(arranque.aplicarInicio(juego.id(), Instant.now()));
        assertEquals("EN_INSCRIPCION", estado(juego));
        jdbc.update("update partida set inicio_en = now() - interval '1 minute' where id = ?",
                juego.id());
        assertTrue(arranque.aplicarInicio(juego.id(), Instant.now()));
        assertFalse(arranque.aplicarInicio(juego.id(), Instant.now()));
        assertEquals("EN_JUEGO", estado(juego));
        assertEquals(List.of("PROGRAMADO|3"), arranques(juego));
        assertEquals(1, ciclos(juego));
    }

    @Test
    void horarioQuedaCongeladoAlArrancar() {
        Persona master = registrar();
        Persona jugador = registrar();
        Juego juego = crear(master, true);
        apuntar(jugador, juego);
        apuntar(registrar(), juego);
        arrancarYa(master, juego).then().statusCode(200);
        given().port(puerto).auth().oauth2(jugador.token()).contentType(ContentType.JSON)
                .body(Map.of("franjas", List.of()))
                .put("/api/v1/partidas/{id}/yo/horario", juego.id())
                .then().statusCode(409).body("codigo", equalTo("PARTIDA_NO_EDITABLE"));
    }

    @Test
    void tareaProgramadaArrancaSoloLasVencidas() {
        Juego vencida = conTresJugadores(registrar());
        Juego futura = conTresJugadores(registrar());
        vencer(vencida);
        tarea.revisar(Instant.now());
        assertEquals("EN_JUEGO", estado(vencida));
        assertEquals(List.of("PROGRAMADO|3"), arranques(vencida));
        assertEquals("EN_INSCRIPCION", estado(futura));
        assertEquals(List.of(), arranques(futura));
    }

    @Test
    void consultarAplicaElArranquePendiente() {
        Persona master = registrar();
        Juego juego = conTresJugadores(master);
        Juego otra = conTresJugadores(master);
        vencer(juego);
        given().port(puerto).auth().oauth2(master.token())
                .get("/api/v1/partidas/codigo/{codigo}", juego.codigo())
                .then().statusCode(200).body("estado", equalTo("EN_JUEGO"));
        assertEquals(List.of("PROGRAMADO|3"), arranques(juego));
        vencer(otra);
        given().port(puerto).auth().oauth2(master.token()).get("/api/v1/cuentas/yo/partidas")
                .then().statusCode(200);
        assertEquals("EN_JUEGO", estado(otra));
        assertEquals(List.of("PROGRAMADO|3"), arranques(otra));
    }

    @Test
    void inscribirseTardeArrancaYRechaza() {
        Juego juego = conTresJugadores(registrar());
        vencer(juego);
        given().port(puerto).auth().oauth2(registrar().token()).contentType(ContentType.JSON)
                .body(Map.of("codigo", juego.codigo(), "aceptaNormas", true))
                .post("/api/v1/inscripciones").then().statusCode(409)
                .body("codigo", equalTo("PARTIDA_NO_ABIERTA"));
        assertEquals("EN_JUEGO", estado(juego));
        assertEquals(List.of("PROGRAMADO|3"), arranques(juego));
    }

    private void vencer(Juego juego) {
        jdbc.update("update partida set inicio_en = now() - interval '1 minute' where id = ?",
                juego.id());
    }

    @ParameterizedTest
    @ValueSource(ints = {3, 4, 10})
    void formaUnCicloUnicoYCerrado(int jugadores) {
        Persona master = registrar();
        Juego juego = crear(master, true);
        for (int i = 1; i < jugadores; i++) {
            apuntar(registrar(), juego);
        }
        arrancarYa(master, juego).then().statusCode(200);
        List<UUID> inscritos = jdbc.queryForList(
                "select id from inscripcion where partida_id = ?", UUID.class, juego.id());
        List<UUID> orden = orden(juego);
        assertEquals(jugadores, orden.size());
        assertEquals(jugadores, Set.copyOf(orden).size());
        assertEquals(Set.copyOf(inscritos), Set.copyOf(orden));
        Map<UUID, UUID> objetivos = new HashMap<>();
        for (int i = 0; i < jugadores; i++) {
            objetivos.put(orden.get(i), orden.get((i + 1) % jugadores));
        }
        Set<UUID> visitados = new HashSet<>();
        UUID actual = orden.get(0);
        for (int i = 0; i < jugadores; i++) {
            UUID objetivo = objetivos.get(actual);
            assertFalse(actual.equals(objetivo));
            assertTrue(visitados.add(actual));
            actual = objetivo;
        }
        assertEquals(orden.get(0), actual);
        assertEquals(Set.copyOf(inscritos), visitados);
    }

    @Test
    void cicloVaJustoDespuesDelArranque() {
        Persona master = registrar();
        Juego juego = conTresJugadores(master);
        arrancarYa(master, juego).then().statusCode(200);
        List<Map<String, Object>> eventos = jdbc.queryForList("select tipo, secuencia from evento"
                + " where partida_id = ? order by secuencia desc limit 2", juego.id());
        assertEquals("CICLO_FORMADO", eventos.get(0).get("tipo"));
        assertEquals("PARTIDA_ARRANCADA", eventos.get(1).get("tipo"));
        assertEquals((Long) eventos.get(1).get("secuencia") + 1, eventos.get(0).get("secuencia"));
    }

    @Test
    void cancelaAlLlegarLaHoraConMenosDeTres() {
        Persona master = registrar();
        Juego juego = crear(master, true);
        apuntar(registrar(), juego);
        vencer(juego);
        assertTrue(arranque.aplicarInicio(juego.id(), Instant.now()));
        assertEquals("CANCELADA", estado(juego));
        assertEquals(List.of("POCOS_JUGADORES|2"), cancelaciones(juego));
        assertEquals(List.of(), arranques(juego));
        assertEquals(0, ciclos(juego));
    }

    @Test
    void losInscritosVenLaCancelacionAlAbrirLaApp() {
        Persona jugador = registrar();
        Juego juego = crear(registrar(), true);
        apuntar(jugador, juego);
        vencer(juego);
        given().port(puerto).auth().oauth2(jugador.token()).get("/api/v1/cuentas/yo/partidas")
                .then().statusCode(200)
                .body("find { it.id == '" + juego.id() + "' }.estado", equalTo("CANCELADA"));
        assertEquals(List.of("POCOS_JUGADORES|2"), cancelaciones(juego));
    }

    @Test
    void sinJugadoresSeCancelaConCero() {
        Juego juego = crear(registrar(), false);
        vencer(juego);
        tarea.revisar(Instant.now());
        assertEquals("CANCELADA", estado(juego));
        assertEquals(List.of("POCOS_JUGADORES|0"), cancelaciones(juego));
    }

    @Test
    void antesDeLaHoraNoCancelaYSoloCancelaUnaVez() {
        Persona master = registrar();
        Juego juego = crear(master, true);
        apuntar(registrar(), juego);
        assertFalse(arranque.aplicarInicio(juego.id(), Instant.now()));
        assertEquals("EN_INSCRIPCION", estado(juego));
        vencer(juego);
        tarea.revisar(Instant.now());
        tarea.revisar(Instant.now());
        assertFalse(arranque.aplicarInicio(juego.id(), Instant.now()));
        assertEquals("CANCELADA", estado(juego));
        assertEquals(List.of("POCOS_JUGADORES|2"), cancelaciones(juego));
    }

    private List<String> cancelaciones(Juego juego) {
        return jdbc.queryForList("select (datos->>'motivo') || '|' || (datos->>'numJugadores')"
                + " from evento where partida_id = ? and tipo = 'PARTIDA_CANCELADA'"
                + " order by secuencia", String.class, juego.id());
    }

    private List<UUID> orden(Juego juego) {
        return jdbc.queryForList("select e.valor::uuid from evento ev,"
                + " jsonb_array_elements_text(ev.datos->'orden') with ordinality as e(valor, pos)"
                + " where ev.partida_id = ? and ev.tipo = 'CICLO_FORMADO' order by e.pos",
                UUID.class, juego.id());
    }

    private int ciclos(Juego juego) {
        return jdbc.queryForObject("select count(*) from evento where partida_id = ?"
                + " and tipo = 'CICLO_FORMADO'", Integer.class, juego.id());
    }

    private Juego conTresJugadores(Persona master) {
        Juego juego = crear(master, true);
        apuntar(registrar(), juego);
        apuntar(registrar(), juego);
        return juego;
    }

    private Persona registrar() {
        String alias = "a_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Response respuesta = given().port(puerto).contentType(ContentType.JSON)
                .body(Map.of("alias", alias, "email", alias + "@example.com",
                        "contrasena", "fantasma2026"))
                .post("/api/v1/cuentas").then().statusCode(201).extract().response();
        return new Persona(UUID.fromString(respuesta.path("cuenta.id")),
                respuesta.path("tokenAcceso"));
    }

    private Juego crear(Persona master, boolean juega) {
        Instant inicio = Instant.now().plus(Duration.ofDays(1));
        Response respuesta = given().port(puerto).auth().oauth2(master.token())
                .contentType(ContentType.JSON).body(Map.of("nombre", "Partida Arranque",
                        "inicioEn", inicio.toString(), "finEn", inicio.plus(Duration.ofDays(7))
                                .toString(), "tipoMisiones", "BIBLIOTECA", "plazoDisputaHoras", 6,
                        "entradasTardias", false, "masterJuega", juega))
                .post("/api/v1/partidas").then().statusCode(201).extract().response();
        return new Juego(UUID.fromString(respuesta.path("id")), respuesta.path("codigoInvitacion"));
    }

    private void apuntar(Persona jugador, Juego juego) {
        given().port(puerto).auth().oauth2(jugador.token()).contentType(ContentType.JSON)
                .body(Map.of("codigo", juego.codigo(), "aceptaNormas", true))
                .post("/api/v1/inscripciones").then().statusCode(201);
    }

    private Response arrancarYa(Persona persona, Juego juego) {
        return given().port(puerto).auth().oauth2(persona.token())
                .post(RUTA_ARRANQUE, juego.id());
    }

    private String estado(Juego juego) {
        return jdbc.queryForObject("select estado from partida where id = ?", String.class,
                juego.id());
    }

    private List<String> arranques(Juego juego) {
        return jdbc.queryForList("select (datos->>'modo') || '|' || (datos->>'numJugadores')"
                + " from evento where partida_id = ? and tipo = 'PARTIDA_ARRANCADA'"
                + " order by secuencia", String.class, juego.id());
    }

    private record Persona(UUID id, String token) {
    }

    private record Juego(UUID id, String codigo) {
    }
}
