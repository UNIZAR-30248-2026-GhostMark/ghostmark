package es.unizar.ghostmark.backend.inscripcion;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import es.unizar.ghostmark.backend.TestcontainersConfiguration;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
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
class InscripcionTest {

    @LocalServerPort
    int puerto;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void inscribirseCopiaHorarioAceptaNormasYGuardaEvento() {
        Persona master = registrar();
        Persona jugador = registrar();
        horario(jugador);
        Juego juego = crear(master, false);
        String id = alta(jugador, juego).then().statusCode(201)
                .body("partidaId", equalTo(juego.id().toString()))
                .body("horario.franjas[0].dia", equalTo(1))
                .body("horario.franjas[0].horaInicio", equalTo("08:00"))
                .extract().path("id");
        Map<String, Object> fila = jdbc.queryForMap("select * from inscripcion where id = ?",
                UUID.fromString(id));
        assertEquals(jugador.id(), fila.get("cuenta_id"));
        assertEquals(fila.get("inscrita_en"), fila.get("normas_aceptadas_en"));
        assertEquals(id, jdbc.queryForObject("select datos->>'inscripcionId' from evento"
                + " where partida_id = ? and tipo = 'JUGADOR_INSCRITO'", String.class, juego.id()));
        assertEquals(2, contar("evento", "partida_id", juego.id()));
        lista(master, juego).then().statusCode(200).body("[0].id", equalTo(id))
                .body("[0].alias", equalTo(jugador.alias())).body("[0].esMaster", equalTo(false));
        lista(jugador, juego).then().statusCode(200).body("size()", equalTo(1));
        // La copia de una partida no cambia al editar el horario general.
        jdbc.update("delete from franja_horario where cuenta_id = ?", jugador.id());
        assertEquals(1, contar("franja_horario_inscripcion", "inscripcion_id", UUID.fromString(id)));
    }

    @Test
    void jugadorPuedeEstarEnVariasPartidasYVerlasSinDuplicados() {
        Persona master = registrar();
        Persona jugador = registrar();
        Juego primera = crear(master, true);
        Juego segunda = crear(master, false);
        alta(jugador, primera).then().statusCode(201).body("horario.franjas.size()", equalTo(0));
        alta(jugador, segunda).then().statusCode(201);
        misPartidas(jugador).then().statusCode(200).body("size()", equalTo(2))
                .body("id", hasItems(primera.id().toString(), segunda.id().toString()))
                .body("soyMaster", equalTo(List.of(false, false)));
        misPartidas(master).then().statusCode(200).body("size()", equalTo(2));
        given().port(puerto).auth().oauth2(jugador.token()).when()
                .get("/api/v1/partidas/codigo/{codigo}", primera.codigo()).then()
                .statusCode(200).body("numInscritos", equalTo(2));
    }

    @Test
    void masterQueJuegaSeInscribeDesdeLaCreacion() {
        Persona master = registrar();
        horario(master);
        Juego juego = crear(master, true);
        lista(master, juego).then().statusCode(200).body("size()", equalTo(1))
                .body("[0].alias", equalTo(master.alias())).body("[0].esMaster", equalTo(true));
        UUID id = jdbc.queryForObject("select id from inscripcion where partida_id = ?",
                UUID.class, juego.id());
        assertNotEquals(master.id(), id);
        assertEquals(1, contar("franja_horario_inscripcion", "inscripcion_id", id));
        assertEquals(List.of("PARTIDA_CREADA", "JUGADOR_INSCRITO"), tipos(juego));
        alta(master, juego).then().statusCode(409).body("codigo", equalTo("YA_INSCRITO"));
    }

    @Test
    void masterQueNoJuegaNoPuedeInscribirseConCodigo() {
        Persona master = registrar();
        Juego juego = crear(master, false);
        alta(master, juego).then().statusCode(409).body("codigo", equalTo("ES_MASTER"));
        assertEquals(0, contar("inscripcion", "partida_id", juego.id()));
    }

    @Test
    void normasNoAceptadasNoCreanInscripcionNiEvento() {
        Persona jugador = registrar();
        Juego juego = crear(registrar(), false);
        peticion(jugador, Map.of("codigo", juego.codigo(), "aceptaNormas", false))
                .then().statusCode(400).body("codigo", equalTo("NORMAS_NO_ACEPTADAS"));
        assertEquals(0, contar("inscripcion", "partida_id", juego.id()));
        assertEquals(List.of("PARTIDA_CREADA"), tipos(juego));
    }

    @Test
    void faltanDatosOCodigoNoValidoDan400() {
        Persona jugador = registrar();
        peticion(jugador, Map.of("aceptaNormas", true)).then().statusCode(400)
                .body("codigo", equalTo("DATOS_NO_VALIDOS"));
        peticion(jugador, Map.of("codigo", "0123")).then().statusCode(400);
        peticion(jugador, Map.of("codigo", "K7PX4MQ2")).then().statusCode(400);
        peticion(jugador, "{\"codigo\":").then().statusCode(400)
                .body("codigo", equalTo("DATOS_NO_VALIDOS"));
        peticion(jugador, "").then().statusCode(400);
    }

    @Test
    void codigoDesconocidoDa404() {
        peticion(registrar(), Map.of("codigo", "ZZZZZZZZ", "aceptaNormas", true))
                .then().statusCode(404).body("codigo", equalTo("NO_ENCONTRADA"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"EN_JUEGO", "CANCELADA", "CERRADA"})
    void partidaNoAbiertaRechazaInclusoEntradasTardias(String estado) {
        Juego juego = crear(registrar(), false);
        jdbc.update("update partida set estado = ?, entradas_tardias = true where id = ?",
                estado, juego.id());
        alta(registrar(), juego).then().statusCode(409)
                .body("codigo", equalTo("PARTIDA_NO_ABIERTA"));
        assertEquals(0, contar("inscripcion", "partida_id", juego.id()));
    }

    @Test
    void noAdmiteInscripcionTrasLaHoraDeArranquePendiente() {
        Juego juego = crear(registrar(), false);
        jdbc.update("update partida set inicio_en = now() - interval '1 minute' where id = ?",
                juego.id());
        alta(registrar(), juego).then().statusCode(409)
                .body("codigo", equalTo("PARTIDA_NO_ABIERTA"));
    }

    @Test
    void duplicadoNoCambiaFechasHorarioNiEventos() {
        Persona jugador = registrar();
        Juego juego = crear(registrar(), false);
        alta(jugador, juego).then().statusCode(201);
        List<Map<String, Object>> antes = jdbc.queryForList(
                "select * from inscripcion where partida_id = ?", juego.id());
        alta(jugador, juego).then().statusCode(409).body("codigo", equalTo("YA_INSCRITO"));
        assertEquals(antes, jdbc.queryForList(
                "select * from inscripcion where partida_id = ?", juego.id()));
        assertEquals(2, contar("evento", "partida_id", juego.id()));
    }

    @Test
    void listaPrivadaOcultaPartidaAQuienNoEstaInscrito() {
        Persona master = registrar();
        Persona ajeno = registrar();
        Juego juego = crear(master, false);
        lista(ajeno, juego).then().statusCode(404).body("codigo", equalTo("NO_ENCONTRADA"));
        lista(ajeno, new Juego(UUID.randomUUID(), "K7PX4MQ2"))
                .then().statusCode(404).body("codigo", equalTo("NO_ENCONTRADA"));
        Persona uno = registrar();
        Persona dos = registrar();
        alta(uno, juego).then().statusCode(201);
        alta(dos, juego).then().statusCode(201);
        lista(master, juego).then().statusCode(200)
                .body("alias", equalTo(List.of(uno.alias(), dos.alias())))
                .body("[0].size()", equalTo(3)).body("[0].email", nullValue());
    }

    @Test
    void sinTokenDa401EnAmbosEndpoints() {
        given().port(puerto).contentType(ContentType.JSON)
                .body(Map.of("codigo", "K7PX4MQ2", "aceptaNormas", true))
                .post("/api/v1/inscripciones").then().statusCode(401);
        given().port(puerto).get("/api/v1/partidas/{id}/inscritos", UUID.randomUUID())
                .then().statusCode(401).body("codigo", equalTo("NO_AUTENTICADO"));
    }

    @Test
    void tokenDeCuentaBorradaDa401SinCrearDatos() {
        Persona jugador = registrar();
        Juego juego = crear(registrar(), false);
        borrar(jugador);
        alta(jugador, juego).then().statusCode(401).body("codigo", equalTo("NO_AUTENTICADO"));
        lista(jugador, juego).then().statusCode(401).body("codigo", equalTo("NO_AUTENTICADO"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"EN_INSCRIPCION", "CANCELADA", "CERRADA"})
    void borrarCuentaRetiraInscripcionYHorarioSinReescribirEventos(String estado) {
        Persona jugador = registrar();
        horario(jugador);
        Juego juego = crear(registrar(), false);
        UUID id = UUID.fromString(alta(jugador, juego).then().statusCode(201).extract().path("id"));
        List<Map<String, Object>> antes = eventos(juego);
        jdbc.update("update partida set estado = ? where id = ?", estado, juego.id());
        borrar(jugador);
        assertEquals(0, contar("inscripcion", "id", id));
        assertEquals(0, contar("franja_horario_inscripcion", "inscripcion_id", id));
        assertEquals(antes, eventos(juego).subList(0, antes.size()));
        assertEquals(List.of("PARTIDA_CREADA", "JUGADOR_INSCRITO", "INSCRIPCION_RETIRADA"),
                tipos(juego));
        assertEquals("CUENTA_BORRADA", jdbc.queryForObject("select datos->>'motivo' from evento"
                + " where partida_id = ? and tipo = 'INSCRIPCION_RETIRADA'",
                String.class, juego.id()));
    }

    @Test
    void borrarCuentaEnJuegoConservaInscripcionSinCuentaNiHorarioNiEventosNuevos() {
        Persona master = registrar();
        Persona jugador = registrar();
        horario(jugador);
        Juego juego = crear(master, false);
        UUID id = UUID.fromString(alta(jugador, juego).then().statusCode(201).extract().path("id"));
        jdbc.update("update partida set estado = 'EN_JUEGO' where id = ?", juego.id());
        List<Map<String, Object>> antes = eventos(juego);
        borrar(jugador);
        assertEquals(1, contar("inscripcion", "id", id));
        assertEquals(0, contar("franja_horario_inscripcion", "inscripcion_id", id));
        assertEquals(antes, eventos(juego));
        assertTrue(jdbc.queryForObject("select cuenta_id is null from inscripcion where id = ?",
                Boolean.class, id));
        lista(master, juego).then().statusCode(200).body("[0].alias", nullValue())
                .body("[0].id", equalTo(id.toString())).body("[0].esMaster", equalTo(false));
        String datos = jdbc.queryForObject("select string_agg(datos::text, '') from evento"
                + " where partida_id = ?", String.class, juego.id());
        assertFalse(datos.contains(jugador.id().toString()));
        assertFalse(datos.contains(jugador.alias()));
    }

    @Test
    void borrarMasterCancelaConNumeroRealAntesDeRetirarlo() {
        Persona master = registrar();
        Juego juego = crear(master, true);
        alta(registrar(), juego).then().statusCode(201);
        borrar(master);
        assertEquals("2", jdbc.queryForObject("select datos->>'numJugadores' from evento"
                + " where partida_id = ? and tipo = 'PARTIDA_CANCELADA'",
                String.class, juego.id()));
        assertEquals(1, contar("inscripcion", "partida_id", juego.id()));
        assertEquals(List.of("PARTIDA_CREADA", "JUGADOR_INSCRITO", "JUGADOR_INSCRITO",
                "PARTIDA_CANCELADA", "INSCRIPCION_RETIRADA"), tipos(juego));
    }

    @Test
    void inscripcionesSimultaneasTienenSecuenciasConsecutivas() throws Exception {
        Juego juego = crear(registrar(), false);
        Persona uno = registrar();
        Persona dos = registrar();
        CyclicBarrier barrera = new CyclicBarrier(2);
        try (var ejecutor = Executors.newFixedThreadPool(2)) {
            Callable<Integer> altaUno = () -> {
                barrera.await();
                return alta(uno, juego).statusCode();
            };
            Callable<Integer> altaDos = () -> {
                barrera.await();
                return alta(dos, juego).statusCode();
            };
            var resultados = ejecutor.invokeAll(List.of(altaUno, altaDos));
            assertEquals(201, resultados.get(0).get());
            assertEquals(201, resultados.get(1).get());
        }
        assertEquals(List.of(1L, 2L, 3L), jdbc.queryForList(
                "select secuencia from evento where partida_id = ? order by secuencia",
                Long.class, juego.id()));
    }

    @Test
    void dosAltasSimultaneasDeLaMismaCuentaDan201Y409() throws Exception {
        Juego juego = crear(registrar(), false);
        Persona jugador = registrar();
        CyclicBarrier barrera = new CyclicBarrier(2);
        Callable<Integer> operacion = () -> {
            barrera.await();
            return alta(jugador, juego).statusCode();
        };
        try (var ejecutor = Executors.newFixedThreadPool(2)) {
            var resultados = ejecutor.invokeAll(List.of(operacion, operacion));
            assertEquals(List.of(201, 409), resultados.stream().map(resultado -> {
                try {
                    return resultado.get();
                } catch (Exception error) {
                    throw new IllegalStateException(error);
                }
            }).sorted().toList());
        }
        assertEquals(1, contar("inscripcion", "partida_id", juego.id()));
        assertEquals(2, contar("evento", "partida_id", juego.id()));
    }

    @Test
    void borrarMasterEnJuegoNoCancelaNiPierdeLaInscripcion() {
        Persona master = registrar();
        Juego juego = crear(master, true);
        jdbc.update("update partida set estado = 'EN_JUEGO' where id = ?", juego.id());
        List<Map<String, Object>> antes = eventos(juego);
        borrar(master);
        assertEquals(antes, eventos(juego));
        assertEquals("EN_JUEGO", jdbc.queryForObject(
                "select estado from partida where id = ?", String.class, juego.id()));
        assertTrue(jdbc.queryForObject("select master_id is null from partida where id = ?",
                Boolean.class, juego.id()));
        assertEquals(1, contar("inscripcion", "partida_id", juego.id()));
    }

    @Test
    void borrarDosCuentasConVariasPartidasEnComunNoBloqueaLosEventos() throws Exception {
        Persona uno = registrar();
        Persona dos = registrar();
        Juego primera = crear(uno, true);
        Juego segunda = crear(dos, true);
        alta(uno, segunda).then().statusCode(201);
        alta(dos, primera).then().statusCode(201);
        CyclicBarrier barrera = new CyclicBarrier(2);
        try (var ejecutor = Executors.newFixedThreadPool(2)) {
            var resultados = ejecutor.invokeAll(List.of((Callable<Integer>) () -> {
                barrera.await();
                return given().port(puerto).auth().oauth2(uno.token())
                        .delete("/api/v1/cuentas/yo").statusCode();
            }, () -> {
                barrera.await();
                return given().port(puerto).auth().oauth2(dos.token())
                        .delete("/api/v1/cuentas/yo").statusCode();
            }));
            assertEquals(204, resultados.get(0).get());
            assertEquals(204, resultados.get(1).get());
        }
        for (Juego juego : List.of(primera, segunda)) {
            assertEquals(0, contar("inscripcion", "partida_id", juego.id()));
            assertEquals(List.of(1L, 2L, 3L, 4L, 5L, 6L), jdbc.queryForList(
                    "select secuencia from evento where partida_id = ? order by secuencia",
                    Long.class, juego.id()));
        }
    }

    private Persona registrar() {
        String alias = "i_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Response respuesta = given().port(puerto).contentType(ContentType.JSON)
                .body(Map.of("alias", alias, "email", alias + "@example.com",
                        "contrasena", "fantasma2026"))
                .post("/api/v1/cuentas").then().statusCode(201).extract().response();
        return new Persona(UUID.fromString(respuesta.path("cuenta.id")),
                respuesta.path("tokenAcceso"), alias);
    }

    private Juego crear(Persona master, boolean juega) {
        Instant inicio = Instant.now().plus(Duration.ofDays(1));
        Response respuesta = given().port(puerto).auth().oauth2(master.token())
                .contentType(ContentType.JSON).body(Map.of("nombre", "Partida Inscripcion",
                        "inicioEn", inicio.toString(), "finEn", inicio.plus(Duration.ofDays(7))
                                .toString(), "tipoMisiones", "BIBLIOTECA", "plazoDisputaHoras", 6,
                        "entradasTardias", false, "masterJuega", juega))
                .post("/api/v1/partidas").then().statusCode(201)
                .body("numInscritos", equalTo(juega ? 1 : 0)).extract().response();
        return new Juego(UUID.fromString(respuesta.path("id")), respuesta.path("codigoInvitacion"));
    }

    private Response peticion(Persona jugador, Object cuerpo) {
        return given().port(puerto).auth().oauth2(jugador.token()).contentType(ContentType.JSON)
                .body(cuerpo).post("/api/v1/inscripciones");
    }

    private Response alta(Persona jugador, Juego juego) {
        return peticion(jugador, Map.of("codigo", juego.codigo(), "aceptaNormas", true));
    }

    private Response lista(Persona persona, Juego juego) {
        return given().port(puerto).auth().oauth2(persona.token())
                .get("/api/v1/partidas/{id}/inscritos", juego.id());
    }

    private Response misPartidas(Persona persona) {
        return given().port(puerto).auth().oauth2(persona.token())
                .get("/api/v1/cuentas/yo/partidas");
    }

    private void borrar(Persona persona) {
        given().port(puerto).auth().oauth2(persona.token()).delete("/api/v1/cuentas/yo")
                .then().statusCode(204);
    }

    private void horario(Persona persona) {
        jdbc.update("insert into franja_horario (cuenta_id, dia, hora_inicio) values (?, 1, '08:00')",
                persona.id());
    }

    private int contar(String tabla, String columna, UUID id) {
        return jdbc.queryForObject("select count(*) from " + tabla + " where " + columna + " = ?",
                Integer.class, id);
    }

    private List<String> tipos(Juego juego) {
        return jdbc.queryForList("select tipo from evento where partida_id = ? order by secuencia",
                String.class, juego.id());
    }

    private List<Map<String, Object>> eventos(Juego juego) {
        return jdbc.queryForList("select * from evento where partida_id = ? order by secuencia",
                juego.id());
    }

    private record Persona(UUID id, String token, String alias) {
    }

    private record Juego(UUID id, String codigo) {
    }
}
