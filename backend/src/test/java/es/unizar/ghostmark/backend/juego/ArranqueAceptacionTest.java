package es.unizar.ghostmark.backend.juego;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import es.unizar.ghostmark.backend.TestcontainersConfiguration;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Aceptación de la PBI 4 (#5): arranque, ciclo, cancelación y reproducción de eventos (#65).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ArranqueAceptacionTest {

    @LocalServerPort
    int puerto;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TareaArranque tarea;

    @Test
    void criterio1ArrancaSolaEnLaFechaDeInicio() {
        Mesa mesa = preparar(3);
        // Se invoca la tarea real sin esperar al temporizador ni dormir el test.
        tarea.revisar(Instant.now());
        assertEquals("EN_INSCRIPCION", estado(mesa));
        assertEquals(0, contarEventos(mesa, "PARTIDA_ARRANCADA"));

        vencer(mesa);
        tarea.revisar(Instant.now());
        assertEquals("EN_JUEGO", estado(mesa));
        assertEquals(List.of("PROGRAMADO"), modosDeArranque(mesa));
        resumen(mesa.master()).then().statusCode(200)
                .body("find { it.id == '" + mesa.id() + "' }.estado", equalTo("EN_JUEGO"));
    }

    @Test
    void criterio1ElMasterPuedeAdelantarElArranque() {
        Mesa mesa = preparar(3);
        // La fecha permanece en el futuro: el arranque tiene que proceder de esta petición.
        arrancar(mesa).then().statusCode(200).body("estado", equalTo("EN_JUEGO"));
        assertEquals("EN_JUEGO", estado(mesa));
        assertEquals(List.of("ADELANTADO"), modosDeArranque(mesa));
    }

    @ParameterizedTest
    @ValueSource(ints = {3, 4, 10})
    void criterio2CadaJugadorTieneUnObjetivoYUnPerseguidorEnUnSoloCiclo(int jugadores) {
        Mesa mesa = preparar(jugadores);
        arrancar(mesa).then().statusCode(200);
        List<UUID> orden = ordenDelCiclo(mesa);
        Set<UUID> inscritos = inscripciones(mesa);
        assertEquals(jugadores, orden.size());
        assertEquals(jugadores, new HashSet<>(orden).size());
        assertEquals(inscritos, new HashSet<>(orden));
        comprobarCiclo(objetivos(orden), inscritos);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void criterio3ConMenosDeTresSeCancelaYLosInscritosLaVenAlConsultar(int jugadores) {
        Mesa mesa = preparar(jugadores);
        vencer(mesa);
        // Es la consulta de Inicio la que aplica el vencimiento si el servidor estuvo dormido.
        resumen(mesa.master()).then().statusCode(200)
                .body("find { it.id == '" + mesa.id() + "' }.estado", equalTo("CANCELADA"))
                .body("find { it.id == '" + mesa.id() + "' }.motivoCancelacion",
                        equalTo("POCOS_JUGADORES"));
        assertEquals("CANCELADA", estado(mesa));
        assertEquals(0, contarEventos(mesa, "PARTIDA_ARRANCADA"));
        assertEquals(0, contarEventos(mesa, "CICLO_FORMADO"));
        assertEquals(jugadores, jdbc.queryForObject("select (datos->>'numJugadores')::int"
                + " from evento where partida_id = ? and tipo = 'PARTIDA_CANCELADA'",
                Integer.class, mesa.id()));
        for (Persona jugador : mesa.jugadores()) {
            resumen(jugador).then().statusCode(200)
                    .body("find { it.id == '" + mesa.id() + "' }.estado", equalTo("CANCELADA"))
                    .body("find { it.id == '" + mesa.id() + "' }.motivoCancelacion",
                            equalTo("POCOS_JUGADORES"));
        }
        // Volver a revisar una partida cancelada no debe duplicar el evento.
        tarea.revisar(Instant.now());
        assertEquals(1, contarEventos(mesa, "PARTIDA_CANCELADA"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"EN_INSCRIPCION", "EN_JUEGO", "CANCELADA"})
    void elEstadoSeReconstruyeDesdeLosEventosEnOrden(String destino) {
        Mesa mesa = preparar("CANCELADA".equals(destino) ? 2 : 4);
        switch (destino) {
            case "EN_JUEGO" -> arrancar(mesa).then().statusCode(200);
            case "CANCELADA" -> {
                vencer(mesa);
                tarea.revisar(Instant.now());
            }
            default -> {
                // También se reproduce una retirada; contar solo las altas daría un inscrito extra.
                Persona retirada = mesa.jugadores().getLast();
                given().port(puerto).auth().oauth2(retirada.token())
                        .delete("/api/v1/cuentas/yo").then().statusCode(204);
            }
        }
        EstadoReconstruido reconstruido = reproducir(mesa);
        assertEquals(destino, reconstruido.estado());
        assertEquals(estado(mesa), reconstruido.estado());
        assertEquals(inscripciones(mesa), reconstruido.inscritos());
        resumen(mesa.master()).then().statusCode(200)
                .body("find { it.id == '" + mesa.id() + "' }.estado",
                        equalTo(reconstruido.estado()))
                .body("find { it.id == '" + mesa.id() + "' }.numInscritos",
                        equalTo(reconstruido.inscritos().size()));
        if ("EN_JUEGO".equals(destino)) {
            comprobarCiclo(reconstruido.objetivos(), reconstruido.inscritos());
        } else {
            assertTrue(reconstruido.objetivos().isEmpty());
        }
    }

    private EstadoReconstruido reproducir(Mesa mesa) {
        // Este intérprete de prueba parte de cero; no toma el estado actual de partida.
        String reconstruido = null;
        Set<UUID> inscritos = new LinkedHashSet<>();
        Map<UUID, UUID> objetivos = new HashMap<>();
        long secuencia = 1;
        List<Map<String, Object>> eventos = jdbc.queryForList("select secuencia, tipo,"
                + " datos::text as datos from evento where partida_id = ? order by secuencia",
                mesa.id());
        assertFalse(eventos.isEmpty());
        for (Map<String, Object> evento : eventos) {
            // Detecta huecos o un orden incorrecto, aunque el estado final coincidiera.
            assertEquals(secuencia++, ((Number) evento.get("secuencia")).longValue());
            JsonPath datos = JsonPath.from((String) evento.get("datos"));
            switch ((String) evento.get("tipo")) {
                case "PARTIDA_CREADA" -> reconstruido = "EN_INSCRIPCION";
                case "JUGADOR_INSCRITO" -> assertTrue(inscritos.add(
                        UUID.fromString(datos.getString("inscripcionId"))));
                case "INSCRIPCION_RETIRADA" -> assertTrue(inscritos.remove(
                        UUID.fromString(datos.getString("inscripcionId"))));
                case "PARTIDA_ARRANCADA" -> {
                    assertEquals("EN_INSCRIPCION", reconstruido);
                    assertEquals(inscritos.size(), datos.getInt("numJugadores"));
                    reconstruido = "EN_JUEGO";
                }
                case "CICLO_FORMADO" -> {
                    assertEquals("EN_JUEGO", reconstruido);
                    objetivos.putAll(objetivos(datos.getList("orden", String.class).stream()
                            .map(UUID::fromString).toList()));
                }
                case "PARTIDA_CANCELADA" -> {
                    assertEquals("EN_INSCRIPCION", reconstruido);
                    assertEquals(inscritos.size(), datos.getInt("numJugadores"));
                    reconstruido = "CANCELADA";
                }
                // Si la PBI 5 ya está integrada, su reparto no cambia el estado ni el ciclo.
                case "MISIONES_REPARTIDAS" -> assertEquals("EN_JUEGO", reconstruido);
                default -> fail("Evento no previsto en el recorrido de la PBI 4: "
                        + evento.get("tipo"));
            }
        }
        return new EstadoReconstruido(reconstruido, inscritos, objetivos);
    }

    private void comprobarCiclo(Map<UUID, UUID> objetivos, Set<UUID> inscritos) {
        assertEquals(inscritos, objetivos.keySet());
        // Un objetivo por jugador no basta: todos deben tener también un único perseguidor.
        assertEquals(inscritos.size(), new HashSet<>(objetivos.values()).size());
        assertEquals(inscritos, new HashSet<>(objetivos.values()));
        UUID primero = inscritos.iterator().next();
        UUID actual = primero;
        Set<UUID> visitados = new HashSet<>();
        for (int i = 0; i < inscritos.size(); i++) {
            assertTrue(visitados.add(actual),
                    "El ciclo se ha cerrado antes de recorrerlos a todos");
            UUID siguiente = objetivos.get(actual);
            assertNotEquals(actual, siguiente, "Nadie puede perseguirse a sí mismo");
            actual = siguiente;
        }
        // Esta vuelta completa descarta que existan varios ciclos pequeños independientes.
        assertEquals(primero, actual);
        assertEquals(inscritos, visitados);
    }

    private Map<UUID, UUID> objetivos(List<UUID> orden) {
        Map<UUID, UUID> resultado = new HashMap<>();
        for (int i = 0; i < orden.size(); i++) {
            resultado.put(orden.get(i), orden.get((i + 1) % orden.size()));
        }
        return resultado;
    }

    private Mesa preparar(int cantidad) {
        Persona master = registrar();
        Instant inicio = Instant.now().plus(Duration.ofDays(1));
        Response creada = given().port(puerto).auth().oauth2(master.token())
                .contentType(ContentType.JSON).body(Map.of("nombre", "Aceptación PBI 4",
                        "inicioEn", inicio.toString(), "finEn", inicio.plus(Duration.ofDays(7))
                                .toString(), "tipoMisiones", "BIBLIOTECA", "plazoDisputaHoras", 6,
                        "entradasTardias", false, "masterJuega", cantidad > 0))
                .post("/api/v1/partidas").then().statusCode(201).extract().response();
        List<Persona> jugadores = new ArrayList<>();
        if (cantidad > 0) {
            jugadores.add(master);
        }
        while (jugadores.size() < cantidad) {
            Persona jugador = registrar();
            given().port(puerto).auth().oauth2(jugador.token()).contentType(ContentType.JSON)
                    .body(Map.of("codigo", creada.<String>path("codigoInvitacion"),
                            "aceptaNormas", true))
                    .post("/api/v1/inscripciones").then().statusCode(201);
            jugadores.add(jugador);
        }
        return new Mesa(UUID.fromString(creada.path("id")), master, jugadores);
    }

    private Persona registrar() {
        String alias = "a4_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Response respuesta = given().port(puerto).contentType(ContentType.JSON)
                .body(Map.of("alias", alias, "email", alias + "@example.com",
                        "contrasena", "fantasma2026"))
                .post("/api/v1/cuentas").then().statusCode(201).extract().response();
        return new Persona(respuesta.path("tokenAcceso"));
    }

    private Response arrancar(Mesa mesa) {
        return given().port(puerto).auth().oauth2(mesa.master().token())
                .post("/api/v1/partidas/{id}/arranque", mesa.id());
    }

    private Response resumen(Persona persona) {
        return given().port(puerto).auth().oauth2(persona.token())
                .get("/api/v1/cuentas/yo/partidas");
    }

    private void vencer(Mesa mesa) {
        // Solo se ajusta el reloj de la partida de prueba; la transición la ejecuta el servidor.
        jdbc.update("update partida set inicio_en = now() - interval '1 minute' where id = ?",
                mesa.id());
    }

    private String estado(Mesa mesa) {
        return jdbc.queryForObject("select estado from partida where id = ?",
                String.class, mesa.id());
    }

    private int contarEventos(Mesa mesa, String tipo) {
        return jdbc.queryForObject("select count(*) from evento where partida_id = ? and tipo = ?",
                Integer.class, mesa.id(), tipo);
    }

    private List<String> modosDeArranque(Mesa mesa) {
        return jdbc.queryForList("select datos->>'modo' from evento where partida_id = ?"
                + " and tipo = 'PARTIDA_ARRANCADA'", String.class, mesa.id());
    }

    private List<UUID> ordenDelCiclo(Mesa mesa) {
        return jdbc.queryForList("select jugador.id::uuid from evento e,"
                + " jsonb_array_elements_text(e.datos->'orden') with ordinality as jugador(id, pos)"
                + " where e.partida_id = ? and e.tipo = 'CICLO_FORMADO' order by jugador.pos",
                UUID.class, mesa.id());
    }

    private Set<UUID> inscripciones(Mesa mesa) {
        return new HashSet<>(jdbc.queryForList("select id from inscripcion where partida_id = ?",
                UUID.class, mesa.id()));
    }

    private record Persona(String token) {
    }

    private record Mesa(UUID id, Persona master, List<Persona> jugadores) {
    }

    private record EstadoReconstruido(String estado, Set<UUID> inscritos,
            Map<UUID, UUID> objetivos) {
    }
}
