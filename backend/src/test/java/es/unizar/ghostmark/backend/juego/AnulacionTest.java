package es.unizar.ghostmark.backend.juego;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import es.unizar.ghostmark.backend.TestcontainersConfiguration;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AnulacionTest {

    private static final String DISPUTAS = "/api/v1/partidas/{id}/disputas";

    @LocalServerPort
    int puerto;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ServicioAnulacion anulacion;

    @Autowired
    PlatformTransactionManager transacciones;

    @MockitoBean
    Clock reloj;

    private final GeneradorCodigoVida generador = new GeneradorCodigoVida();

    @BeforeEach
    void fijarReloj() {
        when(reloj.instant()).thenReturn(
                Instant.now().truncatedTo(ChronoUnit.MINUTES).plusSeconds(30));
        when(reloj.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @Test
    void laVictimaRevivePierdeElCazadorLaEliminacionYSeRehaceElCiclo() {
        Mesa mesa = preparar(5);
        UUID cazador = mesa.inscritos().getFirst();
        UUID victima = objetivo(cazador);
        eliminar(mesa, cazador, victima);
        UUID otro = mesa.inscritos().stream()
                .filter(id -> "VIVO".equals(estado(id)) && !id.equals(cazador))
                .findFirst().orElseThrow();
        jdbc.update("update jugador_partida set intentos_fallidos = 2, bloqueado_hasta = ?"
                + " where inscripcion_id = ?", OffsetDateTime.now(ZoneOffset.UTC).plusHours(1),
                otro);
        UUID evento = eliminacionDe(mesa, victima);

        anular(mesa, evento);

        assertEquals("VIVO", estado(victima));
        assertNull(jdbc.queryForObject("select eliminado_en from jugador_partida"
                + " where inscripcion_id = ?", OffsetDateTime.class, victima));
        assertEquals(0, eliminaciones(cazador));
        assertNotEquals(victima, objetivo(cazador));
        assertNotEquals(cazador, objetivo(victima));
        comprobarCiclo(mesa, 5);
        for (UUID id : mesa.inscritos()) {
            assertEquals(0, jdbc.queryForObject("select intentos_fallidos from jugador_partida"
                    + " where inscripcion_id = ?", Integer.class, id));
            assertNull(jdbc.queryForObject("select bloqueado_hasta from jugador_partida"
                    + " where inscripcion_id = ?", OffsetDateTime.class, id));
        }

        JsonPath datos = ultimaAnulacion(mesa);
        assertEquals(evento.toString(), datos.getString("eventoEliminacionId"));
        assertEquals(List.of(evento.toString()), datos.getList("anuladas"));
        assertEquals(List.of(victima.toString()), datos.getList("revividos"));
        List<String> orden = datos.getList("orden");
        assertEquals(5, orden.size());
        for (int i = 0; i < orden.size(); i++) {
            assertEquals(UUID.fromString(orden.get((i + 1) % orden.size())),
                    objetivo(UUID.fromString(orden.get(i))));
        }
        assertEquals(Set.of(cazador.toString(), victima.toString()),
                Set.copyOf(datos.getList("misiones.inscripcionId", String.class)));

        anular(mesa, evento);
        assertEquals(1, eventos(mesa, "ELIMINACION_ANULADA"));
        assertEquals(0, eliminaciones(cazador));
    }

    @Test
    void seAnulanLasEliminacionesPosterioresDelMismoCazadorYNoLasDeOtros() {
        Mesa mesa = preparar(5);
        UUID cazador = mesa.inscritos().getFirst();
        UUID primera = objetivo(cazador);
        UUID segunda = objetivo(primera);
        UUID otro = objetivo(segunda);
        UUID victimaDelOtro = objetivo(otro);
        eliminar(mesa, cazador, primera);
        eliminar(mesa, cazador, segunda);
        eliminar(mesa, otro, victimaDelOtro);
        assertEquals(2, eliminaciones(cazador));

        anular(mesa, eliminacionDe(mesa, primera));

        assertEquals("VIVO", estado(primera));
        assertEquals("VIVO", estado(segunda));
        assertEquals("ELIMINADO", estado(victimaDelOtro));
        assertEquals(0, eliminaciones(cazador));
        assertEquals(1, eliminaciones(otro));
        comprobarCiclo(mesa, 4);
        JsonPath datos = ultimaAnulacion(mesa);
        assertEquals(List.of(eliminacionDe(mesa, primera).toString(),
                eliminacionDe(mesa, segunda).toString()), datos.getList("anuladas"));
        assertEquals(List.of(primera.toString(), segunda.toString()),
                datos.getList("revividos"));
    }

    @Test
    void lasDisputasDeLaCadenaYLasNegativasSinObjetoSeCierran() {
        Mesa mesa = preparar(5);
        UUID cazador = mesa.inscritos().getFirst();
        UUID primera = objetivo(cazador);
        UUID segunda = objetivo(primera);
        UUID negador = objetivo(segunda);
        UUID negado = objetivo(negador);
        eliminar(mesa, cazador, primera);
        eliminar(mesa, cazador, segunda);
        UUID enCadena = abrir(mesa, segunda, "ELIMINACION");
        UUID negativa = abrir(mesa, negador, "NEGATIVA");

        anular(mesa, eliminacionDe(mesa, primera));

        assertEquals("SIN_OBJETO", estadoDisputa(enCadena));
        assertEquals(negado.equals(objetivo(negador)) ? "ABIERTA" : "SIN_OBJETO",
                estadoDisputa(negativa));
        int sinObjeto = negado.equals(objetivo(negador)) ? 1 : 2;
        assertEquals(sinObjeto, jdbc.queryForObject("select count(*) from evento"
                + " where partida_id = ? and tipo = 'DISPUTA_RESUELTA'"
                + " and datos->>'estado' = 'SIN_OBJETO'", Integer.class, mesa.id()));
        assertFalse(jdbc.queryForObject("select exists(select 1 from disputa"
                + " where cazador_id = ? and estado = 'ABIERTA')", Boolean.class, cazador));
    }

    @Test
    void elArbitroDaLaRazonALaVictimaYEstaRevive() {
        Mesa mesa = preparar(4);
        UUID cazador = mesa.inscritos().getFirst();
        UUID victima = objetivo(cazador);
        eliminar(mesa, cazador, victima);
        UUID disputa = abrir(mesa, victima, "ELIMINACION");

        given().port(puerto).auth().oauth2(mesa.master().token())
                .contentType(ContentType.JSON).body(Map.of("razon", "VICTIMA"))
                .post(DISPUTAS + "/{disputa}/decision", mesa.id(), disputa)
                .then().statusCode(200).body("estado", equalTo("RESUELTA"));

        assertEquals("VIVO", estado(victima));
        assertEquals(0, eliminaciones(cazador));
        assertEquals(1, eventos(mesa, "ELIMINACION_ANULADA"));
        comprobarCiclo(mesa, 4);
        carta(mesa, cazador).then().statusCode(200)
                .body("bloqueadoPorDisputa", equalTo(false));
    }

    @Test
    void conLaZonaDentroLaVictimaReviveAlAbrirLaDisputa() {
        Mesa mesa = preparar(4);
        UUID cazador = mesa.inscritos().getFirst();
        UUID victima = objetivo(cazador);
        eliminarDesde(mesa, cazador, victima, "DENTRO");

        abrir(mesa, victima, "ELIMINACION");

        assertEquals("VIVO", estado(victima));
        assertEquals(1, eventos(mesa, "ELIMINACION_ANULADA"));
        comprobarCiclo(mesa, 4);
        carta(mesa, victima).then().statusCode(200).body("estado", equalTo("VIVO"));
    }

    private void anular(Mesa mesa, UUID evento) {
        new TransactionTemplate(transacciones).executeWithoutResult(estado ->
                anulacion.anular(mesa.id(), evento, reloj.instant()));
    }

    private void comprobarCiclo(Mesa mesa, int vivos) {
        List<UUID> ids = jdbc.queryForList("select inscripcion_id from jugador_partida"
                + " where partida_id = ? and estado = 'VIVO'", UUID.class, mesa.id());
        assertEquals(vivos, ids.size());
        Set<UUID> vistos = new HashSet<>();
        UUID actual = ids.getFirst();
        for (int i = 0; i < vivos; i++) {
            vistos.add(actual);
            actual = objetivo(actual);
        }
        assertEquals(ids.getFirst(), actual);
        assertEquals(Set.copyOf(ids), vistos);
    }

    private Mesa preparar(int numJugadores) {
        Persona master = registrar();
        Instant inicio = Instant.now().plus(Duration.ofDays(1));
        Response creada = given().port(puerto).auth().oauth2(master.token())
                .contentType(ContentType.JSON).body(Map.of("nombre", "Anular", "inicioEn",
                        inicio.toString(), "finEn", inicio.plus(Duration.ofDays(7)).toString(),
                        "tipoMisiones", "BIBLIOTECA", "plazoDisputaHoras", 6,
                        "entradasTardias", false, "masterJuega", false))
                .post("/api/v1/partidas").then().statusCode(201).extract().response();
        UUID id = UUID.fromString(creada.path("id"));
        List<Persona> jugadores = new ArrayList<>();
        while (jugadores.size() < numJugadores) {
            Persona jugador = registrar();
            given().port(puerto).auth().oauth2(jugador.token())
                    .contentType(ContentType.JSON)
                    .body(Map.of("codigo", creada.<String>path("codigoInvitacion"),
                            "aceptaNormas", true))
                    .post("/api/v1/inscripciones").then().statusCode(201);
            jugadores.add(jugador);
        }
        given().port(puerto).auth().oauth2(master.token())
                .post("/api/v1/partidas/{id}/arranque", id).then().statusCode(200);
        List<UUID> inscritos = new ArrayList<>();
        for (Persona jugador : jugadores) {
            inscritos.add(jdbc.queryForObject("select id from inscripcion"
                    + " where partida_id = ? and cuenta_id = ?", UUID.class, id, jugador.id()));
        }
        return new Mesa(id, master, jugadores, inscritos);
    }

    private Persona registrar() {
        String alias = "an_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Response r = given().port(puerto).contentType(ContentType.JSON)
                .body(Map.of("alias", alias, "email", alias + "@example.com",
                        "contrasena", "fantasma2026"))
                .post("/api/v1/cuentas").then().statusCode(201).extract().response();
        return new Persona(UUID.fromString(r.path("cuenta.id")), r.path("tokenAcceso"));
    }

    private Persona persona(Mesa mesa, UUID inscripcionId) {
        return mesa.jugadores().get(mesa.inscritos().indexOf(inscripcionId));
    }

    private void eliminar(Mesa mesa, UUID cazador, UUID victima) {
        eliminarDesde(mesa, cazador, victima, "FUERA");
    }

    private void eliminarDesde(Mesa mesa, UUID cazador, UUID victima, String zona) {
        byte[] secreto = jdbc.queryForObject("select secreto_codigo from jugador_partida"
                + " where inscripcion_id = ?", byte[].class, victima);
        given().port(puerto).auth().oauth2(persona(mesa, cazador).token())
                .contentType(ContentType.JSON)
                .body(Map.of("codigo", generador.generar(secreto, reloj.instant()).codigo(),
                        "resultadoZona", zona))
                .post("/api/v1/partidas/{id}/eliminaciones", mesa.id())
                .then().statusCode(200);
    }

    private UUID abrir(Mesa mesa, UUID quien, String tipo) {
        return UUID.fromString(given().port(puerto).auth().oauth2(persona(mesa, quien).token())
                .contentType(ContentType.JSON).body(Map.of("tipo", tipo, "texto", "No fue así."))
                .post(DISPUTAS, mesa.id()).then().statusCode(201).extract().path("id"));
    }

    private Response carta(Mesa mesa, UUID inscripcionId) {
        return given().port(puerto).auth().oauth2(persona(mesa, inscripcionId).token())
                .get("/api/v1/partidas/{id}/yo/carta", mesa.id());
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

    private String estadoDisputa(UUID disputa) {
        return jdbc.queryForObject("select estado from disputa where id = ?",
                String.class, disputa);
    }

    private int eventos(Mesa mesa, String tipo) {
        return jdbc.queryForObject("select count(*) from evento"
                + " where partida_id = ? and tipo = ?", Integer.class, mesa.id(), tipo);
    }

    private record Persona(UUID id, String token) {
    }

    private record Mesa(UUID id, Persona master, List<Persona> jugadores, List<UUID> inscritos) {
    }
}
