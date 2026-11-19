package es.unizar.ghostmark.backend.disputa;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import es.unizar.ghostmark.backend.TestcontainersConfiguration;
import es.unizar.ghostmark.backend.juego.GeneradorCodigoVida;
import es.unizar.ghostmark.backend.juego.TareaCierre;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
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

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DisputaTest {

    private static final String RUTA = "/api/v1/partidas/{id}/disputas";

    @LocalServerPort
    int puerto;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TareaCierre tareaCierre;

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
    void laVictimaDisputaSuEliminacionYElCazadorResponde() {
        Mesa mesa = preparar(4);
        Persona cazador = mesa.jugadores().get(0);
        UUID cazadorId = inscripcion(cazador, mesa);
        UUID victimaId = objetivo(cazadorId);
        Persona victima = persona(mesa, victimaId);
        String mision = misionTexto(cazadorId);
        Instant ahora = reloj.instant();
        eliminar(cazador, mesa, codigoDe(victimaId, ahora)).then().statusCode(200);
        carta(victima, mesa).then().statusCode(200)
                .body("disputarHasta", equalTo(ahora.plus(Duration.ofMinutes(10)).toString()));

        String disputaId = abrir(victima, mesa, "ELIMINACION", "  No me pidió un boli.  ")
                .then().statusCode(201)
                .body("tipo", equalTo("ELIMINACION"))
                .body("estado", equalTo("ABIERTA"))
                .body("aliasCazador", equalTo(cazador.alias()))
                .body("aliasVictima", equalTo(victima.alias()))
                .body("mision", equalTo(mision))
                .body("textoApertura", equalTo("No me pidió un boli."))
                .body("textoRespuesta", nullValue())
                .body("plazoHasta", equalTo(ahora.plus(Duration.ofHours(6)).toString()))
                .body("soyArbitro", equalTo(false))
                .body("puedoResponder", equalTo(false))
                .body("resultadoZona", nullValue())
                .extract().path("id");

        carta(victima, mesa).then().body("disputarHasta", nullValue());
        carta(cazador, mesa).then().body("bloqueadoPorDisputa", equalTo(true));
        eliminar(cazador, mesa, codigoDe(objetivo(cazadorId), ahora)).then().statusCode(409)
                .body("codigo", equalTo("DISPUTA_ABIERTA"));
        assertEquals(0, jdbc.queryForObject("select intentos_fallidos from jugador_partida"
                + " where inscripcion_id = ?", Integer.class, cazadorId));

        obtener(cazador, mesa, disputaId).then().statusCode(200)
                .body("puedoResponder", equalTo(true));
        responder(victima, mesa, disputaId).then().statusCode(403)
                .body("codigo", equalTo("NO_PUEDE_RESPONDER"));
        responder(mesa.master(), mesa, disputaId).then().statusCode(403)
                .body("codigo", equalTo("NO_PUEDE_RESPONDER"));
        responder(cazador, mesa, disputaId).then().statusCode(200)
                .body("textoRespuesta", equalTo("Le pedí un boli en la biblioteca."))
                .body("puedoResponder", equalTo(false));
        responder(cazador, mesa, disputaId).then().statusCode(409)
                .body("codigo", equalTo("YA_RESPONDIDA"));

        obtener(mesa.master(), mesa, disputaId).then().statusCode(200)
                .body("soyArbitro", equalTo(true))
                .body("resultadoZona", equalTo("FUERA"));
        Persona otro = persona(mesa, objetivo(objetivo(cazadorId)));
        obtener(otro, mesa, disputaId).then().statusCode(404);
        obtener(registrar(), mesa, disputaId).then().statusCode(404);
        assertEquals(1, eventos(mesa, "DISPUTA_ABIERTA"));
        assertEquals(1, eventos(mesa, "DISPUTA_RESPONDIDA"));
    }

    @Test
    void unaEliminacionSoloSeDisputaUnaVezYEnLosDiezMinutos() {
        Mesa mesa = preparar(4);
        Persona cazador = mesa.jugadores().get(0);
        UUID cazadorId = inscripcion(cazador, mesa);
        UUID victimaId = objetivo(cazadorId);
        Persona victima = persona(mesa, victimaId);
        Instant ahora = reloj.instant();
        eliminar(cazador, mesa, codigoDe(victimaId, ahora)).then().statusCode(200);
        abrir(victima, mesa, "ELIMINACION", "No fue así.").then().statusCode(201);
        abrir(victima, mesa, "ELIMINACION", "Otra vez.").then().statusCode(409)
                .body("codigo", equalTo("ELIMINACION_NO_DISPUTABLE"));

        UUID perseguidorId = jdbc.queryForObject("select inscripcion_id from jugador_partida"
                + " where objetivo_id = ?", UUID.class, cazadorId);
        eliminar(persona(mesa, perseguidorId), mesa, codigoDe(cazadorId, ahora))
                .then().statusCode(200);
        when(reloj.instant()).thenReturn(ahora.plus(Duration.ofMinutes(10)));
        carta(cazador, mesa).then().body("disputarHasta", nullValue());
        abrir(cazador, mesa, "ELIMINACION", "Tarde.").then().statusCode(409)
                .body("codigo", equalTo("PLAZO_DISPUTA_VENCIDO"));
        abrir(persona(mesa, perseguidorId), mesa, "ELIMINACION", "Sigo vivo.")
                .then().statusCode(409)
                .body("codigo", equalTo("ELIMINACION_NO_DISPUTABLE"));
    }

    @Test
    void laNegativaNoBloqueaAlCazadorYSeQuedaSinObjetoAlEliminarALaVictima() {
        Mesa mesa = preparar(4);
        Persona cazador = mesa.jugadores().get(0);
        UUID cazadorId = inscripcion(cazador, mesa);
        UUID victimaId = objetivo(cazadorId);
        Persona victima = persona(mesa, victimaId);

        String disputaId = abrir(cazador, mesa, "NEGATIVA", "No me enseña el código.")
                .then().statusCode(201)
                .body("tipo", equalTo("NEGATIVA"))
                .body("aliasCazador", equalTo(cazador.alias()))
                .body("aliasVictima", equalTo(victima.alias()))
                .body("mision", equalTo(misionTexto(cazadorId)))
                .extract().path("id");
        abrir(cazador, mesa, "NEGATIVA", "Otra.").then().statusCode(409)
                .body("codigo", equalTo("DISPUTA_YA_ABIERTA"));
        carta(cazador, mesa).then().body("bloqueadoPorDisputa", equalTo(false));
        responder(cazador, mesa, disputaId).then().statusCode(403);
        responder(victima, mesa, disputaId).then().statusCode(200);

        eliminar(cazador, mesa, codigoDe(victimaId, reloj.instant())).then().statusCode(200);
        obtener(cazador, mesa, disputaId).then().statusCode(200)
                .body("estado", equalTo("SIN_OBJETO"))
                .body("cerradaEn", notNullValue())
                .body("razon", nullValue());
        assertEquals("SIN_OBJETO", jdbc.queryForObject("select datos->>'estado' from evento"
                + " where partida_id = ? and tipo = 'DISPUTA_RESUELTA'", String.class,
                mesa.partida()));
        abrir(victima, mesa, "NEGATIVA", "Ya no juego.").then().statusCode(409)
                .body("codigo", equalTo("JUGADOR_ELIMINADO"));
    }

    @Test
    void elTextoYElTipoSeValidan() {
        Mesa mesa = preparar(3);
        Persona jugador = mesa.jugadores().get(0);
        for (String texto : List.of("   ", "a".repeat(281))) {
            abrir(jugador, mesa, "NEGATIVA", texto).then().statusCode(400)
                    .body("codigo", equalTo("DATOS_NO_VALIDOS"))
                    .body("errores[0].campo", equalTo("texto"));
        }
        for (String tipo : java.util.Arrays.asList("OTRA", null)) {
            abrir(jugador, mesa, tipo, "Algo pasó.").then().statusCode(400)
                    .body("errores[0].campo", equalTo("tipo"));
        }
        assertEquals(0, eventos(mesa, "DISPUTA_ABIERTA"));
    }

    @Test
    void cadaUnoVeSoloLasDisputasEnLasQueEstaYElMasterLasQueArbitra() {
        Mesa mesa = preparar(4);
        Persona cazador = mesa.jugadores().get(0);
        UUID cazadorId = inscripcion(cazador, mesa);
        UUID victimaId = objetivo(cazadorId);
        Persona victima = persona(mesa, victimaId);
        Persona ajeno = persona(mesa, objetivo(objetivo(victimaId)));
        eliminar(cazador, mesa, codigoDe(victimaId, reloj.instant())).then().statusCode(200);
        abrir(victima, mesa, "ELIMINACION", "No fue así.").then().statusCode(201);

        for (Persona implicado : List.of(cazador, victima)) {
            listar(implicado, mesa).then().statusCode(200).body("", hasSize(1))
                    .body("[0].soyArbitro", equalTo(false));
        }
        listar(mesa.master(), mesa).then().statusCode(200).body("", hasSize(1))
                .body("[0].soyArbitro", equalTo(true));
        listar(ajeno, mesa).then().statusCode(200).body("", hasSize(0));
        listar(registrar(), mesa).then().statusCode(404);
        obtener(cazador, mesa, UUID.randomUUID().toString()).then().statusCode(404);
    }

    @Test
    void laUltimaEliminacionCierraLaPartidaAlVencerLosDiezMinutos() {
        Mesa mesa = preparar(3);
        Persona cazador = mesa.jugadores().get(0);
        UUID cazadorId = inscripcion(cazador, mesa);
        Instant ahora = reloj.instant();
        eliminar(cazador, mesa, codigoDe(objetivo(cazadorId), ahora)).then().statusCode(200);
        eliminar(cazador, mesa, codigoDe(objetivo(cazadorId), ahora)).then().statusCode(200);

        assertEquals("EN_JUEGO", estado(mesa));
        carta(cazador, mesa).then().statusCode(200)
                .body("estado", equalTo("VIVO"))
                .body("aliasObjetivo", nullValue())
                .body("mision", nullValue());
        eliminar(cazador, mesa, "000000").then().statusCode(409)
                .body("codigo", equalTo("PARTIDA_NO_EN_JUEGO"));
        cerrar(mesa).then().statusCode(409).body("codigo", equalTo("DISPUTAS_PENDIENTES"));

        revisarCierre(ahora.plus(Duration.ofMinutes(10)).minusSeconds(1));
        assertEquals("EN_JUEGO", estado(mesa));
        revisarCierre(ahora.plus(Duration.ofMinutes(10)));
        assertEquals("CERRADA", estado(mesa));
        assertEquals("ULTIMO_SUPERVIVIENTE", motivoDeCierre(mesa));
    }

    @Test
    void conUnaDisputaAbiertaLaPartidaNoSeCierra() {
        Mesa mesa = preparar(3);
        Persona cazador = mesa.jugadores().get(0);
        UUID cazadorId = inscripcion(cazador, mesa);
        Instant ahora = reloj.instant();
        eliminar(cazador, mesa, codigoDe(objetivo(cazadorId), ahora)).then().statusCode(200);
        UUID ultimaId = objetivo(cazadorId);
        eliminar(cazador, mesa, codigoDe(ultimaId, ahora)).then().statusCode(200);
        String disputaId = abrir(persona(mesa, ultimaId), mesa, "ELIMINACION", "No fue así.")
                .then().statusCode(201).extract().path("id");

        revisarCierre(ahora.plus(Duration.ofHours(1)));
        assertEquals("EN_JUEGO", estado(mesa));
        cerrar(mesa).then().statusCode(409).body("codigo", equalTo("DISPUTAS_PENDIENTES"));

        // La decisión llega con la 11a.3: aquí se resuelve a mano para ver que entonces cierra
        jdbc.update("update disputa set estado = 'RESUELTA', razon = 'CAZADOR',"
                + " resuelta_por = 'MASTER', cerrada_en = now() where id = ?::uuid", disputaId);
        revisarCierre(ahora.plus(Duration.ofHours(1)));
        assertEquals("CERRADA", estado(mesa));
        assertEquals("ULTIMO_SUPERVIVIENTE", motivoDeCierre(mesa));
    }

    @Test
    void desdeLaFechaDeFinSoloSeDisputaYLaPartidaEsperaParaCerrar() {
        Mesa mesa = preparar(4);
        Persona cazador = mesa.jugadores().get(0);
        UUID cazadorId = inscripcion(cazador, mesa);
        UUID victimaId = objetivo(cazadorId);
        Instant ahora = reloj.instant();
        eliminar(cazador, mesa, codigoDe(victimaId, ahora)).then().statusCode(200);
        jdbc.update("update partida set fin_en = ?::timestamptz where id = ?",
                ahora.plusSeconds(60).toString(), mesa.partida());
        Instant despuesDelFin = ahora.plusSeconds(120);
        when(reloj.instant()).thenReturn(despuesDelFin);
        revisarCierre(despuesDelFin);
        assertEquals("EN_JUEGO", estado(mesa));

        UUID otroId = objetivo(cazadorId);
        Persona otro = persona(mesa, otroId);
        eliminar(otro, mesa, codigoDe(objetivo(otroId), despuesDelFin)).then().statusCode(409)
                .body("codigo", equalTo("PARTIDA_NO_EN_JUEGO"));
        abrir(otro, mesa, "NEGATIVA", "No me enseña el código.").then().statusCode(409)
                .body("codigo", equalTo("PARTIDA_NO_EN_JUEGO"));
        String disputaId = abrir(persona(mesa, victimaId), mesa, "ELIMINACION", "No fue así.")
                .then().statusCode(201).extract().path("id");
        responder(cazador, mesa, disputaId).then().statusCode(200);
        cerrar(mesa).then().statusCode(409).body("codigo", equalTo("DISPUTAS_PENDIENTES"));

        revisarCierre(ahora.plus(Duration.ofMinutes(30)));
        assertEquals("EN_JUEGO", estado(mesa));
        jdbc.update("update disputa set estado = 'RESUELTA', razon = 'CAZADOR',"
                + " resuelta_por = 'MASTER', cerrada_en = now() where id = ?::uuid", disputaId);
        revisarCierre(ahora.plus(Duration.ofMinutes(30)));
        assertEquals("CERRADA", estado(mesa));
        assertEquals("FECHA_FIN", motivoDeCierre(mesa));
    }

    private Mesa preparar(int numero) {
        Persona master = registrar();
        Instant inicio = Instant.now().plus(Duration.ofDays(1));
        Response respuesta = given().port(puerto).auth().oauth2(master.token())
                .contentType(ContentType.JSON).body(Map.of("nombre", "Partida Disputas",
                        "inicioEn", inicio.toString(), "finEn", inicio.plus(Duration.ofDays(7))
                                .toString(), "tipoMisiones", "BIBLIOTECA", "plazoDisputaHoras", 6,
                        "entradasTardias", false, "masterJuega", false))
                .post("/api/v1/partidas").then().statusCode(201).extract().response();
        UUID partida = UUID.fromString(respuesta.path("id"));
        List<Persona> jugadores = new ArrayList<>();
        while (jugadores.size() < numero) {
            Persona jugador = registrar();
            given().port(puerto).auth().oauth2(jugador.token()).contentType(ContentType.JSON)
                    .body(Map.of("codigo", respuesta.<String>path("codigoInvitacion"),
                            "aceptaNormas", true))
                    .post("/api/v1/inscripciones").then().statusCode(201);
            jugadores.add(jugador);
        }
        given().port(puerto).auth().oauth2(master.token())
                .post("/api/v1/partidas/{id}/arranque", partida).then().statusCode(200);
        return new Mesa(master, partida, jugadores);
    }

    private Persona registrar() {
        String alias = "d_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Response respuesta = given().port(puerto).contentType(ContentType.JSON)
                .body(Map.of("alias", alias, "email", alias + "@example.com",
                        "contrasena", "fantasma2026"))
                .post("/api/v1/cuentas").then().statusCode(201).extract().response();
        return new Persona(UUID.fromString(respuesta.path("cuenta.id")), alias,
                respuesta.path("tokenAcceso"));
    }

    private Response abrir(Persona persona, Mesa mesa, String tipo, String texto) {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("tipo", tipo);
        cuerpo.put("texto", texto);
        return given().port(puerto).auth().oauth2(persona.token()).contentType(ContentType.JSON)
                .body(cuerpo).post(RUTA, mesa.partida());
    }

    private Response responder(Persona persona, Mesa mesa, String disputaId) {
        return given().port(puerto).auth().oauth2(persona.token()).contentType(ContentType.JSON)
                .body(Map.of("texto", "Le pedí un boli en la biblioteca."))
                .post(RUTA + "/{disputa}/respuesta", mesa.partida(), disputaId);
    }

    private Response obtener(Persona persona, Mesa mesa, String disputaId) {
        return given().port(puerto).auth().oauth2(persona.token())
                .get(RUTA + "/{disputa}", mesa.partida(), disputaId);
    }

    private Response listar(Persona persona, Mesa mesa) {
        return given().port(puerto).auth().oauth2(persona.token()).get(RUTA, mesa.partida());
    }

    private Response carta(Persona persona, Mesa mesa) {
        return given().port(puerto).auth().oauth2(persona.token())
                .get("/api/v1/partidas/{id}/yo/carta", mesa.partida());
    }

    private Response eliminar(Persona persona, Mesa mesa, String codigo) {
        return given().port(puerto).auth().oauth2(persona.token()).contentType(ContentType.JSON)
                .body(Map.of("codigo", codigo, "resultadoZona", "FUERA"))
                .post("/api/v1/partidas/{id}/eliminaciones", mesa.partida());
    }

    private UUID inscripcion(Persona persona, Mesa mesa) {
        return jdbc.queryForObject("select id from inscripcion where partida_id = ?"
                + " and cuenta_id = ?", UUID.class, mesa.partida(), persona.id());
    }

    private UUID objetivo(UUID inscripcionId) {
        return jdbc.queryForObject("select objetivo_id from jugador_partida"
                + " where inscripcion_id = ?", UUID.class, inscripcionId);
    }

    private String misionTexto(UUID inscripcionId) {
        return jdbc.queryForObject("select m.texto from jugador_partida jp"
                + " join mision m on m.id = jp.mision_id where jp.inscripcion_id = ?",
                String.class, inscripcionId);
    }

    private Persona persona(Mesa mesa, UUID inscripcionId) {
        return mesa.jugadores().stream()
                .filter(jugador -> inscripcionId.equals(inscripcion(jugador, mesa)))
                .findFirst().orElseThrow();
    }

    private String codigoDe(UUID inscripcionId, Instant cuando) {
        byte[] secreto = jdbc.queryForObject("select secreto_codigo from jugador_partida"
                + " where inscripcion_id = ?", byte[].class, inscripcionId);
        return generador.generar(secreto, cuando).codigo();
    }

    private Response cerrar(Mesa mesa) {
        return given().port(puerto).auth().oauth2(mesa.master().token())
                .post("/api/v1/partidas/{id}/cierre", mesa.partida());
    }

    private void revisarCierre(Instant cuando) {
        when(reloj.instant()).thenReturn(cuando);
        tareaCierre.revisar(cuando);
    }

    private String estado(Mesa mesa) {
        return jdbc.queryForObject("select estado from partida where id = ?", String.class,
                mesa.partida());
    }

    private String motivoDeCierre(Mesa mesa) {
        return jdbc.queryForObject("select datos->>'motivo' from evento where partida_id = ?"
                + " and tipo = 'PARTIDA_CERRADA'", String.class, mesa.partida());
    }

    private int eventos(Mesa mesa, String tipo) {
        return jdbc.queryForObject("select count(*) from evento where partida_id = ?"
                + " and tipo = ?", Integer.class, mesa.partida(), tipo);
    }

    private record Persona(UUID id, String alias, String token) {
    }

    private record Mesa(Persona master, UUID partida, List<Persona> jugadores) {
    }
}
