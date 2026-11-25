package es.unizar.ghostmark.backend.cuenta;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import es.unizar.ghostmark.backend.TestcontainersConfiguration;
import es.unizar.ghostmark.backend.cuenta.repositories.FranjaHorarioRepository;
import es.unizar.ghostmark.backend.juego.CodigosDePrueba;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Borrado de cuenta y conservación del historial de las partidas en juego.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BorradoCuentaTest {

    @LocalServerPort
    int puerto;

    @Autowired
    CuentaRepository cuentas;

    @Autowired
    FranjaHorarioRepository franjas;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void borrarCuentaEnJuegoConservaLosEventosYDejaLaInscripcionSinAlias() {
        Response master = registrarParaPartida();
        Response jugador = registrarParaPartida();
        Response tercero = registrarParaPartida();
        String tokenMaster = master.path("tokenAcceso");
        String tokenJugador = jugador.path("tokenAcceso");
        UUID cuentaId = UUID.fromString(jugador.path("cuenta.id"));
        Instant inicio = Instant.now().plus(Duration.ofDays(1));
        Response creada = given().port(puerto).auth().oauth2(tokenMaster)
                .contentType(ContentType.JSON).body(Map.of("nombre", "Borrado en juego",
                        "inicioEn", inicio.toString(), "finEn", inicio.plus(Duration.ofDays(7))
                                .toString(), "tipoMisiones", "BIBLIOTECA", "plazoDisputaHoras", 6,
                        "entradasTardias", false, "masterJuega", true))
                .post("/api/v1/partidas").then().statusCode(201).extract().response();
        UUID partidaId = UUID.fromString(creada.path("id"));
        for (String token : List.of(tokenJugador, tercero.<String>path("tokenAcceso"))) {
            given().port(puerto).auth().oauth2(token).contentType(ContentType.JSON)
                    .body(Map.of("codigo", creada.<String>path("codigoInvitacion"),
                            "aceptaNormas", true))
                    .post("/api/v1/inscripciones").then().statusCode(201);
        }
        given().port(puerto).auth().oauth2(tokenMaster)
                .post("/api/v1/partidas/{id}/arranque", partidaId).then().statusCode(200);
        UUID inscripcionId = jdbc.queryForObject("select id from inscripcion"
                + " where partida_id = ? and cuenta_id = ?", UUID.class, partidaId, cuentaId);
        List<Map<String, Object>> anteriores = eventosDe(partidaId);
        assertFalse(anteriores.isEmpty());
        assertTrue(anteriores.stream().anyMatch(evento -> evento.get("datos").toString()
                .contains(inscripcionId.toString())));

        given().port(puerto).auth().oauth2(tokenJugador).delete("/api/v1/cuentas/yo")
                .then().statusCode(204);
        assertTrue(cuentas.findById(cuentaId).isEmpty());
        // Se conserva la inscripción del ciclo; solo desaparece su relación con la cuenta.
        assertNull(jdbc.queryForObject("select cuenta_id from inscripcion where id = ?",
                UUID.class, inscripcionId));
        // La comparación completa detecta tanto reescrituras como eventos borrados o añadidos.
        assertEquals(anteriores, eventosDe(partidaId));
        for (Map<String, Object> evento : anteriores) {
            String datos = evento.get("datos").toString();
            assertFalse(datos.contains(jugador.<String>path("cuenta.alias")));
            assertFalse(datos.contains(cuentaId.toString()));
        }
        // El contrato devuelve alias nulo; la app lo representa como «Cuenta eliminada».
        given().port(puerto).auth().oauth2(tokenMaster)
                .get("/api/v1/partidas/{id}/inscritos", partidaId).then().statusCode(200)
                .body("find { it.id == '" + inscripcionId + "' }.id",
                        equalTo(inscripcionId.toString()))
                .body("find { it.id == '" + inscripcionId + "' }.alias", nullValue())
                .body("find { it.id == '" + inscripcionId + "' }.estado", equalTo("VIVO"));
    }

    @Test
    void borrarCuentaConDisputasAbiertasLasConservaSinAliasNiCuenta() {
        Response master = registrarParaPartida();
        String tokenMaster = master.path("tokenAcceso");
        Instant inicio = Instant.now().plus(Duration.ofDays(1));
        Response creada = given().port(puerto).auth().oauth2(tokenMaster)
                .contentType(ContentType.JSON).body(Map.of("nombre", "Borrado con disputas",
                        "inicioEn", inicio.toString(), "finEn", inicio.plus(Duration.ofDays(7))
                                .toString(), "tipoMisiones", "BIBLIOTECA", "plazoDisputaHoras", 6,
                        "entradasTardias", false, "masterJuega", false))
                .post("/api/v1/partidas").then().statusCode(201).extract().response();
        UUID partidaId = UUID.fromString(creada.path("id"));
        Map<UUID, Response> jugadores = new HashMap<>();
        for (int i = 0; i < 4; i++) {
            Response jugador = registrarParaPartida();
            given().port(puerto).auth().oauth2(jugador.<String>path("tokenAcceso"))
                    .contentType(ContentType.JSON)
                    .body(Map.of("codigo", creada.<String>path("codigoInvitacion"),
                            "aceptaNormas", true))
                    .post("/api/v1/inscripciones").then().statusCode(201);
            jugadores.put(jdbc.queryForObject("select id from inscripcion where partida_id = ?"
                    + " and cuenta_id = ?", UUID.class, partidaId,
                    UUID.fromString(jugador.path("cuenta.id"))), jugador);
        }
        given().port(puerto).auth().oauth2(tokenMaster)
                .post("/api/v1/partidas/{id}/arranque", partidaId).then().statusCode(200);

        UUID cazador = jugadores.keySet().iterator().next();
        UUID victima = objetivo(cazador);
        given().port(puerto).auth().oauth2(token(jugadores, cazador))
                .contentType(ContentType.JSON)
                .body(Map.of("codigo", CodigosDePrueba.emitir(jdbc, victima, Instant.now()),
                        "resultadoZona", "FUERA"))
                .post("/api/v1/partidas/{id}/eliminaciones", partidaId)
                .then().statusCode(200);
        String porEliminacion = abrirDisputa(partidaId, token(jugadores, victima),
                "ELIMINACION");
        UUID negador = jugadores.keySet().stream()
                .filter(id -> !id.equals(cazador) && !id.equals(victima))
                .findFirst().orElseThrow();
        UUID negado = objetivo(negador);
        String porNegativa = abrirDisputa(partidaId, token(jugadores, negador), "NEGATIVA");

        borrarCuenta(jugadores.get(victima));
        borrarCuenta(jugadores.get(negador));

        for (String token : List.of(tokenMaster, token(jugadores, cazador))) {
            Response vista = verDisputa(partidaId, porEliminacion, token);
            vista.then().statusCode(200).body("estado", equalTo("ABIERTA"))
                    .body("aliasVictima", nullValue())
                    .body("aliasCazador", equalTo(jugadores.get(cazador).path("cuenta.alias")));
            sinRastroDe(vista, jugadores.get(victima));
        }
        for (String token : List.of(tokenMaster, token(jugadores, negado))) {
            Response vista = verDisputa(partidaId, porNegativa, token);
            vista.then().statusCode(200).body("estado", equalTo("ABIERTA"))
                    .body("aliasCazador", nullValue())
                    .body("aliasVictima", equalTo(jugadores.get(negado).path("cuenta.alias")));
            sinRastroDe(vista, jugadores.get(negador));
        }
        decidir(partidaId, porEliminacion, tokenMaster, "CAZADOR");
        decidir(partidaId, porNegativa, tokenMaster, "VICTIMA");
    }

    private UUID objetivo(UUID inscripcionId) {
        return jdbc.queryForObject("select objetivo_id from jugador_partida"
                + " where inscripcion_id = ?", UUID.class, inscripcionId);
    }

    private static String token(Map<UUID, Response> jugadores, UUID inscripcionId) {
        return jugadores.get(inscripcionId).path("tokenAcceso");
    }

    private String abrirDisputa(UUID partidaId, String token, String tipo) {
        return given().port(puerto).auth().oauth2(token).contentType(ContentType.JSON)
                .body(Map.of("tipo", tipo, "texto", "No fue limpio."))
                .post("/api/v1/partidas/{id}/disputas", partidaId)
                .then().statusCode(201).extract().path("id");
    }

    private Response verDisputa(UUID partidaId, String disputa, String token) {
        return given().port(puerto).auth().oauth2(token)
                .get("/api/v1/partidas/{id}/disputas/{disputa}", partidaId, disputa);
    }

    private void decidir(UUID partidaId, String disputa, String token, String razon) {
        given().port(puerto).auth().oauth2(token).contentType(ContentType.JSON)
                .body(Map.of("razon", razon))
                .post("/api/v1/partidas/{id}/disputas/{disputa}/decision", partidaId, disputa)
                .then().statusCode(200).body("estado", equalTo("RESUELTA"));
    }

    private void borrarCuenta(Response jugador) {
        given().port(puerto).auth().oauth2(jugador.<String>path("tokenAcceso"))
                .delete("/api/v1/cuentas/yo").then().statusCode(204);
    }

    private static void sinRastroDe(Response vista, Response cuenta) {
        String cuerpo = vista.asString();
        assertFalse(cuerpo.contains(cuenta.<String>path("cuenta.alias")));
        assertFalse(cuerpo.contains(cuenta.<String>path("cuenta.id")));
    }

    private List<Map<String, Object>> eventosDe(UUID partidaId) {
        return jdbc.queryForList("select id, partida_id, secuencia, tipo, datos::text as datos,"
                + " ocurrido_en from evento where partida_id = ? order by secuencia", partidaId);
    }

    private Response registrarParaPartida() {
        String alias = "b4_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return given().port(puerto).contentType(ContentType.JSON)
                .body(Map.of("alias", alias, "email", alias + "@example.com",
                        "contrasena", "fantasma2026"))
                .post("/api/v1/cuentas").then().statusCode(201).extract().response();
    }

    @Test
    void borrarCuentaEliminaDatosEImpideIniciarSesion() {
        String alias = "borrar_u";
        String email = "borrar@example.com";
        String contrasena = "fantasma2026";

        var registro = given()
            .port(puerto)
            .contentType(ContentType.JSON)
            .body(Map.of(
                "alias", alias,
                "email", email,
                "contrasena", contrasena))
            .when()
            .post("/api/v1/cuentas")
            .then()
            .statusCode(201)
            .extract()
            .response();

        String acceso = registro.path("tokenAcceso");
        String refresco = registro.path("tokenRefresco");

        UUID cuentaId = cuentas.findByEmail(email).orElseThrow().getId();

        given()
            .port(puerto)
            .header("Authorization", "Bearer " + acceso)
            .contentType(ContentType.JSON)
            .body(Map.of(
                "franjas", List.of(
                    Map.of("dia", 1, "horaInicio", "08:00"))))
            .when()
            .put("/api/v1/cuentas/yo/horario")
            .then()
            .statusCode(200);

        assertFalse(franjas.buscarPorCuentaId(cuentaId).isEmpty());

        PartidaDePrueba partida = crearPartidaAjena();
        String inscripcionId = given().port(puerto).auth().oauth2(acceso)
                .contentType(ContentType.JSON)
                .body(Map.of("codigo", partida.codigo(), "aceptaNormas", true))
                .post("/api/v1/inscripciones").then().statusCode(201)
                .extract().path("id");
        UUID idInscripcion = UUID.fromString(inscripcionId);
        assertEquals(1, jdbc.queryForObject("select count(*) from franja_horario_inscripcion"
                + " where inscripcion_id = ?", Integer.class, idInscripcion));
        List<Map<String, Object>> eventosAntes = jdbc.queryForList(
                "select * from evento where partida_id = ? order by secuencia", partida.id());

        given()
            .port(puerto)
            .header("Authorization", "Bearer " + acceso)
            .when()
            .delete("/api/v1/cuentas/yo")
            .then()
            .statusCode(204);

        // La cuenta y su horario ya no existen.
        assertTrue(cuentas.findById(cuentaId).isEmpty());
        assertTrue(franjas.buscarPorCuentaId(cuentaId).isEmpty());

        // También desaparecen su inscripción y la copia del horario de la partida.
        assertEquals(0, jdbc.queryForObject("select count(*) from inscripcion where id = ?",
                Integer.class, idInscripcion));
        assertEquals(0, jdbc.queryForObject("select count(*) from inscripcion where cuenta_id = ?",
                Integer.class, cuentaId));
        assertEquals(0, jdbc.queryForObject("select count(*) from franja_horario_inscripcion"
                + " where inscripcion_id = ?", Integer.class, idInscripcion));
        given().port(puerto).auth().oauth2(partida.tokenMaster())
                .get("/api/v1/partidas/{id}/inscritos", partida.id())
                .then().statusCode(200).body("size()", equalTo(0));
        List<Map<String, Object>> eventosDespues = jdbc.queryForList(
                "select * from evento where partida_id = ? order by secuencia", partida.id());
        assertEquals(eventosAntes.size() + 1, eventosDespues.size());
        assertEquals(eventosAntes, eventosDespues.subList(0, eventosAntes.size()));
        assertEquals("INSCRIPCION_RETIRADA",
                eventosDespues.get(eventosAntes.size()).get("tipo"));

        // Ya no puede iniciar sesión.
        given()
            .port(puerto)
            .contentType(ContentType.JSON)
            .body(Map.of(
                "email", email,
                "contrasena", contrasena))
            .when()
            .post("/api/v1/sesiones")
            .then()
            .statusCode(401);

        // El token de refresco anterior deja de funcionar.
        given()
            .port(puerto)
            .contentType(ContentType.JSON)
            .body(Map.of("tokenRefresco", refresco))
            .when()
            .post("/api/v1/sesiones/refresco")
            .then()
            .statusCode(401);

        // El token de acceso anterior tampoco permite consultar el horario.
        given()
            .port(puerto)
            .header("Authorization", "Bearer " + acceso)
            .when()
            .get("/api/v1/cuentas/yo/horario")
            .then()
            .statusCode(401);

        // El alias y el email quedan disponibles para otra cuenta.
        given()
            .port(puerto)
            .contentType(ContentType.JSON)
            .body(Map.of(
                "alias", alias,
                "email", email,
                "contrasena", contrasena))
            .when()
            .post("/api/v1/cuentas")
            .then()
            .statusCode(201);
    }

    private PartidaDePrueba crearPartidaAjena() {
        String alias = "b_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String tokenMaster = given().port(puerto).contentType(ContentType.JSON)
                .body(Map.of("alias", alias, "email", alias + "@example.com",
                        "contrasena", "fantasma2026"))
                .post("/api/v1/cuentas").then().statusCode(201).extract().path("tokenAcceso");
        Instant inicio = Instant.now().plus(Duration.ofDays(2));
        var respuesta = given().port(puerto).auth().oauth2(tokenMaster)
                .contentType(ContentType.JSON)
                .body(Map.of("nombre", "Borrado PBI 3", "inicioEn", inicio.toString(),
                        "finEn", inicio.plus(Duration.ofDays(7)).toString(),
                        "tipoMisiones", "BIBLIOTECA", "plazoDisputaHoras", 6,
                        "entradasTardias", false, "masterJuega", false))
                .post("/api/v1/partidas").then().statusCode(201).extract().response();
        return new PartidaDePrueba(UUID.fromString(respuesta.path("id")),
                respuesta.path("codigoInvitacion"), tokenMaster);
    }

    private record PartidaDePrueba(UUID id, String codigo, String tokenMaster) {
    }
}
