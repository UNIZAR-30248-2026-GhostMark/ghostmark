package es.unizar.ghostmark.backend.juego;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
            comprobarJugadores(mesa, reconstruido);
        } else {
            assertTrue(reconstruido.objetivos().isEmpty());
        }
    }

    @Test
    void elEstadoConEliminacionesSeReconstruyeDesdeLosEventos() {
        Mesa mesa = preparar(4);
        arrancar(mesa).then().statusCode(200);
        // El mismo cazador elimina dos veces seguidas: hereda dos veces y quedan dos vivos.
        Persona cazador = mesa.jugadores().get(1);
        UUID cazadorId = inscripcion(mesa, cazador);
        for (int i = 0; i < 2; i++) {
            UUID victimaId = jdbc.queryForObject("select objetivo_id from jugador_partida"
                    + " where inscripcion_id = ?", UUID.class, cazadorId);
            byte[] secreto = jdbc.queryForObject("select secreto_codigo from jugador_partida"
                    + " where inscripcion_id = ?", byte[].class, victimaId);
            given().port(puerto).auth().oauth2(cazador.token()).contentType(ContentType.JSON)
                    .body(Map.of("codigo", new GeneradorCodigoVida()
                            .generar(secreto, Instant.now()).codigo()))
                    .post("/api/v1/partidas/{id}/eliminaciones", mesa.id())
                    .then().statusCode(200);
        }

        EstadoReconstruido reconstruido = reproducir(mesa);
        assertEquals("EN_JUEGO", reconstruido.estado());
        assertEquals(2, reconstruido.eliminados().size());
        assertEquals(2, reconstruido.eliminaciones().get(cazadorId));
        comprobarJugadores(mesa, reconstruido);
        Set<UUID> vivos = new HashSet<>(reconstruido.inscritos());
        vivos.removeAll(reconstruido.eliminados());
        comprobarCiclo(reconstruido.objetivos(), vivos);
    }

    private EstadoReconstruido reproducir(Mesa mesa) {
        // Este intérprete de prueba parte de cero; no toma el estado actual de partida.
        String reconstruido = null;
        Set<UUID> inscritos = new LinkedHashSet<>();
        Map<UUID, UUID> objetivos = new HashMap<>();
        Map<UUID, UUID> misiones = new HashMap<>();
        Set<UUID> eliminados = new HashSet<>();
        Map<UUID, Integer> eliminaciones = new HashMap<>();
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
                // El reparto de la PBI 5 no cambia el estado ni el ciclo: da la misión de cada uno.
                case "MISIONES_REPARTIDAS" -> {
                    assertEquals("EN_JUEGO", reconstruido);
                    List<String> jugadores = datos.getList("asignaciones.inscripcionId");
                    List<String> asignadas = datos.getList("asignaciones.misionId");
                    for (int i = 0; i < jugadores.size(); i++) {
                        misiones.put(UUID.fromString(jugadores.get(i)),
                                UUID.fromString(asignadas.get(i)));
                    }
                }
                // PBI 7a: la víctima sale del ciclo y su cazador hereda objetivo y misión.
                case "JUGADOR_ELIMINADO" -> {
                    assertEquals("EN_JUEGO", reconstruido);
                    UUID cazador = UUID.fromString(datos.getString("cazadorId"));
                    UUID victima = UUID.fromString(datos.getString("victimaId"));
                    assertEquals(victima, objetivos.get(cazador));
                    assertEquals(misiones.get(cazador), uuid(datos.getString("misionId")));
                    UUID heredado = cazador.equals(objetivos.get(victima)) ? null
                            : objetivos.get(victima);
                    assertEquals(heredado, uuid(datos.getString("objetivoHeredadoId")));
                    assertEquals(heredado == null ? null : misiones.get(victima),
                            uuid(datos.getString("misionHeredadaId")));
                    objetivos.remove(victima);
                    misiones.remove(victima);
                    assertTrue(eliminados.add(victima));
                    objetivos.put(cazador, heredado);
                    misiones.put(cazador, uuid(datos.getString("misionHeredadaId")));
                    eliminaciones.merge(cazador, 1, Integer::sum);
                }
                default -> fail("Evento no previsto en el recorrido de las PBI 4 y 7a: "
                        + evento.get("tipo"));
            }
        }
        return new EstadoReconstruido(reconstruido, inscritos, objetivos, misiones, eliminados,
                eliminaciones);
    }

    private void comprobarJugadores(Mesa mesa, EstadoReconstruido reconstruido) {
        // Desde la PBI 7a, jugador_partida tiene que coincidir con lo que dicen los eventos.
        List<Map<String, Object>> filas = jdbc.queryForList("select inscripcion_id, estado,"
                + " objetivo_id, mision_id, eliminaciones from jugador_partida"
                + " where partida_id = ?", mesa.id());
        assertEquals(reconstruido.inscritos().size(), filas.size());
        for (Map<String, Object> fila : filas) {
            UUID id = (UUID) fila.get("inscripcion_id");
            assertTrue(reconstruido.inscritos().contains(id));
            assertEquals(reconstruido.eliminados().contains(id) ? "ELIMINADO" : "VIVO",
                    fila.get("estado"));
            assertEquals(reconstruido.objetivos().get(id), fila.get("objetivo_id"));
            assertEquals(reconstruido.misiones().get(id), fila.get("mision_id"));
            assertEquals(reconstruido.eliminaciones().getOrDefault(id, 0),
                    ((Number) fila.get("eliminaciones")).intValue());
            if (reconstruido.eliminados().contains(id)) {
                assertNull(fila.get("objetivo_id"));
            }
        }
    }

    private static UUID uuid(String valor) {
        return valor == null ? null : UUID.fromString(valor);
    }

    private UUID inscripcion(Mesa mesa, Persona persona) {
        return jdbc.queryForObject("select id from inscripcion where partida_id = ?"
                + " and cuenta_id = ?", UUID.class, mesa.id(), persona.id());
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
        return new Persona(UUID.fromString(respuesta.path("cuenta.id")),
                respuesta.path("tokenAcceso"));
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

    private record Persona(UUID id, String token) {
    }

    private record Mesa(UUID id, Persona master, List<Persona> jugadores) {
    }

    private record EstadoReconstruido(String estado, Set<UUID> inscritos,
            Map<UUID, UUID> objetivos, Map<UUID, UUID> misiones, Set<UUID> eliminados,
            Map<UUID, Integer> eliminaciones) {
    }
}
