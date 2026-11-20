package es.unizar.ghostmark.backend.juego;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import es.unizar.ghostmark.backend.TestcontainersConfiguration;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MisionesAnulacionTest {

    @LocalServerPort
    int puerto;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ServicioMisionesAnulacion misiones;

    @Autowired
    PlatformTransactionManager transacciones;

    @Test
    void reasignaAlCazadorYRevividoSinTocarLasMisionesDeLosDemas() {
        Mesa mesa = preparar(4);
        List<UUID> ids = ids(mesa);
        UUID cazador = ids.get(0);
        UUID revivido = ids.get(1);
        UUID noImplicado = ids.get(2);
        UUID anterior = mision(cazador);
        UUID conservada = mision(noImplicado);
        jdbc.update("update jugador_partida set estado = 'ELIMINADO',"
                + " mision_id = null where inscripcion_id = ?", revivido);
        jdbc.update("update jugador_partida set estado = 'VIVO'"
                + " where inscripcion_id = ?", revivido);

        List<ServicioMisionesAnulacion.Asignacion> cambios = new TransactionTemplate(transacciones)
                .execute(estado -> misiones.sustituir(mesa.partidaId(), cazador,
                        List.of(revivido)));
        assertEquals(2, cambios.size());
        assertEquals(List.of(cazador, revivido), cambios.stream()
                .map(ServicioMisionesAnulacion.Asignacion::inscripcionId).toList());
        assertNotEquals(anterior, mision(cazador));
        assertNotEquals(mision(cazador), mision(revivido));
        assertNotEquals(conservada, mision(cazador));
        assertNotEquals(conservada, mision(revivido));
        assertEquals(conservada, mision(noImplicado));
        for (var cambio : cambios) {
            assertTrue(jdbc.queryForObject("select exists(select 1 from mision"
                    + " where id = ? and activa and partida_id is null)",
                    Boolean.class, cambio.misionId()));
        }
    }

    @Test
    void conUnaMisionLibreElCazadorLaTomaYNoLaDeOtroVivo() {
        Mesa mesa = preparar(3);
        List<UUID> ids = ids(mesa);
        UUID cazador = ids.get(0);
        UUID revivido = ids.get(1);
        UUID otro = ids.get(2);
        UUID delCazador = mision(cazador);
        UUID delOtro = mision(otro);
        UUID libre = jdbc.queryForObject("select id from mision where activa"
                + " and partida_id is null and id not in (?, ?, ?) limit 1", UUID.class,
                delCazador, delOtro, mision(revivido));

        // Con solo tres misiones activas, si no se reservara la del otro vivo el cazador
        // la sacaría la mitad de las veces: diez vueltas lo dejan a la vista.
        conBiblioteca(List.of(delCazador, delOtro, libre), () -> {
            for (int vez = 0; vez < 10; vez++) {
                jdbc.update("update jugador_partida set mision_id = ?"
                        + " where inscripcion_id = ?", delCazador, cazador);
                revivirSinMision(revivido);
                sustituir(mesa, cazador, revivido);
                assertEquals(libre, mision(cazador));
                assertEquals(delCazador, mision(revivido));
                assertEquals(delOtro, mision(otro));
            }
        });
    }

    @Test
    void sinMisionesLibresSeRepiteLaDeOtroVivo() {
        Mesa mesa = preparar(3);
        List<UUID> ids = ids(mesa);
        UUID cazador = ids.get(0);
        UUID revivido = ids.get(1);
        UUID otro = ids.get(2);
        UUID delCazador = mision(cazador);
        UUID delOtro = mision(otro);

        conBiblioteca(List.of(delCazador, delOtro), () -> {
            revivirSinMision(revivido);
            sustituir(mesa, cazador, revivido);
            assertEquals(delOtro, mision(cazador));
            assertEquals(delCazador, mision(revivido));
            assertEquals(delOtro, mision(otro));
        });
    }

    @Test
    void elFeedPublicaUnaEntradaPorRevividoSinRevelarCazador() {
        Mesa mesa = preparar(4);
        List<UUID> ids = ids(mesa);
        UUID victima = ids.get(1);
        UUID otra = ids.get(2);
        Instant momento = Instant.now();
        String datos = "{\"eventoEliminacionId\":\"" + UUID.randomUUID()
                + "\",\"revividos\":[\"" + victima + "\",\"" + otra + "\"]}";
        jdbc.update("""
                insert into evento (id, partida_id, secuencia, tipo, datos, ocurrido_en)
                select ?, ?, coalesce(max(secuencia), 0) + 1, 'ELIMINACION_ANULADA',
                       ?::jsonb, ? from evento where partida_id = ?
                """, UUID.randomUUID(), mesa.partidaId(), datos,
                OffsetDateTime.ofInstant(momento, ZoneOffset.UTC), mesa.partidaId());

        for (Persona persona : mesa.personas()) {
            List<Map<String, Object>> entradas = given().port(puerto)
                    .auth().oauth2(persona.token()).get("/api/v1/partidas/{id}/feed",
                            mesa.partidaId()).then().statusCode(200)
                    .extract().jsonPath().getList("");
            List<Map<String, Object>> anuladas = entradas.stream()
                    .filter(item -> "ANULACION".equals(item.get("tipo"))).toList();
            assertEquals(2, anuladas.size());
            assertEquals(Set.of(alias(victima), alias(otra)),
                    Set.of(anuladas.get(0).get("alias"), anuladas.get(1).get("alias")));
            assertEquals(Set.of("tipo", "alias", "ocurridoEn"),
                    anuladas.get(0).keySet());
        }
    }

    @Test
    void siNoHayRevividosNoHayEntradasDeAnulacion() {
        Mesa mesa = preparar(3);
        jdbc.update("""
                insert into evento (id, partida_id, secuencia, tipo, datos, ocurrido_en)
                select ?, ?, coalesce(max(secuencia), 0) + 1, 'ELIMINACION_ANULADA',
                       '{"revividos":[]}'::jsonb, now()
                from evento where partida_id = ?
                """, UUID.randomUUID(), mesa.partidaId(), mesa.partidaId());
        List<Map<String, Object>> entradas = given().port(puerto)
                .auth().oauth2(mesa.personas().get(0).token())
                .get("/api/v1/partidas/{id}/feed", mesa.partidaId())
                .then().statusCode(200).extract().jsonPath().getList("");
        assertTrue(entradas.stream().noneMatch(x -> "ANULACION".equals(x.get("tipo"))));
    }

    private void conBiblioteca(List<UUID> activas, Runnable prueba) {
        List<UUID> apagadas = jdbc.queryForList("select id from mision"
                + " where activa and partida_id is null", UUID.class).stream()
                .filter(id -> !activas.contains(id)).toList();
        apagadas.forEach(id -> jdbc.update("update mision set activa = false where id = ?", id));
        try {
            prueba.run();
        } finally {
            apagadas.forEach(id -> jdbc.update("update mision set activa = true where id = ?",
                    id));
        }
    }

    private void revivirSinMision(UUID inscripcionId) {
        jdbc.update("update jugador_partida set estado = 'VIVO', mision_id = null"
                + " where inscripcion_id = ?", inscripcionId);
    }

    private void sustituir(Mesa mesa, UUID cazador, UUID revivido) {
        new TransactionTemplate(transacciones).executeWithoutResult(estado ->
                misiones.sustituir(mesa.partidaId(), cazador, List.of(revivido)));
    }

    private Mesa preparar(int jugadores) {
        List<Persona> personas = new ArrayList<>();
        Persona master = registrar();
        personas.add(master);
        Instant inicio = Instant.now().plus(Duration.ofDays(1));
        Response creada = given().port(puerto).auth().oauth2(master.token())
                .contentType(ContentType.JSON)
                .body(Map.of("nombre", "Prueba misiones de anulacion",
                        "inicioEn", inicio.toString(),
                        "finEn", inicio.plus(Duration.ofDays(7)).toString(),
                        "tipoMisiones", "BIBLIOTECA", "plazoDisputaHoras", 6,
                        "entradasTardias", false, "masterJuega", true))
                .post("/api/v1/partidas").then().statusCode(201).extract().response();
        UUID partidaId = UUID.fromString(creada.path("id"));
        String codigo = creada.path("codigoInvitacion");
        while (personas.size() < jugadores) {
            Persona persona = registrar();
            given().port(puerto).auth().oauth2(persona.token())
                    .contentType(ContentType.JSON)
                    .body(Map.of("codigo", codigo, "aceptaNormas", true))
                    .post("/api/v1/inscripciones").then().statusCode(201);
            personas.add(persona);
        }
        given().port(puerto).auth().oauth2(master.token())
                .post("/api/v1/partidas/{id}/arranque", partidaId)
                .then().statusCode(200);
        return new Mesa(partidaId, personas);
    }

    private Persona registrar() {
        String alias = "an_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Response creada = given().port(puerto).contentType(ContentType.JSON)
                .body(Map.of("alias", alias, "email", alias + "@example.com",
                        "contrasena", "fantasma2026"))
                .post("/api/v1/cuentas").then().statusCode(201).extract().response();
        return new Persona(UUID.fromString(creada.path("cuenta.id")),
                creada.path("tokenAcceso"));
    }

    private List<UUID> ids(Mesa mesa) {
        return mesa.personas().stream().map(p -> jdbc.queryForObject(
                "select id from inscripcion where partida_id = ? and cuenta_id = ?",
                UUID.class, mesa.partidaId(), p.cuentaId())).toList();
    }

    private UUID mision(UUID inscripcionId) {
        return jdbc.queryForObject("select mision_id from jugador_partida"
                + " where inscripcion_id = ?", UUID.class, inscripcionId);
    }

    private String alias(UUID inscripcionId) {
        return jdbc.queryForObject("select c.alias from inscripcion i"
                + " join cuenta c on c.id = i.cuenta_id where i.id = ?", String.class,
                inscripcionId);
    }

    private record Persona(UUID cuentaId, String token) {
    }

    private record Mesa(UUID partidaId, List<Persona> personas) {
    }
}
