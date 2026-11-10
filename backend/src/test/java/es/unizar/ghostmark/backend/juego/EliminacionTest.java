package es.unizar.ghostmark.backend.juego;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
class EliminacionTest {

    private static final String RUTA_ELIMINAR = "/api/v1/partidas/{id}/eliminaciones";
    private static final String RUTA_CARTA = "/api/v1/partidas/{id}/yo/carta";

    @LocalServerPort
    int puerto;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoBean
    Clock reloj;

    private final GeneradorCodigoVida generador = new GeneradorCodigoVida();

    @BeforeEach
    void fijarReloj() {
        // A mitad del minuto: ningún test depende de cuándo cambia el código de verdad.
        Instant ahora = Instant.now().truncatedTo(ChronoUnit.MINUTES).plusSeconds(30);
        when(reloj.instant()).thenReturn(ahora);
        when(reloj.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @Test
    void eliminaConElCodigoDelObjetivoYHeredaObjetivoYMision() {
        Mesa mesa = preparar(4, true);
        arrancar(mesa).then().statusCode(200);
        Persona cazador = mesa.jugadores().get(0);
        UUID cazadorId = inscripcion(cazador, mesa.juego());
        UUID victimaId = objetivo(mesa.juego(), cazadorId);
        UUID objetivoHeredado = objetivo(mesa.juego(), victimaId);
        UUID misionCazador = mision(mesa.juego(), cazadorId);
        UUID misionHeredada = mision(mesa.juego(), victimaId);
        String aliasVictima = alias(victimaId);

        Response respuesta = eliminar(cazador, mesa.juego(), codigoDe(mesa.juego(), victimaId,
                reloj.instant()));

        respuesta.then().statusCode(200)
                .body("aliasVictima", equalTo(aliasVictima))
                .body("carta.estado", equalTo("VIVO"))
                .body("carta.aliasObjetivo", equalTo(alias(objetivoHeredado)))
                .body("carta.mision", equalTo(misionTexto(misionHeredada)))
                .body("carta.eliminaciones", equalTo(1));

        assertEquals("ELIMINADO", jdbc.queryForObject("select estado from jugador_partida"
                + " where partida_id = ? and inscripcion_id = ?", String.class,
                mesa.juego().id(), victimaId));
        assertNotNull(jdbc.queryForObject("select eliminado_en from jugador_partida"
                + " where partida_id = ? and inscripcion_id = ?", java.time.OffsetDateTime.class,
                mesa.juego().id(), victimaId));
        assertNull(jdbc.queryForObject("select objetivo_id from jugador_partida"
                + " where partida_id = ? and inscripcion_id = ?", UUID.class,
                mesa.juego().id(), victimaId));
        assertNull(jdbc.queryForObject("select mision_id from jugador_partida"
                + " where partida_id = ? and inscripcion_id = ?", UUID.class,
                mesa.juego().id(), victimaId));
        assertEquals(objetivoHeredado, objetivo(mesa.juego(), cazadorId));
        assertEquals(misionHeredada, mision(mesa.juego(), cazadorId));
        assertEquals(1, jdbc.queryForObject("select eliminaciones from jugador_partida"
                + " where partida_id = ? and inscripcion_id = ?", Integer.class,
                mesa.juego().id(), cazadorId));

        assertEquals(1, jdbc.queryForObject("select count(*) from evento where partida_id = ?"
                + " and tipo = 'JUGADOR_ELIMINADO'", Integer.class, mesa.juego().id()));
        Map<String, Object> datos = jdbc.queryForMap("select datos->>'cazadorId' as cazador,"
                + " datos->>'victimaId' as victima, datos->>'misionId' as mision,"
                + " datos->>'objetivoHeredadoId' as objetivo,"
                + " datos->>'misionHeredadaId' as heredada from evento"
                + " where partida_id = ? and tipo = 'JUGADOR_ELIMINADO'", mesa.juego().id());
        assertEquals(cazadorId.toString(), datos.get("cazador"));
        assertEquals(victimaId.toString(), datos.get("victima"));
        assertEquals(misionCazador.toString(), datos.get("mision"));
        assertEquals(objetivoHeredado.toString(), datos.get("objetivo"));
        assertEquals(misionHeredada.toString(), datos.get("heredada"));
    }

    @Test
    void elUltimoVivoSeQuedaSinObjetivoNiMision() {
        Mesa mesa = preparar(3, true);
        arrancar(mesa).then().statusCode(200);
        Persona cazador = mesa.jugadores().get(0);
        UUID cazadorId = inscripcion(cazador, mesa.juego());

        UUID primera = objetivo(mesa.juego(), cazadorId);
        eliminar(cazador, mesa.juego(), codigoDe(mesa.juego(), primera, reloj.instant()))
                .then().statusCode(200);
        UUID segunda = objetivo(mesa.juego(), cazadorId);
        eliminar(cazador, mesa.juego(), codigoDe(mesa.juego(), segunda, reloj.instant()))
                .then().statusCode(200)
                .body("carta.estado", equalTo("VIVO"))
                .body("carta.aliasObjetivo", equalTo(null))
                .body("carta.mision", equalTo(null))
                .body("carta.eliminaciones", equalTo(2))
                .body("carta.intentosRestantes", nullValue())
                .body("carta.bloqueadoHasta", nullValue());

        assertNull(objetivo(mesa.juego(), cazadorId));
        assertNull(mision(mesa.juego(), cazadorId));
        assertEquals(2, jdbc.queryForObject("select count(*) from jugador_partida"
                + " where partida_id = ? and estado = 'ELIMINADO'", Integer.class,
                mesa.juego().id()));
        assertNull(jdbc.queryForObject("select datos->>'objetivoHeredadoId' from evento"
                + " where partida_id = ? and tipo = 'JUGADOR_ELIMINADO'"
                + " order by secuencia desc limit 1", String.class, mesa.juego().id()));
    }

    @Test
    void elCodigoIncorrectoNoEliminaYDescuentaIntentos() {
        Mesa mesa = preparar(3, true);
        arrancar(mesa).then().statusCode(200);
        Persona cazador = mesa.jugadores().get(0);
        UUID victimaId = objetivo(mesa.juego(), inscripcion(cazador, mesa.juego()));
        String correcto = codigoDe(mesa.juego(), victimaId, reloj.instant());

        eliminar(cazador, mesa.juego(), distintoDe(correcto))
                .then().statusCode(422)
                .body("codigo", equalTo("CODIGO_INCORRECTO"))
                .body("intentosRestantes", equalTo(2));

        assertEquals("VIVO", jdbc.queryForObject("select estado from jugador_partida"
                + " where partida_id = ? and inscripcion_id = ?", String.class,
                mesa.juego().id(), victimaId));
        assertEquals(1, jdbc.queryForObject("select intentos_fallidos from jugador_partida"
                + " where partida_id = ? and inscripcion_id = ?", Integer.class,
                mesa.juego().id(), inscripcion(cazador, mesa.juego())));

        eliminar(cazador, mesa.juego(), correcto).then().statusCode(200)
                .body("carta.eliminaciones", equalTo(1))
                .body("carta.intentosRestantes", equalTo(3))
                .body("carta.bloqueadoHasta", nullValue());
        assertEquals(0, jdbc.queryForObject("select intentos_fallidos from jugador_partida"
                + " where partida_id = ? and inscripcion_id = ?", Integer.class,
                mesa.juego().id(), inscripcion(cazador, mesa.juego())));
    }

    @Test
    void unCodigoDeLosUltimosMinutosRespondeCaducado() {
        Mesa mesa = preparar(3, true);
        arrancar(mesa).then().statusCode(200);
        Persona cazador = mesa.jugadores().get(0);
        UUID victimaId = objetivo(mesa.juego(), inscripcion(cazador, mesa.juego()));

        eliminar(cazador, mesa.juego(), codigoDe(mesa.juego(), victimaId,
                reloj.instant().minusSeconds(120)))
                .then().statusCode(422)
                .body("codigo", equalTo("CODIGO_CADUCADO"))
                .body("intentosRestantes", equalTo(2));
        assertEquals("VIVO", estadoJugador(victimaId));
        assertTrue(victimasRegistradas(mesa.juego()).isEmpty());
    }

    @Test
    void elCaducadoLlegaHastaDiezMinutosYDespuesEsIncorrecto() {
        Mesa mesa = preparar(3, true);
        arrancar(mesa).then().statusCode(200);
        Persona cazador = mesa.jugadores().get(0);
        UUID victimaId = objetivo(mesa.juego(), inscripcion(cazador, mesa.juego()));

        eliminar(cazador, mesa.juego(), codigoDe(mesa.juego(), victimaId,
                reloj.instant().minusSeconds(600)))
                .then().statusCode(422)
                .body("codigo", equalTo("CODIGO_CADUCADO"));
        eliminar(cazador, mesa.juego(), codigoDe(mesa.juego(), victimaId,
                reloj.instant().minusSeconds(660)))
                .then().statusCode(422)
                .body("codigo", equalTo("CODIGO_INCORRECTO"));
    }

    @Test
    void tresFallosBloqueanHastaQueRotaElCodigo() {
        Mesa mesa = preparar(3, true);
        arrancar(mesa).then().statusCode(200);
        Persona cazador = mesa.jugadores().get(0);
        UUID victimaId = objetivo(mesa.juego(), inscripcion(cazador, mesa.juego()));
        String correcto = codigoDe(mesa.juego(), victimaId, reloj.instant());
        String incorrecto = distintoDe(correcto);

        eliminar(cazador, mesa.juego(), incorrecto).then().statusCode(422)
                .body("intentosRestantes", equalTo(2));
        eliminar(cazador, mesa.juego(), incorrecto).then().statusCode(422)
                .body("intentosRestantes", equalTo(1));
        eliminar(cazador, mesa.juego(), incorrecto).then().statusCode(409)
                .body("codigo", equalTo("INTENTOS_BLOQUEADOS"))
                .body("bloqueadoHasta", notNullValue());
        assertEquals(0, jdbc.queryForObject("select intentos_fallidos from jugador_partida"
                + " where partida_id = ? and inscripcion_id = ?", Integer.class,
                mesa.juego().id(), inscripcion(cazador, mesa.juego())));
        assertTrue(jdbc.queryForObject("select bloqueado_hasta from jugador_partida"
                + " where partida_id = ? and inscripcion_id = ?", java.time.OffsetDateTime.class,
                mesa.juego().id(), inscripcion(cazador, mesa.juego()))
                .toInstant().isAfter(reloj.instant().minusSeconds(60)));

        eliminar(cazador, mesa.juego(), correcto).then().statusCode(409)
                .body("codigo", equalTo("INTENTOS_BLOQUEADOS"));
    }

    @Test
    void unCodigoDeOtroJugadorNoElimina() {
        Mesa mesa = preparar(4, true);
        arrancar(mesa).then().statusCode(200);
        Persona cazador = mesa.jugadores().get(0);
        UUID cazadorId = inscripcion(cazador, mesa.juego());
        UUID victimaId = objetivo(mesa.juego(), cazadorId);
        UUID ajeno = objetivo(mesa.juego(), victimaId);

        eliminar(cazador, mesa.juego(), codigoDe(mesa.juego(), ajeno, reloj.instant()))
                .then().statusCode(422)
                .body("codigo", equalTo("CODIGO_INCORRECTO"))
                .body("intentosRestantes", equalTo(2));

        assertEquals("VIVO", estadoJugador(victimaId));
        assertEquals(1, jdbc.queryForObject("select intentos_fallidos from jugador_partida"
                + " where partida_id = ? and inscripcion_id = ?", Integer.class,
                mesa.juego().id(), cazadorId));
    }

    @Test
    void elCodigoDeUnaVictimaYaEliminadaEsIncorrecto() {
        Mesa mesa = preparar(4, true);
        arrancar(mesa).then().statusCode(200);
        Persona cazador = mesa.jugadores().get(0);
        UUID cazadorId = inscripcion(cazador, mesa.juego());
        UUID victimaId = objetivo(mesa.juego(), cazadorId);
        String codigoVictima = codigoDe(mesa.juego(), victimaId, reloj.instant());
        eliminar(cazador, mesa.juego(), codigoVictima).then().statusCode(200);

        eliminar(cazador, mesa.juego(), codigoVictima)
                .then().statusCode(422)
                .body("codigo", equalTo("CODIGO_INCORRECTO"))
                .body("intentosRestantes", equalTo(2));

        assertEquals(1, jdbc.queryForObject("select count(*) from evento where partida_id = ?"
                + " and tipo = 'JUGADOR_ELIMINADO'", Integer.class, mesa.juego().id()));
        assertEquals(1, jdbc.queryForObject("select intentos_fallidos from jugador_partida"
                + " where partida_id = ? and inscripcion_id = ?", Integer.class,
                mesa.juego().id(), cazadorId));
    }

    @Test
    void laCartaLlevaLosIntentosQueQuedanYElBloqueo() {
        Mesa mesa = preparar(3, true);
        arrancar(mesa).then().statusCode(200);
        Persona cazador = mesa.jugadores().get(0);
        UUID victimaId = objetivo(mesa.juego(), inscripcion(cazador, mesa.juego()));
        String incorrecto = distintoDe(codigoDe(mesa.juego(), victimaId, reloj.instant()));

        carta(cazador, mesa.juego()).then().statusCode(200)
                .body("intentosRestantes", equalTo(3))
                .body("bloqueadoHasta", nullValue());
        eliminar(cazador, mesa.juego(), incorrecto).then().statusCode(422);
        carta(cazador, mesa.juego()).then().statusCode(200)
                .body("intentosRestantes", equalTo(2))
                .body("bloqueadoHasta", nullValue());
        eliminar(cazador, mesa.juego(), incorrecto).then().statusCode(422);
        carta(cazador, mesa.juego()).then().statusCode(200)
                .body("intentosRestantes", equalTo(1));
        eliminar(cazador, mesa.juego(), incorrecto).then().statusCode(409);

        Instant rotado = reloj.instant().truncatedTo(ChronoUnit.MINUTES).plusSeconds(60);
        carta(cazador, mesa.juego()).then().statusCode(200)
                .body("intentosRestantes", equalTo(3))
                .body("bloqueadoHasta", equalTo(rotado.toString()));
        when(reloj.instant()).thenReturn(rotado);
        carta(cazador, mesa.juego()).then().statusCode(200)
                .body("intentosRestantes", equalTo(3))
                .body("bloqueadoHasta", nullValue());
    }

    @Test
    void trasRotarElCodigoSeDesbloqueaYSePuedeEliminar() {
        Mesa mesa = preparar(3, true);
        arrancar(mesa).then().statusCode(200);
        Persona cazador = mesa.jugadores().get(0);
        UUID victimaId = objetivo(mesa.juego(), inscripcion(cazador, mesa.juego()));
        String incorrecto = distintoDe(codigoDe(mesa.juego(), victimaId, reloj.instant()));

        eliminar(cazador, mesa.juego(), incorrecto).then().statusCode(422);
        eliminar(cazador, mesa.juego(), incorrecto).then().statusCode(422);
        Instant rotado = reloj.instant().truncatedTo(ChronoUnit.MINUTES).plusSeconds(60);
        eliminar(cazador, mesa.juego(), incorrecto).then().statusCode(409)
                .body("codigo", equalTo("INTENTOS_BLOQUEADOS"))
                .body("bloqueadoHasta", equalTo(rotado.toString()));

        // Un segundo antes de la rotación el código correcto tampoco permite eliminar
        Instant antes = rotado.minusSeconds(1);
        when(reloj.instant()).thenReturn(antes);
        eliminar(cazador, mesa.juego(), codigoDe(mesa.juego(), victimaId, antes))
                .then().statusCode(409).body("codigo", equalTo("INTENTOS_BLOQUEADOS"));
        assertTrue(victimasRegistradas(mesa.juego()).isEmpty());

        when(reloj.instant()).thenReturn(rotado);
        eliminar(cazador, mesa.juego(), codigoDe(mesa.juego(), victimaId, rotado))
                .then().statusCode(200)
                .body("carta.eliminaciones", equalTo(1));
        assertEquals("ELIMINADO", estadoJugador(victimaId));
    }

    @Test
    void eliminacionesEnCadenaALaVezDejanElCicloCerrado() throws Exception {
        Mesa mesa = preparar(4, true);
        arrancar(mesa).then().statusCode(200);
        Persona a = mesa.jugadores().get(0);
        UUID aId = inscripcion(a, mesa.juego());
        UUID bId = objetivo(mesa.juego(), aId);
        UUID cId = objetivo(mesa.juego(), bId);
        UUID dId = objetivo(mesa.juego(), cId);
        Persona b = persona(mesa, bId);
        String codigoB = codigoDe(mesa.juego(), bId, reloj.instant());
        String codigoC = codigoDe(mesa.juego(), cId, reloj.instant());
        CyclicBarrier barrera = new CyclicBarrier(2);
        Callable<Response> deA = () -> {
            barrera.await();
            return eliminar(a, mesa.juego(), codigoB);
        };
        Callable<Response> deB = () -> {
            barrera.await();
            return eliminar(b, mesa.juego(), codigoC);
        };
        List<Response> respuestas = new ArrayList<>();

        try (var ejecutor = Executors.newFixedThreadPool(2)) {
            for (Future<Response> respuesta : ejecutor.invokeAll(List.of(deA, deB))) {
                respuestas.add(respuesta.get());
            }
        }

        respuestas.get(0).then().statusCode(200);
        List<UUID> victimas = victimasRegistradas(mesa.juego());
        assertEquals("ELIMINADO", estadoJugador(bId));
        if (respuestas.get(1).statusCode() == 200) {
            // B solo puede eliminar a C si su jugada quedó registrada antes que la de A
            assertEquals(List.of(cId, bId), victimas);
            assertEquals("ELIMINADO", estadoJugador(cId));
            assertEquals(dId, objetivo(mesa.juego(), aId));
        } else {
            respuestas.get(1).then().statusCode(409)
                    .body("codigo", equalTo("JUGADOR_ELIMINADO"));
            assertEquals(List.of(bId), victimas);
            assertEquals("VIVO", estadoJugador(cId));
            assertEquals(cId, objetivo(mesa.juego(), aId));
        }
        comprobarCicloCerrado(mesa.juego());
    }

    @Test
    void unCodigoSinSeisDigitosNoCuentaComoFallo() {
        Mesa mesa = preparar(3, true);
        arrancar(mesa).then().statusCode(200);
        Persona cazador = mesa.jugadores().get(0);

        eliminar(cazador, mesa.juego(), "12345").then().statusCode(400)
                .body("codigo", equalTo("DATOS_NO_VALIDOS"));
        assertEquals(0, jdbc.queryForObject("select intentos_fallidos from jugador_partida"
                + " where partida_id = ? and inscripcion_id = ?", Integer.class,
                mesa.juego().id(), inscripcion(cazador, mesa.juego())));
    }

    @Test
    void unJugadorEliminadoNoPuedeEliminar() {
        Mesa mesa = preparar(3, true);
        arrancar(mesa).then().statusCode(200);
        Persona cazador = mesa.jugadores().get(0);
        UUID victimaId = objetivo(mesa.juego(), inscripcion(cazador, mesa.juego()));

        eliminar(cazador, mesa.juego(), codigoDe(mesa.juego(), victimaId, reloj.instant()))
                .then().statusCode(200);
        Persona eliminado = mesa.jugadores().stream()
                .filter(persona -> victimaId.equals(inscripcion(persona, mesa.juego())))
                .findFirst().orElseThrow();
        eliminar(eliminado, mesa.juego(), "000000").then().statusCode(409)
                .body("codigo", equalTo("JUGADOR_ELIMINADO"));
    }

    @Test
    void elMasterQueNoJuegaYElAjenoReciben404() {
        Mesa mesa = preparar(3, false);
        arrancar(mesa).then().statusCode(200);
        Persona ajeno = registrar();

        for (Persona persona : List.of(mesa.master(), ajeno)) {
            eliminar(persona, mesa.juego(), "000000").then().statusCode(404)
                    .body("codigo", equalTo("NO_ENCONTRADA"));
        }
        eliminar(mesa.jugadores().get(0), new Juego(UUID.randomUUID(), ""), "000000")
                .then().statusCode(404).body("codigo", equalTo("NO_ENCONTRADA"));
    }

    @Test
    void fueraDeJuegoResponde409() {
        Mesa mesa = preparar(3, true);
        jdbc.update("update partida set estado = 'CANCELADA' where id = ?", mesa.juego().id());
        eliminar(mesa.jugadores().get(0), mesa.juego(), "000000").then().statusCode(409)
                .body("codigo", equalTo("PARTIDA_NO_EN_JUEGO"));
    }

    @Test
    void sinAutenticacionNoSePuedeEliminar() {
        Mesa mesa = preparar(3, true);
        arrancar(mesa).then().statusCode(200);
        given().port(puerto).contentType(ContentType.JSON).body(Map.of("codigo", "000000"))
                .post(RUTA_ELIMINAR, mesa.juego().id()).then().statusCode(401)
                .body("codigo", equalTo("NO_AUTENTICADO"));
    }

    @Test
    void laCartaDelEliminadoSigueSiendoSuyaYMarcadaComoEliminada() {
        Mesa mesa = preparar(3, true);
        arrancar(mesa).then().statusCode(200);
        Persona cazador = mesa.jugadores().get(0);
        UUID victimaId = objetivo(mesa.juego(), inscripcion(cazador, mesa.juego()));
        eliminar(cazador, mesa.juego(), codigoDe(mesa.juego(), victimaId, reloj.instant()))
                .then().statusCode(200);
        Persona eliminado = mesa.jugadores().stream()
                .filter(persona -> victimaId.equals(inscripcion(persona, mesa.juego())))
                .findFirst().orElseThrow();

        given().port(puerto).auth().oauth2(eliminado.token()).get(RUTA_CARTA, mesa.juego().id())
                .then().statusCode(200)
                .body("estado", equalTo("ELIMINADO"))
                .body("aliasObjetivo", equalTo(null))
                .body("mision", equalTo(null))
                .body("intentosRestantes", nullValue())
                .body("bloqueadoHasta", nullValue());
    }

    @Test
    void elCodigoAnteriorValeDuranteLosQuincePrimerosSegundosDelPaso() {
        Mesa mesa = preparar(3, true);
        arrancar(mesa).then().statusCode(200);
        Persona cazador = mesa.jugadores().get(0);
        UUID victimaId = objetivo(mesa.juego(), inscripcion(cazador, mesa.juego()));
        String anterior = codigoDe(mesa.juego(), victimaId, reloj.instant());
        Instant pasoNuevo = reloj.instant().truncatedTo(ChronoUnit.MINUTES).plusSeconds(60);

        // R11-13: el segundo 14 del paso nuevo todavía acepta el código anterior.
        when(reloj.instant()).thenReturn(pasoNuevo.plusSeconds(14));
        eliminar(cazador, mesa.juego(), anterior).then().statusCode(200);
    }

    @Test
    void pasadosLosQuinceSegundosElCodigoAnteriorEstaCaducado() {
        Mesa mesa = preparar(3, true);
        arrancar(mesa).then().statusCode(200);
        Persona cazador = mesa.jugadores().get(0);
        UUID victimaId = objetivo(mesa.juego(), inscripcion(cazador, mesa.juego()));
        String anterior = codigoDe(mesa.juego(), victimaId, reloj.instant());
        Instant pasoNuevo = reloj.instant().truncatedTo(ChronoUnit.MINUTES).plusSeconds(60);

        when(reloj.instant()).thenReturn(pasoNuevo.plusSeconds(15));
        eliminar(cazador, mesa.juego(), anterior).then().statusCode(422)
                .body("codigo", equalTo("CODIGO_CADUCADO"));
        assertEquals("VIVO", jdbc.queryForObject("select estado from jugador_partida"
                + " where partida_id = ? and inscripcion_id = ?", String.class,
                mesa.juego().id(), victimaId));
    }

    @Test
    void borrarLaCuentaConservaEstadoYEliminacionesYElAliasQuedaNulo() {
        Mesa mesa = preparar(4, false);
        arrancar(mesa).then().statusCode(200);
        Persona cazador = mesa.jugadores().get(0);
        UUID cazadorId = inscripcion(cazador, mesa.juego());
        UUID victimaId = objetivo(mesa.juego(), cazadorId);
        eliminar(cazador, mesa.juego(), codigoDe(mesa.juego(), victimaId, reloj.instant()))
                .then().statusCode(200);
        Persona victima = persona(mesa, victimaId);
        Persona perseguidor = persona(mesa, jdbc.queryForObject("select inscripcion_id"
                + " from jugador_partida where partida_id = ? and objetivo_id = ?", UUID.class,
                mesa.juego().id(), cazadorId));
        UUID perseguidorId = inscripcion(perseguidor, mesa.juego());
        eliminar(perseguidor, mesa.juego(), distintoDe(codigoDe(mesa.juego(),
                objetivo(mesa.juego(), perseguidorId), reloj.instant())))
                .then().statusCode(422);
        List<Map<String, Object>> antes = filas(mesa.juego());
        Persona restante = mesa.jugadores().stream()
                .filter(persona -> !List.of(cazadorId, victimaId, perseguidorId)
                        .contains(inscripcion(persona, mesa.juego())))
                .findFirst().orElseThrow();

        for (Persona persona : List.of(cazador, victima, perseguidor)) {
            given().port(puerto).auth().oauth2(persona.token()).delete("/api/v1/cuentas/yo")
                    .then().statusCode(204);
        }

        // Las filas no tienen datos de la persona: siguen con su estado, sus eliminaciones
        // y sus intentos fallidos.
        assertEquals(antes, filas(mesa.juego()));
        assertEquals(1, jdbc.queryForObject("select eliminaciones from jugador_partida"
                + " where inscripcion_id = ?", Integer.class, cazadorId));
        assertEquals(1, jdbc.queryForObject("select intentos_fallidos from jugador_partida"
                + " where inscripcion_id = ?", Integer.class, perseguidorId));
        assertEquals("ELIMINADO", jdbc.queryForObject("select estado from jugador_partida"
                + " where inscripcion_id = ?", String.class, victimaId));

        given().port(puerto).auth().oauth2(restante.token()).get(RUTA_CARTA, mesa.juego().id())
                .then().statusCode(200)
                .body("estado", equalTo("VIVO"))
                .body("aliasObjetivo", equalTo(null));
    }

    private void comprobarCicloCerrado(Juego juego) {
        Map<UUID, UUID> siguiente = new HashMap<>();
        for (Map<String, Object> fila : jdbc.queryForList("select inscripcion_id, estado,"
                + " objetivo_id from jugador_partida where partida_id = ?", juego.id())) {
            if ("VIVO".equals(fila.get("estado"))) {
                siguiente.put((UUID) fila.get("inscripcion_id"), (UUID) fila.get("objetivo_id"));
            } else {
                assertNull(fila.get("objetivo_id"));
            }
        }
        UUID inicio = siguiente.keySet().iterator().next();
        Set<UUID> vistos = new HashSet<>();
        UUID actual = inicio;
        do {
            assertTrue(vistos.add(actual));
            actual = siguiente.get(actual);
            assertNotNull(actual);
        } while (!actual.equals(inicio));
        assertEquals(siguiente.keySet(), vistos);
    }

    private Response carta(Persona persona, Juego juego) {
        return given().port(puerto).auth().oauth2(persona.token()).get(RUTA_CARTA, juego.id());
    }

    private String estadoJugador(UUID inscripcionId) {
        return jdbc.queryForObject("select estado from jugador_partida"
                + " where inscripcion_id = ?", String.class, inscripcionId);
    }

    private List<UUID> victimasRegistradas(Juego juego) {
        return jdbc.queryForList("select (datos->>'victimaId')::uuid from evento"
                + " where partida_id = ? and tipo = 'JUGADOR_ELIMINADO' order by secuencia",
                UUID.class, juego.id());
    }

    private Persona persona(Mesa mesa, UUID inscripcionId) {
        return mesa.jugadores().stream()
                .filter(jugador -> inscripcionId.equals(inscripcion(jugador, mesa.juego())))
                .findFirst().orElseThrow();
    }

    private List<Map<String, Object>> filas(Juego juego) {
        return jdbc.queryForList("select inscripcion_id, estado, objetivo_id, mision_id,"
                + " eliminaciones, intentos_fallidos, bloqueado_hasta from jugador_partida"
                + " where partida_id = ? order by inscripcion_id", juego.id());
    }

    private Mesa preparar(int jugadores, boolean masterJuega) {
        Persona master = registrar();
        Instant inicio = Instant.now().plus(Duration.ofDays(1));
        Response respuesta = given().port(puerto).auth().oauth2(master.token())
                .contentType(ContentType.JSON).body(Map.of("nombre", "Partida Eliminar",
                        "inicioEn", inicio.toString(), "finEn", inicio.plus(Duration.ofDays(7))
                                .toString(), "tipoMisiones", "BIBLIOTECA", "plazoDisputaHoras", 6,
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
        String alias = "e_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
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

    private Response eliminar(Persona persona, Juego juego, String codigo) {
        return given().port(puerto).auth().oauth2(persona.token()).contentType(ContentType.JSON)
                .body(Map.of("codigo", codigo)).post(RUTA_ELIMINAR, juego.id());
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

    private UUID mision(Juego juego, UUID inscripcionId) {
        return jdbc.queryForObject("select mision_id from jugador_partida"
                + " where partida_id = ? and inscripcion_id = ?", UUID.class,
                juego.id(), inscripcionId);
    }

    private String alias(UUID inscripcionId) {
        return jdbc.queryForObject("select c.alias from inscripcion i"
                + " join cuenta c on c.id = i.cuenta_id where i.id = ?", String.class,
                inscripcionId);
    }

    private String misionTexto(UUID misionId) {
        return jdbc.queryForObject("select texto from mision where id = ?", String.class, misionId);
    }

    private String codigoDe(Juego juego, UUID inscripcionId, Instant cuando) {
        byte[] secreto = jdbc.queryForObject("select secreto_codigo from jugador_partida"
                + " where partida_id = ? and inscripcion_id = ?", byte[].class,
                juego.id(), inscripcionId);
        return generador.generar(secreto, cuando).codigo();
    }

    private static String distintoDe(String codigo) {
        return codigo.equals("123456") ? "654321" : "123456";
    }

    private record Persona(UUID id, String token) {
    }

    private record Juego(UUID id, String codigo) {
    }

    private record Mesa(Persona master, Juego juego, List<Persona> jugadores) {
    }
}
