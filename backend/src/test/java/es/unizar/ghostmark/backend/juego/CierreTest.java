package es.unizar.ghostmark.backend.juego;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import es.unizar.ghostmark.backend.TestcontainersConfiguration;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
class CierreTest {

    private static final String RUTA_PARTIDA = "/api/v1/partidas/{id}";
    private static final String RUTA_CIERRE = "/api/v1/partidas/{id}/cierre";
    private static final String RUTA_ARRANQUE = "/api/v1/partidas/{id}/arranque";
    private static final String RUTA_CARTA = "/api/v1/partidas/{id}/yo/carta";
    private static final String RUTA_INSCRITOS = "/api/v1/partidas/{id}/inscritos";
    private static final String RUTA_MIS_PARTIDAS = "/api/v1/cuentas/yo/partidas";

    @LocalServerPort
    int puerto;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TareaCierre tarea;

    @MockitoBean
    Clock reloj;

    private final GeneradorCodigoVida generador = new GeneradorCodigoVida();

    @BeforeEach
    void fijarReloj() {
        Instant ahora = Instant.now().truncatedTo(ChronoUnit.MINUTES).plusSeconds(30);
        when(reloj.instant()).thenReturn(ahora);
        when(reloj.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @Test
    void elMasterCierraConElGanadorPorEliminaciones() {
        Mesa mesa = conJugadores(4, false);
        arrancar(mesa).then().statusCode(200);
        Persona cazador = mesa.jugadores().get(0);
        UUID cazadorId = inscripcion(cazador, mesa.juego());
        eliminarDosVeces(cazador, mesa.juego(), cazadorId);
        String alias = alias(cazadorId);

        cerrar(mesa.master(), mesa.juego()).then().statusCode(200)
                .body("estado", equalTo("CERRADA"))
                .body("resultado.tipo", equalTo("GANADOR"))
                .body("resultado.motivo", equalTo("MASTER"))
                .body("resultado.aliasGanadores", contains(alias))
                .body("resultado.eliminaciones", equalTo(2))
                .body("resultado.cerradaEn", equalTo(reloj.instant().toString()));

        assertEquals("CERRADA", estado(mesa.juego()));
        assertEquals("GANADOR|MASTER|2", cierre(mesa.juego()));

        given().port(puerto).auth().oauth2(mesa.master().token())
                .get(RUTA_PARTIDA, mesa.juego().id()).then().statusCode(200)
                .body("estado", equalTo("CERRADA"))
                .body("resultado.tipo", equalTo("GANADOR"));
        given().port(puerto).auth().oauth2(mesa.jugadores().get(1).token())
                .get(RUTA_MIS_PARTIDAS).then().statusCode(200)
                .body("find { it.id == '" + mesa.juego().id() + "' }.resultado.tipo",
                        equalTo("GANADOR"));
    }

    @Test
    void elUltimoVivoCierraLaPartidaAlEliminar() {
        Mesa mesa = conJugadores(3, false);
        arrancar(mesa).then().statusCode(200);
        Persona cazador = mesa.jugadores().get(0);
        UUID cazadorId = inscripcion(cazador, mesa.juego());
        eliminarDosVeces(cazador, mesa.juego(), cazadorId);

        assertEquals("CERRADA", estado(mesa.juego()));
        assertEquals("SUPERVIVIENTE|ULTIMO_SUPERVIVIENTE|2", cierre(mesa.juego()));
        given().port(puerto).auth().oauth2(mesa.master().token())
                .get(RUTA_PARTIDA, mesa.juego().id()).then().statusCode(200)
                .body("resultado.tipo", equalTo("SUPERVIVIENTE"))
                .body("resultado.motivo", equalTo("ULTIMO_SUPERVIVIENTE"))
                .body("resultado.aliasGanadores", contains(alias(cazadorId)));
        given().port(puerto).auth().oauth2(cazador.token())
                .get(RUTA_CARTA, mesa.juego().id()).then().statusCode(409)
                .body("codigo", equalTo("PARTIDA_NO_EN_JUEGO"));
    }

    @Test
    void laFechaDeFinCierraConEmpateEnCabeza() {
        Mesa mesa = conJugadores(3, false);
        arrancar(mesa).then().statusCode(200);
        vencer(mesa.juego());

        tarea.revisar(reloj.instant());

        assertEquals("CERRADA", estado(mesa.juego()));
        assertEquals("EMPATE|FECHA_FIN|0", cierre(mesa.juego()));
        given().port(puerto).auth().oauth2(mesa.master().token())
                .get(RUTA_PARTIDA, mesa.juego().id()).then().statusCode(200)
                .body("resultado.tipo", equalTo("EMPATE"))
                .body("resultado.motivo", equalTo("FECHA_FIN"))
                .body("resultado.aliasGanadores", hasSize(3))
                .body("resultado.eliminaciones", equalTo(0))
                .body("resultado.cerradaEn", equalTo(reloj.instant().toString()));
    }

    @Test
    void laFechaDeFinCierraConElGanadorPorEliminaciones() {
        Mesa mesa = conJugadores(4, false);
        arrancar(mesa).then().statusCode(200);
        Persona cazador = mesa.jugadores().get(0);
        UUID cazadorId = inscripcion(cazador, mesa.juego());
        eliminarDosVeces(cazador, mesa.juego(), cazadorId);
        vencer(mesa.juego());

        tarea.revisar(Instant.now());

        assertEquals("CERRADA", estado(mesa.juego()));
        assertEquals("GANADOR|FECHA_FIN|2", cierre(mesa.juego()));
    }

    @Test
    void consultarAplicaElCierrePendiente() {
        Mesa mesa = conJugadores(3, false);
        arrancar(mesa).then().statusCode(200);
        vencer(mesa.juego());

        given().port(puerto).auth().oauth2(mesa.jugadores().get(0).token())
                .get(RUTA_MIS_PARTIDAS).then().statusCode(200)
                .body("find { it.id == '" + mesa.juego().id() + "' }.estado",
                        equalTo("CERRADA"))
                .body("find { it.id == '" + mesa.juego().id() + "' }.resultado.motivo",
                        equalTo("FECHA_FIN"));
        assertEquals("CERRADA", estado(mesa.juego()));
    }

    @Test
    void eliminarDespuesDeLaFechaDeFinResponde409() {
        Mesa mesa = conJugadores(3, false);
        arrancar(mesa).then().statusCode(200);
        Persona cazador = mesa.jugadores().get(0);
        UUID victima = objetivo(mesa.juego(), inscripcion(cazador, mesa.juego()));
        String codigo = codigoDe(mesa.juego(), victima, reloj.instant());
        vencer(mesa.juego());

        eliminar(cazador, mesa.juego(), codigo).then().statusCode(409)
                .body("codigo", equalTo("PARTIDA_NO_EN_JUEGO"));
        assertEquals("CERRADA", estado(mesa.juego()));
    }

    @Test
    void soloElMasterPuedeCerrar() {
        Mesa mesa = conJugadores(3, true);
        arrancar(mesa).then().statusCode(200);
        Persona jugador = mesa.jugadores().stream()
                .filter(persona -> !persona.id().equals(mesa.master().id()))
                .findFirst().orElseThrow();

        cerrar(jugador, mesa.juego()).then().statusCode(403)
                .body("codigo", equalTo("NO_ES_MASTER"));
        cerrar(registrar(), mesa.juego()).then().statusCode(404)
                .body("codigo", equalTo("NO_ENCONTRADA"));
        given().port(puerto).post(RUTA_CIERRE, mesa.juego().id()).then().statusCode(401)
                .body("codigo", equalTo("NO_AUTENTICADO"));
        cerrar(mesa.master(), new Juego(UUID.randomUUID(), "")).then().statusCode(404);

        cerrar(mesa.master(), mesa.juego()).then().statusCode(200);
        cerrar(mesa.master(), mesa.juego()).then().statusCode(409)
                .body("codigo", equalTo("PARTIDA_NO_EN_JUEGO"));
    }

    @Test
    void noSeCierraUnaPartidaQueNoEstaEnJuego() {
        Mesa mesa = conJugadores(3, false);

        cerrar(mesa.master(), mesa.juego()).then().statusCode(409)
                .body("codigo", equalTo("PARTIDA_NO_EN_JUEGO"));
        assertEquals("EN_INSCRIPCION", estado(mesa.juego()));
    }

    @Test
    void dosCierresSimultaneosSoloCierranUnaVez() throws Exception {
        Mesa mesa = conJugadores(3, true);
        arrancar(mesa).then().statusCode(200);
        CyclicBarrier barrera = new CyclicBarrier(2);
        Callable<Integer> operacion = () -> {
            barrera.await();
            return cerrar(mesa.master(), mesa.juego()).statusCode();
        };
        List<Integer> codigos = new ArrayList<>();

        try (var ejecutor = Executors.newFixedThreadPool(2)) {
            for (Future<Integer> resultado : ejecutor.invokeAll(List.of(operacion, operacion))) {
                codigos.add(resultado.get());
            }
        }

        assertEquals(List.of(200, 409), codigos.stream().sorted().toList());
        assertEquals(1, jdbc.queryForObject("select count(*) from evento where partida_id = ?"
                + " and tipo = 'PARTIDA_CERRADA'", Integer.class, mesa.juego().id()));
    }

    @Test
    void lasEliminacionesDeLosEliminadosNoCuentan() {
        Mesa mesa = conJugadores(3, false);
        arrancar(mesa).then().statusCode(200);
        Persona cazador = mesa.jugadores().get(0);
        UUID cazadorId = inscripcion(cazador, mesa.juego());
        eliminar(cazador, mesa.juego(),
                codigoDe(mesa.juego(), objetivo(mesa.juego(), cazadorId), reloj.instant()))
                .then().statusCode(200);
        jdbc.update("update jugador_partida set eliminaciones = 99"
                + " where partida_id = ? and estado = 'ELIMINADO'", mesa.juego().id());

        cerrar(mesa.master(), mesa.juego()).then().statusCode(200)
                .body("resultado.tipo", equalTo("GANADOR"))
                .body("resultado.eliminaciones", equalTo(1));
    }

    @Test
    void borrarLaCuentaDeUnVivoConLaPartidaCerradaConservaSuInscripcion() {
        // Con la partida cerrada, su cazador aún lo tiene como objetivo (R11-33).
        Mesa mesa = conJugadores(3, false);
        arrancar(mesa).then().statusCode(200);
        cerrar(mesa.master(), mesa.juego()).then().statusCode(200)
                .body("resultado.tipo", equalTo("EMPATE"));
        Persona vivo = mesa.jugadores().get(0);
        UUID vivoId = inscripcion(vivo, mesa.juego());

        given().port(puerto).auth().oauth2(vivo.token()).delete("/api/v1/cuentas/yo")
                .then().statusCode(204);

        assertEquals(1, jdbc.queryForObject("select count(*) from inscripcion"
                + " where id = ? and cuenta_id is null", Integer.class, vivoId));
        assertEquals(0, jdbc.queryForObject("select count(*) from evento where partida_id = ?"
                + " and tipo = 'INSCRIPCION_RETIRADA'", Integer.class, mesa.juego().id()));
        given().port(puerto).auth().oauth2(mesa.master().token())
                .get(RUTA_PARTIDA, mesa.juego().id()).then().statusCode(200)
                .body("resultado.tipo", equalTo("EMPATE"))
                .body("resultado.aliasGanadores", hasSize(3))
                .body("resultado.aliasGanadores.findAll { it == null }", hasSize(1));
    }

    @Test
    void elAliasDelGanadorBorradoSaleNulo() {
        Mesa mesa = conJugadores(3, false);
        arrancar(mesa).then().statusCode(200);
        Persona cazador = mesa.jugadores().get(0);
        UUID cazadorId = inscripcion(cazador, mesa.juego());
        eliminarDosVeces(cazador, mesa.juego(), cazadorId);

        given().port(puerto).auth().oauth2(cazador.token()).delete("/api/v1/cuentas/yo")
                .then().statusCode(204);

        given().port(puerto).auth().oauth2(mesa.master().token())
                .get(RUTA_PARTIDA, mesa.juego().id()).then().statusCode(200)
                .body("resultado.tipo", equalTo("SUPERVIVIENTE"))
                .body("resultado.aliasGanadores", contains(nullValue()));
    }

    @Test
    void losInscritosLlevanSuEstadoEnLaLista() {
        Mesa mesa = conJugadores(4, false);
        given().port(puerto).auth().oauth2(mesa.master().token())
                .get(RUTA_INSCRITOS, mesa.juego().id()).then().statusCode(200)
                .body("estado", everyItem(equalTo("VIVO")));
        arrancar(mesa).then().statusCode(200);
        Persona cazador = mesa.jugadores().get(0);
        UUID cazadorId = inscripcion(cazador, mesa.juego());
        UUID victimaId = objetivo(mesa.juego(), cazadorId);
        eliminar(cazador, mesa.juego(), codigoDe(mesa.juego(), victimaId, reloj.instant()))
                .then().statusCode(200);

        given().port(puerto).auth().oauth2(mesa.master().token())
                .get(RUTA_INSCRITOS, mesa.juego().id()).then().statusCode(200)
                .body("find { it.id == '" + victimaId + "' }.estado", equalTo("ELIMINADO"))
                .body("find { it.id == '" + cazadorId + "' }.estado", equalTo("VIVO"));
        given().port(puerto).auth().oauth2(registrar().token())
                .get(RUTA_INSCRITOS, mesa.juego().id()).then().statusCode(404);
    }

    private void eliminarDosVeces(Persona cazador, Juego juego, UUID cazadorId) {
        for (int vez = 0; vez < 2; vez++) {
            UUID victima = objetivo(juego, cazadorId);
            eliminar(cazador, juego, codigoDe(juego, victima, reloj.instant()))
                    .then().statusCode(200);
        }
    }

    private Mesa conJugadores(int cantidad, boolean masterJuega) {
        Persona master = registrar();
        Juego juego = crear(master, masterJuega);
        List<Persona> jugadores = new ArrayList<>();
        if (masterJuega) {
            jugadores.add(master);
        }
        while (jugadores.size() < cantidad) {
            Persona jugador = registrar();
            apuntar(jugador, juego);
            jugadores.add(jugador);
        }
        return new Mesa(master, juego, jugadores);
    }

    private Juego crear(Persona master, boolean masterJuega) {
        Instant inicio = Instant.now().plus(Duration.ofDays(1));
        Response respuesta = given().port(puerto).auth().oauth2(master.token())
                .contentType(ContentType.JSON).body(Map.of("nombre", "Partida Cierre",
                        "inicioEn", inicio.toString(),
                        "finEn", inicio.plus(Duration.ofDays(7)).toString(),
                        "tipoMisiones", "BIBLIOTECA", "plazoDisputaHoras", 6,
                        "entradasTardias", false, "masterJuega", masterJuega))
                .post("/api/v1/partidas").then().statusCode(201).extract().response();
        return new Juego(UUID.fromString(respuesta.path("id")),
                respuesta.path("codigoInvitacion"));
    }

    private void apuntar(Persona jugador, Juego juego) {
        given().port(puerto).auth().oauth2(jugador.token()).contentType(ContentType.JSON)
                .body(Map.of("codigo", juego.codigo(), "aceptaNormas", true))
                .post("/api/v1/inscripciones").then().statusCode(201);
    }

    private Response arrancar(Mesa mesa) {
        return given().port(puerto).auth().oauth2(mesa.master().token())
                .post(RUTA_ARRANQUE, mesa.juego().id());
    }

    private Response cerrar(Persona persona, Juego juego) {
        return given().port(puerto).auth().oauth2(persona.token())
                .post(RUTA_CIERRE, juego.id());
    }

    private Response eliminar(Persona persona, Juego juego, String codigo) {
        return given().port(puerto).auth().oauth2(persona.token()).contentType(ContentType.JSON)
                .body(Map.of("codigo", codigo)).post("/api/v1/partidas/{id}/eliminaciones",
                        juego.id());
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

    private String codigoDe(Juego juego, UUID inscripcionId, Instant cuando) {
        byte[] secreto = jdbc.queryForObject("select secreto_codigo from jugador_partida"
                + " where partida_id = ? and inscripcion_id = ?", byte[].class,
                juego.id(), inscripcionId);
        return generador.generar(secreto, cuando).codigo();
    }

    private String estado(Juego juego) {
        return jdbc.queryForObject("select estado from partida where id = ?", String.class,
                juego.id());
    }

    private String cierre(Juego juego) {
        return jdbc.queryForObject("select datos->>'resultado' || '|' || (datos->>'motivo')"
                + " || '|' || (datos->>'eliminaciones') from evento where partida_id = ?"
                + " and tipo = 'PARTIDA_CERRADA' order by secuencia desc limit 1", String.class,
                juego.id());
    }

    private void vencer(Juego juego) {
        jdbc.update("update partida set inicio_en = now() - interval '2 days',"
                + " fin_en = now() - interval '1 minute' where id = ?", juego.id());
    }

    private record Persona(UUID id, String token) {
    }

    private record Juego(UUID id, String codigo) {
    }

    private record Mesa(Persona master, Juego juego, List<Persona> jugadores) {
    }
}
