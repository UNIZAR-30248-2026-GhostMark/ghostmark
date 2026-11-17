package es.unizar.ghostmark.backend.disputa;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import es.unizar.ghostmark.backend.TestcontainersConfiguration;
import es.unizar.ghostmark.backend.juego.GeneradorCodigoVida;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Pruebas de la 11a.3 (#292): las disputas se abren por la API de la 11a.2. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ArbitrajeTest {

    private static final String RUTA = "/api/v1/partidas/{id}/disputas";

    @LocalServerPort
    int puerto;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TareaArbitraje tarea;

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
    void elMasterNoImplicadoDecideUnaNegativaYNoPuedeDecidirDosVeces() {
        Mesa mesa = preparar(false, 3);
        Persona cazador = mesa.jugadores().getFirst();
        UUID cazadorId = inscrito(mesa, cazador);
        UUID victima = objetivo(cazadorId);
        UUID disputa = abrirNegativa(cazador, mesa);
        assertNull(arbitroActual(disputa));
        assertEquals(1, contador(mesa.master(), mesa));

        decidir(mesa.master(), mesa, disputa, "VICTIMA").then().statusCode(200)
                .body("estado", equalTo("RESUELTA"))
                .body("razon", equalTo("VICTIMA"))
                .body("resueltaPor", equalTo("MASTER"));
        assertEquals(1, eventos(mesa, "DISPUTA_RESUELTA"));
        assertEquals(0, contador(mesa.master(), mesa));
        assertEquals("VIVO", estado(victima));
        assertEquals(victima, objetivo(cazadorId));
        decidir(mesa.master(), mesa, disputa, "CAZADOR").then().statusCode(409)
                .body("codigo", equalTo("DISPUTA_NO_ABIERTA"));
        assertEquals(1, eventos(mesa, "DISPUTA_RESUELTA"));
    }

    @Test
    void elImplicadoRecibeNoEsArbitroYLaCuentaAjenaNoVeLaDisputa() {
        Mesa mesa = preparar(false, 3);
        Persona cazador = mesa.jugadores().getFirst();
        UUID disputa = abrirNegativa(cazador, mesa);
        Persona victima = persona(mesa, objetivo(inscrito(mesa, cazador)));

        decidir(cazador, mesa, disputa, "CAZADOR").then().statusCode(403)
                .body("codigo", equalTo("NO_ES_ARBITRO"));
        decidir(victima, mesa, disputa, "VICTIMA").then().statusCode(403)
                .body("codigo", equalTo("NO_ES_ARBITRO"));
        decidir(registrar(), mesa, disputa, "VICTIMA").then().statusCode(404);
        decidir(mesa.master(), mesa, disputa, "OTRA").then().statusCode(400);
        assertEquals("ABIERTA", estadoDisputa(disputa));
        assertEquals(0, eventos(mesa, "DISPUTA_RESUELTA"));
    }

    @Test
    void siElMasterEsElCazadorSeSorteaUnJugadorNoImplicadoAlAbrir() {
        Mesa mesa = preparar(true, 4);
        UUID cazador = inscrito(mesa, mesa.master());
        UUID victima = objetivo(cazador);
        UUID disputa = abrirNegativa(mesa.master(), mesa);

        UUID elegido = arbitroActual(disputa);
        assertNotNull(elegido);
        assertNotEquals(cazador, elegido);
        assertNotEquals(victima, elegido);
        assertEquals("MASTER_IMPLICADO", ultimoSorteo(mesa, "motivo"));
        assertEquals(0, contador(mesa.master(), mesa));
        decidir(mesa.master(), mesa, disputa, "VICTIMA").then().statusCode(403)
                .body("codigo", equalTo("NO_ES_ARBITRO"));
        Persona sorteado = persona(mesa, elegido);
        assertEquals(1, contador(sorteado, mesa));
        decidir(sorteado, mesa, disputa, "VICTIMA").then().statusCode(200)
                .body("resueltaPor", equalTo("SORTEADO"));
    }

    @Test
    void siElMasterEsLaVictimaSeSorteaUnJugadorNoImplicadoAlAbrir() {
        Mesa mesa = preparar(true, 4);
        UUID masterId = inscrito(mesa, mesa.master());
        UUID cazadorId = cazadorDe(masterId);
        eliminar(persona(mesa, cazadorId), mesa, masterId, "FUERA").then().statusCode(200);

        UUID disputa = abrir(mesa.master(), mesa, "ELIMINACION", "No me pidió nada.")
                .then().statusCode(201).extract().<String>path("id").transform(UUID::fromString);

        UUID elegido = arbitroActual(disputa);
        assertNotNull(elegido);
        assertNotEquals(masterId, elegido);
        assertNotEquals(cazadorId, elegido);
        assertEquals(1, eventos(mesa, "DISPUTA_ARBITRO_SORTEADO"));
    }

    @Test
    void alVencerElPlazoSeSorteaOtroYSeEvitanLosArbitrosAnteriores() {
        Mesa mesa = preparar(true, 5);
        UUID disputa = abrirNegativa(mesa.master(), mesa);
        UUID anterior = arbitroActual(disputa);
        Instant vencimiento = reloj.instant().plus(Duration.ofHours(6));
        tarea.revisar(vencimiento.minusSeconds(1));
        assertEquals(anterior, arbitroActual(disputa));

        tarea.revisar(vencimiento);
        UUID nuevo = arbitroActual(disputa);
        assertNotEquals(anterior, nuevo);
        assertEquals(2, eventos(mesa, "DISPUTA_ARBITRO_SORTEADO"));
        assertEquals("PLAZO_VENCIDO", ultimoSorteo(mesa, "motivo"));
        assertEquals(vencimiento.plus(Duration.ofHours(6)), plazo(disputa));
    }

    @Test
    void ganarUnaNegativaEliminaPorElMismoCaminoQueElCodigo() {
        Mesa mesa = preparar(false, 3);
        Persona cazador = mesa.jugadores().getFirst();
        UUID cazadorId = inscrito(mesa, cazador);
        UUID victimaId = objetivo(cazadorId);
        UUID siguiente = objetivo(victimaId);
        Persona victima = persona(mesa, victimaId);
        String misionDelCazador = misionTexto(cazadorId);
        UUID disputa = abrirNegativa(cazador, mesa);
        // La víctima también tiene abierta una negativa: al eliminarla se queda sin objeto
        UUID suya = abrirNegativa(victima, mesa);

        decidir(mesa.master(), mesa, disputa, "CAZADOR").then().statusCode(200)
                .body("estado", equalTo("RESUELTA"))
                .body("mision", equalTo(misionDelCazador));

        assertEquals("ELIMINADO", estado(victimaId));
        assertEquals(siguiente, objetivo(cazadorId));
        assertEquals(1, jdbc.queryForObject("select eliminaciones from jugador_partida"
                + " where inscripcion_id = ?", Integer.class, cazadorId));
        assertEquals("DISPUTA", ultimaEliminacion(mesa, "origen"));
        assertNull(ultimaEliminacion(mesa, "resultadoZona"));
        assertEquals("SIN_OBJETO", estadoDisputa(suya));
        // Una eliminación por disputa no se puede disputar (R12-20)
        given().port(puerto).auth().oauth2(victima.token())
                .get("/api/v1/partidas/{id}/yo/carta", mesa.id()).then().statusCode(200)
                .body("disputarHasta", nullValue());
        abrir(victima, mesa, "ELIMINACION", "No fue así.").then().statusCode(409)
                .body("codigo", equalTo("ELIMINACION_NO_DISPUTABLE"));
    }

    @Test
    void unaZonaDentroResuelveSolaAlAbrirYSoloElArbitroVeLaZona() {
        Mesa mesa = preparar(false, 3);
        Persona cazador = mesa.jugadores().getFirst();
        UUID cazadorId = inscrito(mesa, cazador);
        UUID victimaId = objetivo(cazadorId);
        Persona victima = persona(mesa, victimaId);
        eliminar(cazador, mesa, victimaId, "DENTRO").then().statusCode(200);
        assertEquals("CODIGO", ultimaEliminacion(mesa, "origen"));

        String disputa = abrir(victima, mesa, "ELIMINACION", "Estaba en la cafetería.")
                .then().statusCode(201)
                .body("estado", equalTo("RESUELTA"))
                .body("razon", equalTo("VICTIMA"))
                .body("resueltaPor", equalTo("ZONA"))
                .body("resultadoZona", nullValue())
                .extract().path("id");

        assertEquals(1, eventos(mesa, "DISPUTA_RESUELTA"));
        assertEquals(0, eventos(mesa, "DISPUTA_ARBITRO_SORTEADO"));
        obtener(mesa.master(), mesa, disputa).then().statusCode(200)
                .body("resultadoZona", equalTo("DENTRO"));
        obtener(cazador, mesa, disputa).then().statusCode(200)
                .body("resultadoZona", nullValue());
    }

    @Test
    void conZonaFueraLaDisputaSigueAbiertaParaElArbitro() {
        Mesa mesa = preparar(false, 3);
        Persona cazador = mesa.jugadores().getFirst();
        UUID victimaId = objetivo(inscrito(mesa, cazador));
        eliminar(cazador, mesa, victimaId, "FUERA").then().statusCode(200);

        abrir(persona(mesa, victimaId), mesa, "ELIMINACION", "No fue limpio.")
                .then().statusCode(201).body("estado", equalTo("ABIERTA"));
        assertEquals(1, contador(mesa.master(), mesa));
    }

    private Mesa preparar(boolean masterJuega, int numJugadores) {
        Persona master = registrar();
        Instant inicio = Instant.now().plus(Duration.ofDays(1));
        Response creada = given().port(puerto).auth().oauth2(master.token())
                .contentType(ContentType.JSON).body(Map.of("nombre", "Disputas", "inicioEn",
                        inicio.toString(), "finEn", inicio.plus(Duration.ofDays(7)).toString(),
                        "tipoMisiones", "BIBLIOTECA", "plazoDisputaHoras", 6,
                        "entradasTardias", false, "masterJuega", masterJuega))
                .post("/api/v1/partidas").then().statusCode(201).extract().response();
        UUID id = UUID.fromString(creada.path("id"));
        String codigo = creada.path("codigoInvitacion");
        List<Persona> jugadores = new ArrayList<>();
        if (masterJuega) {
            jugadores.add(master);
        }
        while (jugadores.size() < numJugadores) {
            Persona jugador = registrar();
            given().port(puerto).auth().oauth2(jugador.token())
                    .contentType(ContentType.JSON)
                    .body(Map.of("codigo", codigo, "aceptaNormas", true))
                    .post("/api/v1/inscripciones").then().statusCode(201);
            jugadores.add(jugador);
        }
        given().port(puerto).auth().oauth2(master.token())
                .post("/api/v1/partidas/{id}/arranque", id).then().statusCode(200);
        return new Mesa(id, master, jugadores);
    }

    private Persona registrar() {
        String alias = "d_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Response r = given().port(puerto).contentType(ContentType.JSON)
                .body(Map.of("alias", alias, "email", alias + "@example.com",
                        "contrasena", "fantasma2026"))
                .post("/api/v1/cuentas").then().statusCode(201).extract().response();
        return new Persona(UUID.fromString(r.path("cuenta.id")), r.path("tokenAcceso"));
    }

    private Response abrir(Persona persona, Mesa mesa, String tipo, String texto) {
        return given().port(puerto).auth().oauth2(persona.token()).contentType(ContentType.JSON)
                .body(Map.of("tipo", tipo, "texto", texto)).post(RUTA, mesa.id());
    }

    private UUID abrirNegativa(Persona cazador, Mesa mesa) {
        return UUID.fromString(abrir(cazador, mesa, "NEGATIVA", "No me enseña el código.")
                .then().statusCode(201).extract().path("id"));
    }

    private Response obtener(Persona persona, Mesa mesa, String disputa) {
        return given().port(puerto).auth().oauth2(persona.token())
                .get(RUTA + "/{disputa}", mesa.id(), disputa);
    }

    private Response decidir(Persona persona, Mesa mesa, UUID disputa, String razon) {
        return given().port(puerto).auth().oauth2(persona.token())
                .contentType(ContentType.JSON).body(Map.of("razon", razon))
                .post(RUTA + "/{disputa}/decision", mesa.id(), disputa);
    }

    private Response eliminar(Persona cazador, Mesa mesa, UUID victima, String zona) {
        byte[] secreto = jdbc.queryForObject("select secreto_codigo from jugador_partida"
                + " where inscripcion_id = ?", byte[].class, victima);
        return given().port(puerto).auth().oauth2(cazador.token()).contentType(ContentType.JSON)
                .body(Map.of("codigo", generador.generar(secreto, reloj.instant()).codigo(),
                        "resultadoZona", zona))
                .post("/api/v1/partidas/{id}/eliminaciones", mesa.id());
    }

    private int contador(Persona persona, Mesa mesa) {
        return given().port(puerto).auth().oauth2(persona.token())
                .get("/api/v1/partidas/{id}", mesa.id())
                .then().statusCode(200).extract().path("disputasPorArbitrar");
    }

    private UUID inscrito(Mesa mesa, Persona persona) {
        return jdbc.queryForObject("select id from inscripcion"
                + " where partida_id = ? and cuenta_id = ?",
                UUID.class, mesa.id(), persona.id());
    }

    private Persona persona(Mesa mesa, UUID inscripcionId) {
        return mesa.jugadores().stream()
                .filter(jugador -> inscripcionId.equals(inscrito(mesa, jugador)))
                .findFirst().orElseThrow();
    }

    private UUID objetivo(UUID jugador) {
        return jdbc.queryForObject("select objetivo_id from jugador_partida"
                + " where inscripcion_id = ?", UUID.class, jugador);
    }

    private UUID cazadorDe(UUID victima) {
        return jdbc.queryForObject("select inscripcion_id from jugador_partida"
                + " where objetivo_id = ?", UUID.class, victima);
    }

    private String misionTexto(UUID inscripcionId) {
        return jdbc.queryForObject("select m.texto from jugador_partida jp"
                + " join mision m on m.id = jp.mision_id where jp.inscripcion_id = ?",
                String.class, inscripcionId);
    }

    private String estado(UUID jugador) {
        return jdbc.queryForObject("select estado from jugador_partida"
                + " where inscripcion_id = ?", String.class, jugador);
    }

    private String estadoDisputa(UUID disputa) {
        return jdbc.queryForObject("select estado from disputa where id = ?",
                String.class, disputa);
    }

    private UUID arbitroActual(UUID disputa) {
        return jdbc.queryForObject("select arbitro_id from disputa where id = ?",
                UUID.class, disputa);
    }

    private Instant plazo(UUID disputa) {
        return jdbc.queryForObject("select plazo_hasta from disputa where id = ?",
                OffsetDateTime.class, disputa).toInstant();
    }

    private int eventos(Mesa mesa, String tipo) {
        return jdbc.queryForObject("select count(*) from evento"
                + " where partida_id = ? and tipo = ?", Integer.class, mesa.id(), tipo);
    }

    private String ultimoSorteo(Mesa mesa, String campo) {
        return jdbc.queryForObject("select datos->>? from evento where partida_id = ?"
                + " and tipo = 'DISPUTA_ARBITRO_SORTEADO' order by secuencia desc limit 1",
                String.class, campo, mesa.id());
    }

    private String ultimaEliminacion(Mesa mesa, String campo) {
        return jdbc.queryForObject("select datos->>? from evento where partida_id = ?"
                + " and tipo = 'JUGADOR_ELIMINADO' order by secuencia desc limit 1",
                String.class, campo, mesa.id());
    }

    private record Persona(UUID id, String token) {
    }

    private record Mesa(UUID id, Persona master, List<Persona> jugadores) {
    }
}
