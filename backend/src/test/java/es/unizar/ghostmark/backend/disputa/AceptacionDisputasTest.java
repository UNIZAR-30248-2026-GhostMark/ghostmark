package es.unizar.ghostmark.backend.disputa;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import es.unizar.ghostmark.backend.TestcontainersConfiguration;
import es.unizar.ghostmark.backend.juego.CodigosDePrueba;
import es.unizar.ghostmark.backend.juego.TareaCierre;
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
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Aceptacion de la PBI 11a mediante los endpoints de las tareas #291 y #292. */
@DisplayName("PBI 11a - aceptacion de disputas")
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AceptacionDisputasTest {

    private static final String RUTA_DISPUTAS = "/api/v1/partidas/{id}/disputas";
    private static final Duration DIEZ_MINUTOS = Duration.ofMinutes(10);

    @LocalServerPort
    int puerto;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TareaArbitraje tareaArbitraje;

    @Autowired
    TareaCierre tareaCierre;

    @MockitoBean
    Clock reloj;

    @BeforeEach
    void fijarReloj() {
        when(reloj.instant()).thenReturn(
                Instant.now().truncatedTo(ChronoUnit.MINUTES).plusSeconds(30));
        when(reloj.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @Test
    void criterio1LaVictimaAbreAntesDeDiezMinutosYElCazadorResponde() {
        Mesa mesa = preparar(false, 4);
        Persona cazador = mesa.jugadores().getFirst();
        Persona victima = persona(mesa, objetivo(inscrito(mesa, cazador)));
        Instant eliminadaEn = reloj.instant();
        eliminar(cazador, mesa, inscrito(mesa, victima), "FUERA")
                .then().statusCode(200);

        fijarHora(eliminadaEn.plus(DIEZ_MINUTOS).minusSeconds(1));
        String disputa = abrir(victima, mesa, "ELIMINACION", "No fue correcto")
                .then().statusCode(201)
                .body("estado", equalTo("ABIERTA"))
                .body("aliasCazador", equalTo(cazador.alias()))
                .body("aliasVictima", equalTo(victima.alias()))
                .body("textoApertura", equalTo("No fue correcto"))
                .body("puedoResponder", equalTo(false))
                .extract().path("id");
        responder(cazador, mesa, disputa, "Mostré la misión")
                .then().statusCode(200)
                .body("textoRespuesta", equalTo("Mostré la misión"))
                .body("puedoResponder", equalTo(false));
        responder(victima, mesa, disputa, "No puedo")
                .then().statusCode(403).body("codigo", equalTo("NO_PUEDE_RESPONDER"));
        assertEquals(1, eventos(mesa, "DISPUTA_ABIERTA"));
        assertEquals(1, eventos(mesa, "DISPUTA_RESPONDIDA"));
    }

    @Test
    void criterio1LaVictimaYaNoPuedeAbrirAlCumplirseLosDiezMinutos() {
        Mesa mesa = preparar(false, 4);
        Persona cazador = mesa.jugadores().getFirst();
        Persona victima = persona(mesa, objetivo(inscrito(mesa, cazador)));
        Instant eliminadaEn = reloj.instant();
        eliminar(cazador, mesa, inscrito(mesa, victima), "FUERA")
                .then().statusCode(200);

        fijarHora(eliminadaEn.plus(DIEZ_MINUTOS));
        abrir(victima, mesa, "ELIMINACION", "Fuera de plazo")
                .then().statusCode(409)
                .body("codigo", equalTo("PLAZO_DISPUTA_VENCIDO"));
        assertEquals(0, eventos(mesa, "DISPUTA_ABIERTA"));
    }

    @Test
    void criterio1ElCazadorPuedeDisputarUnaNegativaHastaLaFechaDeFin() {
        Mesa mesa = preparar(false, 4);
        Persona cazador = mesa.jugadores().getFirst();
        Instant limite = reloj.instant().plus(Duration.ofMinutes(20));
        jdbc.update("update partida set fin_en = ?::timestamptz where id = ?",
                limite.toString(), mesa.id());

        fijarHora(limite.minusSeconds(1));
        String disputa = abrir(cazador, mesa, "NEGATIVA", "No enseña su código")
                .then().statusCode(201)
                .body("estado", equalTo("ABIERTA"))
                .body("tipo", equalTo("NEGATIVA"))
                .extract().path("id");
        Persona victima = persona(mesa, objetivo(inscrito(mesa, cazador)));
        responder(victima, mesa, disputa, "No es cierto")
                .then().statusCode(200)
                .body("textoRespuesta", equalTo("No es cierto"));

        fijarHora(limite);
        Persona otro = mesa.jugadores().stream().filter(jugador -> !jugador.equals(cazador))
                .findFirst().orElseThrow();
        abrir(otro, mesa, "NEGATIVA", "Ya terminó la partida")
                .then().statusCode(409)
                .body("codigo", equalTo("PARTIDA_NO_EN_JUEGO"));
    }

    @Test
    void criterio2UnaDisputaAbiertaBloqueaOtraEliminacionDelCazador() {
        Mesa mesa = preparar(false, 4);
        Persona cazador = mesa.jugadores().getFirst();
        UUID cazadorId = inscrito(mesa, cazador);
        UUID victimaId = objetivo(cazadorId);
        Persona victima = persona(mesa, victimaId);
        eliminar(cazador, mesa, victimaId, "FUERA").then().statusCode(200);
        String disputa = abrir(victima, mesa, "ELIMINACION", "Disputo la eliminación")
                .then().statusCode(201).extract().path("id");
        carta(cazador, mesa).then().statusCode(200)
                .body("bloqueadoPorDisputa", equalTo(true));

        UUID siguiente = objetivo(cazadorId);
        eliminar(cazador, mesa, siguiente, "FUERA").then().statusCode(409)
                .body("codigo", equalTo("DISPUTA_ABIERTA"));
        assertEquals(0, jdbc.queryForObject("select intentos_fallidos from jugador_partida"
                + " where inscripcion_id = ?", Integer.class, cazadorId));
        assertEquals("VIVO", estadoJugador(siguiente));

        decidir(mesa.master(), mesa, disputa, "CAZADOR").then().statusCode(200)
                .body("estado", equalTo("RESUELTA"));
        carta(cazador, mesa).then().body("bloqueadoPorDisputa", equalTo(false));
        eliminar(cazador, mesa, siguiente, "FUERA").then().statusCode(200);
        assertEquals("ELIMINADO", estadoJugador(siguiente));
    }

    @Test
    void criterio3ElMasterArbitraConVistaLimitadaYConfirmaUnaNegativa() {
        Mesa mesa = preparar(false, 4);
        Persona cazador = mesa.jugadores().getFirst();
        Persona victima = persona(mesa, objetivo(inscrito(mesa, cazador)));
        Persona ajeno = mesa.jugadores().stream()
                .filter(persona -> !persona.equals(cazador) && !persona.equals(victima))
                .findFirst().orElseThrow();
        String disputa = abrir(cazador, mesa, "NEGATIVA", "No me da el código")
                .then().statusCode(201).extract().path("id");
        responder(victima, mesa, disputa, "No me lo pidió").then().statusCode(200);

        Map<String, Object> datos = obtener(mesa.master(), mesa, disputa)
                .then().statusCode(200).body("soyArbitro", equalTo(true))
                .extract().jsonPath().getMap("");
        assertEquals(Set.of("id", "tipo", "estado", "aliasCazador", "aliasVictima",
                "mision", "textoApertura", "textoRespuesta", "abiertaEn", "plazoHasta",
                "soyArbitro", "puedoResponder", "resultadoZona", "razon", "resueltaPor",
                "cerradaEn"), datos.keySet());
        assertEquals(cazador.alias(), datos.get("aliasCazador"));
        assertEquals(victima.alias(), datos.get("aliasVictima"));
        assertNotNull(datos.get("mision"));
        obtener(ajeno, mesa, disputa).then().statusCode(404);
        decidir(cazador, mesa, disputa, "CAZADOR").then().statusCode(403)
                .body("codigo", equalTo("NO_ES_ARBITRO"));
        decidir(mesa.master(), mesa, disputa, "CAZADOR").then().statusCode(200)
                .body("estado", equalTo("RESUELTA"))
                .body("razon", equalTo("CAZADOR"))
                .body("resueltaPor", equalTo("MASTER"));
        assertEquals("ELIMINADO", estadoJugador(inscrito(mesa, victima)));
        decidir(mesa.master(), mesa, disputa, "VICTIMA").then().statusCode(409)
                .body("codigo", equalTo("DISPUTA_NO_ABIERTA"));
    }

    @Test
    void criterio3ConMasterImplicadoSeSorteaOtroArbitroYSeReasignaAlVencer() {
        Mesa mesa = preparar(true, 5);
        UUID masterId = inscrito(mesa, mesa.master());
        UUID victimaId = objetivo(masterId);
        String disputa = abrir(mesa.master(), mesa, "NEGATIVA", "Me niega el código")
                .then().statusCode(201).extract().path("id");
        UUID inicial = arbitro(disputa);
        assertNotNull(inicial);
        assertNotEquals(masterId, inicial);
        assertNotEquals(victimaId, inicial);
        decidir(mesa.master(), mesa, disputa, "VICTIMA").then().statusCode(403)
                .body("codigo", equalTo("NO_ES_ARBITRO"));

        Instant vencimiento = reloj.instant().plus(Duration.ofHours(6));
        tareaArbitraje.revisar(vencimiento.minusSeconds(1));
        assertEquals(inicial, arbitro(disputa));
        tareaArbitraje.revisar(vencimiento);
        UUID siguiente = arbitro(disputa);
        assertNotNull(siguiente);
        assertNotEquals(inicial, siguiente);
        assertNotEquals(masterId, siguiente);
        assertNotEquals(victimaId, siguiente);
        assertEquals(2, eventos(mesa, "DISPUTA_ARBITRO_SORTEADO"));
        fijarHora(vencimiento.plusSeconds(1));
        decidir(persona(mesa, inicial), mesa, disputa, "VICTIMA").then().statusCode(404);
        decidir(persona(mesa, siguiente), mesa, disputa, "VICTIMA")
                .then().statusCode(200)
                .body("estado", equalTo("RESUELTA"))
                .body("resueltaPor", equalTo("SORTEADO"));
    }

    @Test
    void criterio4ConZonaDentroLaDisputaSeResuelveAutomaticamente() {
        Mesa mesa = preparar(false, 4);
        Persona cazador = mesa.jugadores().getFirst();
        Persona victima = persona(mesa, objetivo(inscrito(mesa, cazador)));
        eliminar(cazador, mesa, inscrito(mesa, victima), "DENTRO")
                .then().statusCode(200);

        String disputa = abrir(victima, mesa, "ELIMINACION", "Era una zona segura")
                .then().statusCode(201)
                .body("estado", equalTo("RESUELTA"))
                .body("razon", equalTo("VICTIMA"))
                .body("resueltaPor", equalTo("ZONA"))
                .body("resultadoZona", nullValue())
                .extract().path("id");
        obtener(mesa.master(), mesa, disputa).then().statusCode(200)
                .body("resultadoZona", equalTo("DENTRO"));
        obtener(cazador, mesa, disputa).then().statusCode(200)
                .body("resultadoZona", nullValue());
        assertEquals(1, eventos(mesa, "DISPUTA_RESUELTA"));
        assertEquals(0, eventos(mesa, "DISPUTA_ARBITRO_SORTEADO"));
    }

    @Test
    void criterio4ConZonaNoConcluyenteDecideElMaster() {
        Mesa mesa = preparar(false, 4);
        Persona cazador = mesa.jugadores().getFirst();
        Persona victima = persona(mesa, objetivo(inscrito(mesa, cazador)));
        eliminar(cazador, mesa, inscrito(mesa, victima), "NO_CONCLUYENTE")
                .then().statusCode(200);
        String disputa = abrir(victima, mesa, "ELIMINACION", "La precisión era baja")
                .then().statusCode(201)
                .body("estado", equalTo("ABIERTA"))
                .extract().path("id");
        obtener(mesa.master(), mesa, disputa).then().statusCode(200)
                .body("soyArbitro", equalTo(true))
                .body("resultadoZona", equalTo("NO_CONCLUYENTE"));
        decidir(mesa.master(), mesa, disputa, "VICTIMA").then().statusCode(200)
                .body("estado", equalTo("RESUELTA"))
                .body("razon", equalTo("VICTIMA"))
                .body("resueltaPor", equalTo("MASTER"));
        assertEquals(1, eventos(mesa, "DISPUTA_RESUELTA"));
    }

    @Test
    void criterio5ElCierreEsperaElPlazoYLaResolucionDeDisputas() {
        Mesa mesa = preparar(false, 3);
        Persona cazador = mesa.jugadores().getFirst();
        UUID cazadorId = inscrito(mesa, cazador);
        Instant ultimoGolpe = reloj.instant();
        UUID primera = objetivo(cazadorId);
        eliminar(cazador, mesa, primera, "FUERA").then().statusCode(200);
        UUID ultima = objetivo(cazadorId);
        Persona ultimaVictima = persona(mesa, ultima);
        eliminar(cazador, mesa, ultima, "FUERA").then().statusCode(200);
        assertEquals("EN_JUEGO", estadoPartida(mesa));

        tareaCierre.revisar(ultimoGolpe.plus(DIEZ_MINUTOS).minusSeconds(1));
        assertEquals("EN_JUEGO", estadoPartida(mesa));
        String disputa = abrir(ultimaVictima, mesa, "ELIMINACION", "Quiero arbitraje")
                .then().statusCode(201).extract().path("id");
        tareaCierre.revisar(ultimoGolpe.plus(Duration.ofMinutes(11)));
        assertEquals("EN_JUEGO", estadoPartida(mesa));
        cerrar(mesa).then().statusCode(409)
                .body("codigo", equalTo("DISPUTAS_PENDIENTES"));

        fijarHora(ultimoGolpe.plus(Duration.ofMinutes(11)));
        decidir(mesa.master(), mesa, disputa, "CAZADOR").then().statusCode(200)
                .body("estado", equalTo("RESUELTA"));
        tareaCierre.revisar(ultimoGolpe.plus(Duration.ofMinutes(11)));
        assertEquals("CERRADA", estadoPartida(mesa));
        assertEquals(1, eventos(mesa, "PARTIDA_CERRADA"));
    }

    private Mesa preparar(boolean masterJuega, int numero) {
        Persona master = registrar();
        Instant inicio = Instant.now().plus(Duration.ofDays(1));
        Response creada = given().port(puerto).auth().oauth2(master.token())
                .contentType(ContentType.JSON)
                .body(Map.of("nombre", "Partida Aceptacion 11a", "inicioEn", inicio.toString(),
                        "finEn", inicio.plus(Duration.ofDays(7)).toString(),
                        "tipoMisiones", "BIBLIOTECA", "plazoDisputaHoras", 6,
                        "entradasTardias", false, "masterJuega", masterJuega))
                .post("/api/v1/partidas").then().statusCode(201).extract().response();
        UUID id = UUID.fromString(creada.path("id"));
        List<Persona> jugadores = new ArrayList<>();
        if (masterJuega) {
            jugadores.add(master);
        }
        while (jugadores.size() < numero) {
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
        return new Mesa(id, master, jugadores);
    }

    private Persona registrar() {
        String alias = "ad_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Response respuesta = given().port(puerto).contentType(ContentType.JSON)
                .body(Map.of("alias", alias, "email", alias + "@example.com",
                        "contrasena", "fantasma2026"))
                .post("/api/v1/cuentas").then().statusCode(201).extract().response();
        return new Persona(UUID.fromString(respuesta.path("cuenta.id")), alias,
                respuesta.path("tokenAcceso"));
    }

    private void fijarHora(Instant instante) {
        when(reloj.instant()).thenReturn(instante);
    }

    private Response abrir(Persona persona, Mesa mesa, String tipo, String texto) {
        return given().port(puerto).auth().oauth2(persona.token())
                .contentType(ContentType.JSON).body(Map.of("tipo", tipo, "texto", texto))
                .post(RUTA_DISPUTAS, mesa.id());
    }

    private Response responder(Persona persona, Mesa mesa, String disputa, String texto) {
        return given().port(puerto).auth().oauth2(persona.token())
                .contentType(ContentType.JSON).body(Map.of("texto", texto))
                .post(RUTA_DISPUTAS + "/{disputa}/respuesta", mesa.id(), disputa);
    }

    private Response obtener(Persona persona, Mesa mesa, String disputa) {
        return given().port(puerto).auth().oauth2(persona.token())
                .get(RUTA_DISPUTAS + "/{disputa}", mesa.id(), disputa);
    }

    private Response decidir(Persona persona, Mesa mesa, String disputa, String razon) {
        return given().port(puerto).auth().oauth2(persona.token())
                .contentType(ContentType.JSON).body(Map.of("razon", razon))
                .post(RUTA_DISPUTAS + "/{disputa}/decision", mesa.id(), disputa);
    }

    private Response eliminar(Persona cazador, Mesa mesa, UUID victima, String zona) {
        return given().port(puerto).auth().oauth2(cazador.token())
                .contentType(ContentType.JSON)
                .body(Map.of("codigo", CodigosDePrueba.emitir(jdbc, victima, reloj.instant()),
                        "resultadoZona", zona))
                .post("/api/v1/partidas/{id}/eliminaciones", mesa.id());
    }

    private Response carta(Persona persona, Mesa mesa) {
        return given().port(puerto).auth().oauth2(persona.token())
                .get("/api/v1/partidas/{id}/yo/carta", mesa.id());
    }

    private Response cerrar(Mesa mesa) {
        return given().port(puerto).auth().oauth2(mesa.master().token())
                .post("/api/v1/partidas/{id}/cierre", mesa.id());
    }

    private UUID inscrito(Mesa mesa, Persona persona) {
        return jdbc.queryForObject("select id from inscripcion where partida_id = ?"
                + " and cuenta_id = ?", UUID.class, mesa.id(), persona.id());
    }

    private UUID objetivo(UUID inscripcionId) {
        return jdbc.queryForObject("select objetivo_id from jugador_partida"
                + " where inscripcion_id = ?", UUID.class, inscripcionId);
    }

    private Persona persona(Mesa mesa, UUID inscripcionId) {
        return mesa.jugadores().stream()
                .filter(jugador -> inscripcionId.equals(inscrito(mesa, jugador)))
                .findFirst().orElseThrow();
    }

    private String estadoJugador(UUID inscripcionId) {
        return jdbc.queryForObject("select estado from jugador_partida where inscripcion_id = ?",
                String.class, inscripcionId);
    }

    private String estadoPartida(Mesa mesa) {
        return jdbc.queryForObject("select estado from partida where id = ?", String.class,
                mesa.id());
    }

    private UUID arbitro(String disputa) {
        return jdbc.queryForObject("select arbitro_id from disputa where id = ?::uuid",
                UUID.class, disputa);
    }

    private int eventos(Mesa mesa, String tipo) {
        return jdbc.queryForObject("select count(*) from evento where partida_id = ?"
                + " and tipo = ?", Integer.class, mesa.id(), tipo);
    }

    private record Persona(UUID id, String alias, String token) {
    }

    private record Mesa(UUID id, Persona master, List<Persona> jugadores) {
    }
}
