package es.unizar.ghostmark.backend.juego;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * PBI 11b (#273), tarea 11b.4 (#299): aceptación a través de las API reales.
 * La anulación se inicia al resolver una disputa, no invocando directamente su servicio.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AceptacionAnulacionTest {

    private static final String DISPUTAS = "/api/v1/partidas/{id}/disputas";
    private static final String FEED = "/api/v1/partidas/{id}/feed";
    private static final String CARTA = "/api/v1/partidas/{id}/yo/carta";

    @LocalServerPort
    int puerto;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoBean
    Clock reloj;

    private final GeneradorCodigoVida codigos = new GeneradorCodigoVida();

    @BeforeEach
    void fijarReloj() {
        when(reloj.instant()).thenReturn(
                Instant.now().truncatedTo(ChronoUnit.MINUTES).plusSeconds(30));
        when(reloj.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @Test
    void criterio1LaDecisionReviveLaVictimaRecalculaElMarcadorYRehaceElCiclo() {
        Mesa mesa = preparar(5);
        UUID cazador = mesa.inscritos().getFirst();
        UUID victima = objetivo(cazador);
        eliminar(mesa, cazador, victima, "FUERA");
        assertEquals("ELIMINADO", estado(victima));
        assertEquals(1, eliminaciones(cazador));
        UUID evento = eliminacionDe(mesa, victima);
        UUID misionAnterior = mision(cazador);
        UUID disputa = abrir(mesa, victima);

        decidir(mesa, disputa, "VICTIMA").then().statusCode(200)
                .body("estado", equalTo("RESUELTA"))
                .body("razon", equalTo("VICTIMA"));

        assertEquals("VIVO", estado(victima));
        assertEquals(0, eliminaciones(cazador));
        assertEquals(5, comprobarCiclo(mesa));
        assertNotEquals(victima, objetivo(cazador));
        assertNotEquals(cazador, objetivo(victima));
        assertNotEquals(misionAnterior, mision(cazador));
        assertNotNull(mision(victima));
        carta(mesa, victima).then().statusCode(200)
                .body("estado", equalTo("VIVO"))
                .body("mision", notNullValue());
        JsonPath anulacion = ultimaAnulacion(mesa);
        assertEquals(evento.toString(), anulacion.getString("eventoEliminacionId"));
        assertEquals(List.of(victima.toString()), anulacion.getList("revividos"));
        assertEquals(1, eventos(mesa, "ELIMINACION_ANULADA"));
    }

    @Test
    void criterio1UnaAnulacionEnCadenaNoRevocaLasEliminacionesDeOtros() {
        Mesa mesa = preparar(6);
        UUID cazador = mesa.inscritos().getFirst();
        UUID primera = objetivo(cazador);
        UUID segunda = objetivo(primera);
        UUID otroCazador = objetivo(segunda);
        UUID victimaDeOtro = objetivo(otroCazador);
        eliminar(mesa, cazador, primera, "FUERA");
        eliminar(mesa, cazador, segunda, "FUERA");
        eliminar(mesa, otroCazador, victimaDeOtro, "FUERA");
        assertEquals(2, eliminaciones(cazador));
        assertEquals(1, eliminaciones(otroCazador));
        UUID primeraEliminacion = eliminacionDe(mesa, primera);
        UUID segundaEliminacion = eliminacionDe(mesa, segunda);

        decidir(mesa, abrir(mesa, primera), "VICTIMA").then().statusCode(200);

        assertEquals("VIVO", estado(primera));
        assertEquals("VIVO", estado(segunda));
        assertEquals("ELIMINADO", estado(victimaDeOtro));
        assertEquals(0, eliminaciones(cazador));
        assertEquals(1, eliminaciones(otroCazador));
        assertEquals(5, comprobarCiclo(mesa));
        JsonPath anulacion = ultimaAnulacion(mesa);
        assertEquals(List.of(primeraEliminacion.toString(), segundaEliminacion.toString()),
                anulacion.getList("anuladas"));
        assertEquals(List.of(primera.toString(), segunda.toString()),
                anulacion.getList("revividos"));
        assertEquals(3, eventos(mesa, "JUGADOR_ELIMINADO"));
        assertEquals(1, eventos(mesa, "ELIMINACION_ANULADA"));
    }

    @Test
    void criterio1SiElCazadorEstaEliminadoNoReviveNiRecibeOtraMision() {
        Mesa mesa = preparar(5);
        UUID cazador = mesa.inscritos().getFirst();
        UUID victima = objetivo(cazador);
        UUID perseguidor = mesa.inscritos().stream()
                .filter(id -> cazador.equals(objetivo(id))).findFirst().orElseThrow();
        eliminar(mesa, cazador, victima, "FUERA");
        eliminar(mesa, perseguidor, cazador, "FUERA");
        UUID disputa = abrir(mesa, victima);
        UUID misionNoImplicado = mision(perseguidor);

        decidir(mesa, disputa, "VICTIMA").then().statusCode(200);

        assertEquals("VIVO", estado(victima));
        assertEquals("ELIMINADO", estado(cazador));
        assertEquals(0, eliminaciones(cazador));
        assertEquals(1, eliminaciones(perseguidor));
        assertEquals(4, comprobarCiclo(mesa));
        assertEquals(misionNoImplicado, mision(perseguidor));
        assertEquals(List.of(victima.toString()),
                ultimaAnulacion(mesa).getList("misiones.inscripcionId"));
        assertTrue(feed(persona(mesa, cazador), mesa).stream()
                .anyMatch(n -> "ANULACION".equals(n.get("tipo"))));
        carta(mesa, victima).then().statusCode(200)
                .body("estado", equalTo("VIVO"));
    }

    @Test
    void criterio2LasMisionesSeReasignanDeBibliotecaSinAprobacion() {
        Mesa mesa = preparar(5);
        UUID cazador = mesa.inscritos().getFirst();
        UUID victima = objetivo(cazador);
        eliminar(mesa, cazador, victima, "FUERA");
        UUID anterior = mision(cazador);
        UUID noImplicado = mesa.inscritos().stream()
                .filter(id -> !id.equals(cazador) && !id.equals(victima))
                .findFirst().orElseThrow();
        UUID conservada = mision(noImplicado);
        decidir(mesa, abrir(mesa, victima), "VICTIMA").then().statusCode(200);

        assertNotEquals(anterior, mision(cazador));
        assertNotNull(mision(victima));
        assertNotEquals(mision(cazador), mision(victima));
        assertEquals(conservada, mision(noImplicado));
        List<String> afectados = ultimaAnulacion(mesa)
                .getList("misiones.inscripcionId", String.class);
        assertEquals(Set.of(cazador.toString(), victima.toString()), Set.copyOf(afectados));
        for (UUID id : List.of(cazador, victima)) {
            assertTrue(jdbc.queryForObject("select exists(select 1 from mision"
                    + " where id = ? and activa = true and partida_id is null)",
                    Boolean.class, mision(id)));
        }
        carta(mesa, cazador).then().statusCode(200).body("mision", notNullValue());
        carta(mesa, victima).then().statusCode(200).body("mision", notNullValue());
    }

    @Test
    void criterio2ElFeedMuestraLaAnulacionATodosSinRevelarQuienCazo() {
        Mesa mesa = preparar(5);
        UUID cazador = mesa.inscritos().getFirst();
        UUID victima = objetivo(cazador);
        eliminar(mesa, cazador, victima, "FUERA");
        decidir(mesa, abrir(mesa, victima), "VICTIMA").then().statusCode(200);
        String alias = alias(victima);

        List<Persona> lectores = new ArrayList<>(mesa.jugadores());
        lectores.add(mesa.master());
        for (Persona lector : lectores) {
            List<Map<String, Object>> noticias = feed(lector, mesa);
            List<Map<String, Object>> anuladas = noticias.stream()
                    .filter(n -> "ANULACION".equals(n.get("tipo"))).toList();
            assertEquals(1, anuladas.size());
            Map<String, Object> noticia = anuladas.getFirst();
            assertEquals(alias, noticia.get("alias"));
            assertEquals(Set.of("tipo", "alias", "ocurridoEn"), noticia.keySet());
            assertNotNull(noticia.get("ocurridoEn"));
            assertEquals(1, noticias.stream()
                    .filter(n -> "ELIMINACION".equals(n.get("tipo"))).count());
        }
        given().port(puerto).auth().oauth2(registrar().token())
                .get(FEED, mesa.id()).then().statusCode(404);
    }

    @Test
    void criterio2ElFeedPublicaUnaAnulacionPorCadaRevividoDeLaCadena() {
        Mesa mesa = preparar(5);
        UUID cazador = mesa.inscritos().getFirst();
        UUID primera = objetivo(cazador);
        UUID segunda = objetivo(primera);
        eliminar(mesa, cazador, primera, "FUERA");
        eliminar(mesa, cazador, segunda, "FUERA");
        decidir(mesa, abrir(mesa, primera), "VICTIMA").then().statusCode(200);

        Set<String> esperados = Set.of(alias(primera), alias(segunda));
        for (Persona lector : List.of(mesa.master(), persona(mesa, cazador),
                persona(mesa, primera), persona(mesa, segunda))) {
            List<Map<String, Object>> anuladas = feed(lector, mesa).stream()
                    .filter(n -> "ANULACION".equals(n.get("tipo"))).toList();
            assertEquals(2, anuladas.size());
            assertEquals(esperados, Set.of((String) anuladas.get(0).get("alias"),
                    (String) anuladas.get(1).get("alias")));
        }
    }

    @Test
    void criterio1SiSeDaLaRazonAlCazadorNoHayRevocacion() {
        Mesa mesa = preparar(4);
        UUID cazador = mesa.inscritos().getFirst();
        UUID victima = objetivo(cazador);
        eliminar(mesa, cazador, victima, "FUERA");

        UUID disputa = abrir(mesa, victima);
        decidir(mesa, disputa, "CAZADOR").then().statusCode(200)
                .body("razon", equalTo("CAZADOR"));

        assertEquals("ELIMINADO", estado(victima));
        assertEquals(1, eliminaciones(cazador));
        assertEquals(3, comprobarCiclo(mesa));
        assertEquals(0, eventos(mesa, "ELIMINACION_ANULADA"));
        assertFalse(feed(mesa.master(), mesa).stream()
                .anyMatch(n -> "ANULACION".equals(n.get("tipo"))));
        decidir(mesa, disputa, "VICTIMA").then().statusCode(409)
                .body("codigo", equalTo("DISPUTA_NO_ABIERTA"));
    }

    @Test
    void criterio1ZonaDentroAnulaAutomaticamenteYActualizaLaCarta() {
        Mesa mesa = preparar(4);
        UUID cazador = mesa.inscritos().getFirst();
        UUID victima = objetivo(cazador);
        eliminar(mesa, cazador, victima, "DENTRO");

        abrirRespuesta(mesa, victima).then().statusCode(201)
                .body("estado", equalTo("RESUELTA"))
                .body("razon", equalTo("VICTIMA"))
                .body("resueltaPor", equalTo("ZONA"));

        assertEquals("VIVO", estado(victima));
        assertEquals(0, eliminaciones(cazador));
        assertEquals(4, comprobarCiclo(mesa));
        assertEquals(1, eventos(mesa, "ELIMINACION_ANULADA"));
        carta(mesa, victima).then().statusCode(200)
                .body("estado", equalTo("VIVO"))
                .body("mision", notNullValue());
        assertTrue(feed(mesa.master(), mesa).stream()
                .anyMatch(n -> "ANULACION".equals(n.get("tipo"))));
    }

    private Mesa preparar(int numJugadores) {
        Persona master = registrar();
        Instant inicio = Instant.now().plus(Duration.ofDays(1));
        Response creada = given().port(puerto).auth().oauth2(master.token())
                .contentType(ContentType.JSON).body(Map.of("nombre", "Aceptacion PBI 11b",
                        "inicioEn", inicio.toString(),
                        "finEn", inicio.plus(Duration.ofDays(7)).toString(),
                        "tipoMisiones", "BIBLIOTECA", "plazoDisputaHoras", 6,
                        "entradasTardias", false, "masterJuega", false))
                .post("/api/v1/partidas").then().statusCode(201).extract().response();
        UUID partidaId = UUID.fromString(creada.path("id"));
        String codigo = creada.path("codigoInvitacion");
        List<Persona> jugadores = new ArrayList<>();
        while (jugadores.size() < numJugadores) {
            Persona jugador = registrar();
            given().port(puerto).auth().oauth2(jugador.token()).contentType(ContentType.JSON)
                    .body(Map.of("codigo", codigo, "aceptaNormas", true))
                    .post("/api/v1/inscripciones").then().statusCode(201);
            jugadores.add(jugador);
        }
        given().port(puerto).auth().oauth2(master.token())
                .post("/api/v1/partidas/{id}/arranque", partidaId).then().statusCode(200);
        List<UUID> ids = new ArrayList<>();
        for (Persona jugador : jugadores) {
            ids.add(jdbc.queryForObject("select id from inscripcion"
                    + " where partida_id = ? and cuenta_id = ?", UUID.class,
                    partidaId, jugador.id()));
        }
        return new Mesa(partidaId, master, jugadores, ids);
    }

    private Persona registrar() {
        String alias = "ac11b_" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 14);
        Response creada = given().port(puerto).contentType(ContentType.JSON)
                .body(Map.of("alias", alias, "email", alias + "@example.com",
                        "contrasena", "fantasma2026"))
                .post("/api/v1/cuentas").then().statusCode(201).extract().response();
        return new Persona(UUID.fromString(creada.path("cuenta.id")),
                creada.path("tokenAcceso"));
    }

    private void eliminar(Mesa mesa, UUID cazador, UUID victima, String zona) {
        byte[] secreto = jdbc.queryForObject("select secreto_codigo from jugador_partida"
                + " where partida_id = ? and inscripcion_id = ?", byte[].class,
                mesa.id(), victima);
        String codigo = codigos.generar(secreto, reloj.instant()).codigo();
        given().port(puerto).auth().oauth2(persona(mesa, cazador).token())
                .contentType(ContentType.JSON)
                .body(Map.of("codigo", codigo, "resultadoZona", zona))
                .post("/api/v1/partidas/{id}/eliminaciones", mesa.id())
                .then().statusCode(200);
    }

    private Response abrirRespuesta(Mesa mesa, UUID victima) {
        return given().port(puerto).auth().oauth2(persona(mesa, victima).token())
                .contentType(ContentType.JSON)
                .body(Map.of("tipo", "ELIMINACION", "texto", "Impugno esta eliminacion."))
                .post(DISPUTAS, mesa.id());
    }

    private UUID abrir(Mesa mesa, UUID victima) {
        return UUID.fromString(abrirRespuesta(mesa, victima).then().statusCode(201)
                .body("estado", equalTo("ABIERTA"))
                .extract().path("id"));
    }

    private Response decidir(Mesa mesa, UUID disputa, String razon) {
        return given().port(puerto).auth().oauth2(mesa.master().token())
                .contentType(ContentType.JSON).body(Map.of("razon", razon))
                .post(DISPUTAS + "/{disputa}/decision", mesa.id(), disputa);
    }

    private Response carta(Mesa mesa, UUID jugador) {
        return given().port(puerto).auth().oauth2(persona(mesa, jugador).token())
                .get(CARTA, mesa.id());
    }

    private List<Map<String, Object>> feed(Persona lector, Mesa mesa) {
        return given().port(puerto).auth().oauth2(lector.token())
                .get(FEED, mesa.id()).then().statusCode(200)
                .extract().jsonPath().getList("");
    }

    private int comprobarCiclo(Mesa mesa) {
        List<UUID> vivos = jdbc.queryForList("select inscripcion_id from jugador_partida"
                + " where partida_id = ? and estado = 'VIVO'", UUID.class, mesa.id());
        Set<UUID> vistos = new HashSet<>();
        UUID actual = vivos.getFirst();
        for (int i = 0; i < vivos.size(); i++) {
            assertTrue(vistos.add(actual), "El ciclo repite un jugador antes de cerrar");
            actual = objetivo(actual);
            assertNotNull(actual);
        }
        assertEquals(vivos.getFirst(), actual);
        assertEquals(Set.copyOf(vivos), vistos);
        return vivos.size();
    }

    private UUID eliminacionDe(Mesa mesa, UUID victima) {
        return jdbc.queryForObject("select id from evento where partida_id = ?"
                + " and tipo = 'JUGADOR_ELIMINADO' and datos->>'victimaId' = ?",
                UUID.class, mesa.id(), victima.toString());
    }

    private JsonPath ultimaAnulacion(Mesa mesa) {
        return JsonPath.from(jdbc.queryForObject("select datos::text from evento"
                + " where partida_id = ? and tipo = 'ELIMINACION_ANULADA'"
                + " order by secuencia desc limit 1", String.class, mesa.id()));
    }

    private int eventos(Mesa mesa, String tipo) {
        return jdbc.queryForObject("select count(*) from evento"
                + " where partida_id = ? and tipo = ?", Integer.class, mesa.id(), tipo);
    }

    private UUID objetivo(UUID jugador) {
        return jdbc.queryForObject("select objetivo_id from jugador_partida"
                + " where inscripcion_id = ?", UUID.class, jugador);
    }

    private String estado(UUID jugador) {
        return jdbc.queryForObject("select estado from jugador_partida"
                + " where inscripcion_id = ?", String.class, jugador);
    }

    private int eliminaciones(UUID jugador) {
        return jdbc.queryForObject("select eliminaciones from jugador_partida"
                + " where inscripcion_id = ?", Integer.class, jugador);
    }

    private UUID mision(UUID jugador) {
        return jdbc.queryForObject("select mision_id from jugador_partida"
                + " where inscripcion_id = ?", UUID.class, jugador);
    }

    private String alias(UUID jugador) {
        return jdbc.queryForObject("select c.alias from inscripcion i"
                + " join cuenta c on c.id = i.cuenta_id where i.id = ?",
                String.class, jugador);
    }

    private Persona persona(Mesa mesa, UUID inscripcionId) {
        return mesa.jugadores().get(mesa.inscritos().indexOf(inscripcionId));
    }

    private record Persona(UUID id, String token) {
    }

    private record Mesa(UUID id, Persona master, List<Persona> jugadores, List<UUID> inscritos) {
    }
}
