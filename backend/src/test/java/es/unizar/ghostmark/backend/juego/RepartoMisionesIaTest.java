package es.unizar.ghostmark.backend.juego;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import es.unizar.ghostmark.backend.TestcontainersConfiguration;
import es.unizar.ghostmark.backend.mision.GeneradorMisiones;
import es.unizar.ghostmark.backend.mision.GeneradorSimulado;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RepartoMisionesIaTest {

    @LocalServerPort
    int puerto;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    GeneradorMisiones generador;

    @AfterEach
    void restaurarGenerador() {
        ((GeneradorSimulado) generador).usar(GeneradorSimulado.Modo.NORMAL);
    }

    @ParameterizedTest
    @EnumSource(value = GeneradorSimulado.Modo.class, names = {"CAIDO", "LENTO"})
    void siFallaLaIaLaMismaPartidaArrancaConLaBiblioteca(GeneradorSimulado.Modo modo) {
        Mesa mesa = preparar(3, "IA");
        ((GeneradorSimulado) generador).usar(modo);

        pedirTandaFallida(mesa);
        // El máster puede arrancar la misma partida aunque el generador siga fallando
        arrancar(mesa);

        List<UUID> repartidas = repartidas(mesa.partidaId());
        assertEquals(3, repartidas.size());
        assertTrue(repartidas.stream().allMatch(this::deBiblioteca));
    }

    @Test
    void conIaSeRepartenPrimeroLasAprobadasYElRestoDeLaBiblioteca() {
        Mesa mesa = preparar(3, "IA");
        UUID primera = mision(mesa.partidaId(), "APROBADA");
        UUID segunda = mision(mesa.partidaId(), "APROBADA");
        UUID propuesta = mision(mesa.partidaId(), "PROPUESTA");
        UUID rechazada = mision(mesa.partidaId(), "RECHAZADA");

        // Si falla otra tanda, las aprobadas anteriores siguen disponibles para el reparto
        ((GeneradorSimulado) generador).usar(GeneradorSimulado.Modo.CAIDO);
        pedirTandaFallida(mesa);
        arrancar(mesa);

        List<UUID> repartidas = repartidas(mesa.partidaId());
        assertEquals(3, repartidas.size());
        assertTrue(repartidas.containsAll(List.of(primera, segunda)));
        assertFalse(repartidas.contains(propuesta));
        assertFalse(repartidas.contains(rechazada));
        UUID tercera = repartidas.stream().filter(id -> !id.equals(primera) && !id.equals(segunda))
                .findFirst().orElseThrow();
        assertTrue(deBiblioteca(tercera));
    }

    @Test
    void conBibliotecaNoSeUsanLasAprobadas() {
        Mesa mesa = preparar(3, "BIBLIOTECA");
        UUID aprobada = mision(mesa.partidaId(), "APROBADA");

        arrancar(mesa);

        List<UUID> repartidas = repartidas(mesa.partidaId());
        assertEquals(3, repartidas.size());
        assertFalse(repartidas.contains(aprobada));
        assertTrue(repartidas.stream().allMatch(this::deBiblioteca));
    }

    @Test
    void lasMisionesDeLaIaDeOtraPartidaNuncaSeReparten() {
        Mesa otra = preparar(3, "IA");
        Set<UUID> ajenas = new HashSet<>();
        for (int i = 0; i < 4; i++) {
            ajenas.add(mision(otra.partidaId(), "APROBADA"));
        }
        Mesa mesa = preparar(3, "IA");

        arrancar(mesa);

        List<UUID> repartidas = repartidas(mesa.partidaId());
        assertEquals(3, repartidas.size());
        assertTrue(repartidas.stream().noneMatch(ajenas::contains));
        assertTrue(repartidas.stream().allMatch(this::deBiblioteca));
    }

    @Test
    void laMigracionNoMezclaBibliotecaYMisionesDePartida() {
        Mesa mesa = preparar(1, "IA");
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "insert into mision (id, texto, activa, creada_en, tanda)"
                        + " values (gen_random_uuid(), ?, true, now(), 1)", texto()));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "insert into mision (id, texto, activa, creada_en, partida_id)"
                        + " values (gen_random_uuid(), ?, true, now(), ?)", texto(),
                mesa.partidaId()));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "insert into mision (id, texto, activa, creada_en, partida_id, estado, tanda)"
                        + " values (gen_random_uuid(), ?, true, now(), ?, 'APROBADA', 1)",
                texto(), mesa.partidaId()));
    }

    private UUID mision(UUID partidaId, String estado) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into mision (id, texto, activa, creada_en, partida_id, estado, tanda,"
                + " revisada_en) values (?, ?, true, now(), ?, ?, 1,"
                + " case when ? = 'PROPUESTA' then null else now() end)",
                id, texto(), partidaId, estado, estado);
        return id;
    }

    private String texto() {
        return "Que te diga una palabra secreta " + UUID.randomUUID();
    }

    private boolean deBiblioteca(UUID misionId) {
        return jdbc.queryForObject("select partida_id is null from mision where id = ?",
                Boolean.class, misionId);
    }

    private List<UUID> repartidas(UUID partidaId) {
        return jdbc.queryForList("select (a.valor->>'misionId')::uuid from evento e,"
                + " jsonb_array_elements(e.datos->'asignaciones') as a(valor)"
                + " where e.partida_id = ? and e.tipo = 'MISIONES_REPARTIDAS'",
                UUID.class, partidaId);
    }

    private Mesa preparar(int jugadores, String tipoMisiones) {
        String master = registrar();
        Instant inicio = Instant.now().plus(Duration.ofDays(1));
        Response respuesta = given().port(puerto).auth().oauth2(master)
                .contentType(ContentType.JSON).body(Map.of("nombre", "Reparto IA",
                        "inicioEn", inicio.toString(), "finEn", inicio.plus(Duration.ofDays(7))
                                .toString(), "tipoMisiones", tipoMisiones, "plazoDisputaHoras", 6,
                        "entradasTardias", false, "masterJuega", true))
                .post("/api/v1/partidas").then().statusCode(201).extract().response();
        Mesa mesa = new Mesa(master, UUID.fromString(respuesta.path("id")),
                respuesta.path("codigoInvitacion"));
        for (int i = 1; i < jugadores; i++) {
            given().port(puerto).auth().oauth2(registrar()).contentType(ContentType.JSON)
                    .body(Map.of("codigo", mesa.codigo(), "aceptaNormas", true))
                    .post("/api/v1/inscripciones").then().statusCode(201);
        }
        return mesa;
    }

    private void arrancar(Mesa mesa) {
        given().port(puerto).auth().oauth2(mesa.tokenMaster())
                .post("/api/v1/partidas/{id}/arranque", mesa.partidaId()).then().statusCode(200);
    }

    private void pedirTandaFallida(Mesa mesa) {
        given().port(puerto).auth().oauth2(mesa.tokenMaster()).contentType(ContentType.JSON)
                .body(Map.of("contexto", "Campus"))
                .post("/api/v1/partidas/{id}/misiones-ia/tandas", mesa.partidaId()).then()
                .statusCode(503).body("codigo", equalTo("IA_NO_DISPONIBLE"))
                .body("detail", equalTo("La IA no responde: se usarán misiones de la biblioteca"));
    }

    private String registrar() {
        String alias = "r_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return given().port(puerto).contentType(ContentType.JSON)
                .body(Map.of("alias", alias, "email", alias + "@example.com",
                        "contrasena", "fantasma2026"))
                .post("/api/v1/cuentas").then().statusCode(201).extract().path("tokenAcceso");
    }

    private record Mesa(String tokenMaster, UUID partidaId, String codigo) {
    }
}
