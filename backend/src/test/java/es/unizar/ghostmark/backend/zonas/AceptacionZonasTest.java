package es.unizar.ghostmark.backend.zonas;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import es.unizar.ghostmark.backend.TestcontainersConfiguration;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
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

/** Aceptacion de la PBI 10a (#10): zonas persistentes, compartidas y congeladas (#284). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AceptacionZonasTest {

    private static final String RUTA = "/api/v1/partidas/{id}/zonas";

    @LocalServerPort
    int puerto;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void criterio1ElMasterCreaUnaZonaYQuedaGuardadaEnLaPartida() {
        String master = registrar();
        Juego juego = crear(master, false);

        Response guardada = guardar(master, juego, List.of(zona("Biblioteca", cuadrado())))
                .then().statusCode(200).body("zonas", hasSize(1))
                .body("zonas[0].nombre", equalTo("Biblioteca"))
                .body("zonas[0].puntos", hasSize(4)).extract().response();
        String id = guardada.path("zonas[0].id");
        UUID.fromString(id);

        ver(master, juego).then().statusCode(200)
                .body("zonas", hasSize(1)).body("zonas[0].id", equalTo(id));
        assertEquals(1, contar(juego));
        assertEquals(1, contarEventos(juego));
    }

    @Test
    void criterio1ElMasterModificaNombreYPoligonoConservandoLaZona() {
        String master = registrar();
        Juego juego = crear(master, false);
        String id = guardar(master, juego, List.of(zona("Aulario", cuadrado())))
                .then().statusCode(200).extract().path("zonas[0].id");

        guardar(master, juego, List.of(zonaConId(id, "Aulario Norte", cuadradoMovido())))
                .then().statusCode(200)
                .body("zonas", hasSize(1))
                .body("zonas[0].id", equalTo(id))
                .body("zonas[0].nombre", equalTo("Aulario Norte"));
        Response actual = ver(master, juego).then().statusCode(200).extract().response();
        assertEquals(id, actual.path("zonas[0].id"));
        Number lat = actual.path("zonas[0].puntos[0].lat");
        Number lon = actual.path("zonas[0].puntos[0].lon");
        assertEquals(41.70, lat.doubleValue(), 0.000001);
        assertEquals(-0.90, lon.doubleValue(), 0.000001);
        assertEquals(1, contar(juego));
        assertEquals(2, contarEventos(juego));
    }

    @Test
    void criterio1ElMasterBorraUnaZonaYPuedeBorrarlasTodas() {
        String master = registrar();
        Juego juego = crear(master, false);
        Response creadas = guardar(master, juego, List.of(
                zona("Aulario", cuadrado()), zona("Biblioteca", cuadradoMovido())))
                .then().statusCode(200).body("zonas", hasSize(2)).extract().response();
        List<Map<String, Object>> zonas = creadas.jsonPath().getList("zonas");
        String conservada = (String) zonas.get(1).get("id");

        guardar(master, juego, List.of(zonaConId(
                conservada, "Biblioteca", cuadradoMovido())))
                .then().statusCode(200).body("zonas", hasSize(1))
                .body("zonas[0].id", equalTo(conservada));
        ver(master, juego).then().statusCode(200).body("zonas", hasSize(1));
        assertEquals(1, contar(juego));

        guardar(master, juego, List.of()).then().statusCode(200).body("zonas", hasSize(0));
        ver(master, juego).then().statusCode(200).body("zonas", hasSize(0));
        assertEquals(0, contar(juego));
        assertEquals(3, contarEventos(juego));
    }

    @Test
    void criterio1LasZonasSonLasMismasParaTodosLosInscritos() {
        String master = registrar();
        Juego juego = crear(master, false);
        String jugador1 = registrar();
        String jugador2 = registrar();
        String ajeno = registrar();
        inscribir(jugador1, juego);
        inscribir(jugador2, juego);
        guardar(master, juego, List.of(
                zona("Aulario", cuadrado()), zona("Biblioteca", cuadradoMovido())))
                .then().statusCode(200);

        Map<String, Object> zonasMaster = ver(master, juego).then().statusCode(200)
                .extract().jsonPath().getMap("");
        assertEquals(zonasMaster, ver(jugador1, juego).then().statusCode(200)
                .extract().jsonPath().getMap(""));
        assertEquals(zonasMaster, ver(jugador2, juego).then().statusCode(200)
                .extract().jsonPath().getMap(""));
        ver(ajeno, juego).then().statusCode(404)
                .body("codigo", equalTo("NO_ENCONTRADA"));
        guardar(jugador1, juego, List.of()).then().statusCode(403)
                .body("codigo", equalTo("NO_ES_MASTER"));

        Juego otraPartida = crear(master, false);
        ver(master, otraPartida).then().statusCode(200).body("zonas", hasSize(0));
        assertNotEquals(zonasMaster, ver(master, otraPartida).then().statusCode(200)
                .extract().jsonPath().getMap(""));
    }

    @Test
    void criterio1ElArranqueRealCongelaLasZonasPeroPermiteConsultarlas() {
        String master = registrar();
        Juego juego = crear(master, true);
        String jugador1 = registrar();
        String jugador2 = registrar();
        inscribir(jugador1, juego);
        inscribir(jugador2, juego);
        guardar(master, juego, List.of(zona("Biblioteca", cuadrado())))
                .then().statusCode(200);
        Map<String, Object> antes = ver(jugador1, juego).then().statusCode(200)
                .extract().jsonPath().getMap("");

        given().port(puerto).auth().oauth2(master)
                .when().post("/api/v1/partidas/{id}/arranque", juego.id())
                .then().statusCode(200).body("estado", equalTo("EN_JUEGO"));
        guardar(master, juego, List.of()).then().statusCode(409)
                .body("codigo", equalTo("PARTIDA_NO_EDITABLE"));
        guardar(master, juego, List.of(zona("Otra", cuadradoMovido())))
                .then().statusCode(409).body("codigo", equalTo("PARTIDA_NO_EDITABLE"));
        assertEquals(antes, ver(jugador1, juego).then().statusCode(200)
                .extract().jsonPath().getMap(""));
        assertEquals(antes, ver(jugador2, juego).then().statusCode(200)
                .extract().jsonPath().getMap(""));
        assertEquals(1, contar(juego));
        assertEquals(1, contarEventos(juego));
    }

    @Test
    void criterio2ElListadoSoloContieneElDibujoSinPosicionesDeJugadores() {
        String master = registrar();
        Juego juego = crear(master, false);
        String jugador = registrar();
        inscribir(jugador, juego);
        guardar(master, juego, List.of(zona("Biblioteca", cuadrado())))
                .then().statusCode(200);

        Map<String, Object> respuesta = ver(jugador, juego).then().statusCode(200)
                .extract().jsonPath().getMap("");
        assertEquals(Set.of("zonas"), respuesta.keySet());
        List<?> zonas = (List<?>) respuesta.get("zonas");
        assertEquals(1, zonas.size());
        Map<?, ?> zona = (Map<?, ?>) zonas.getFirst();
        assertEquals(Set.of("id", "nombre", "puntos"), zona.keySet());
        assertEquals("Biblioteca", zona.get("nombre"));
        List<?> puntos = (List<?>) zona.get("puntos");
        assertEquals(4, puntos.size());
        for (Object punto : puntos) {
            Map<?, ?> coordenadas = (Map<?, ?>) punto;
            assertEquals(Set.of("lat", "lon"), coordenadas.keySet());
            assertTrue(coordenadas.get("lat") instanceof Number);
            assertTrue(coordenadas.get("lon") instanceof Number);
        }
        // Los puntos del poligono son publicos; la ubicacion de jugadores no lo es.
    }

    private Response guardar(String token, Juego juego, List<Map<String, Object>> zonas) {
        return given().port(puerto).auth().oauth2(token).contentType(ContentType.JSON)
                .body(Map.of("zonas", zonas)).when().put(RUTA, juego.id());
    }

    private Response ver(String token, Juego juego) {
        return given().port(puerto).auth().oauth2(token).when().get(RUTA, juego.id());
    }

    private void inscribir(String token, Juego juego) {
        given().port(puerto).auth().oauth2(token).contentType(ContentType.JSON)
                .body(Map.of("codigo", juego.codigo(), "aceptaNormas", true))
                .when().post("/api/v1/inscripciones").then().statusCode(201);
    }

    private Juego crear(String master, boolean masterJuega) {
        Instant inicio = Instant.now().plus(Duration.ofDays(2));
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("nombre", "Aceptacion zonas");
        cuerpo.put("inicioEn", inicio.toString());
        cuerpo.put("finEn", inicio.plus(Duration.ofDays(5)).toString());
        cuerpo.put("tipoMisiones", "BIBLIOTECA");
        cuerpo.put("plazoDisputaHoras", 6);
        cuerpo.put("entradasTardias", false);
        cuerpo.put("masterJuega", masterJuega);
        Response creada = given().port(puerto).auth().oauth2(master)
                .contentType(ContentType.JSON).body(cuerpo)
                .when().post("/api/v1/partidas")
                .then().statusCode(201).extract().response();
        return new Juego(creada.path("id"), creada.path("codigoInvitacion"));
    }

    private String registrar() {
        String alias = "az_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        return given().port(puerto).contentType(ContentType.JSON)
                .body(Map.of("alias", alias, "email", alias + "@example.com",
                        "contrasena", "fantasma2026"))
                .when().post("/api/v1/cuentas")
                .then().statusCode(201).extract().path("tokenAcceso");
    }

    private int contar(Juego juego) {
        return jdbc.queryForObject("select count(*) from zona_segura where partida_id = ?",
                Integer.class, UUID.fromString(juego.id()));
    }

    private int contarEventos(Juego juego) {
        return jdbc.queryForObject("select count(*) from evento where partida_id = ?"
                + " and tipo = 'ZONAS_CAMBIADAS'", Integer.class, UUID.fromString(juego.id()));
    }

    private static Map<String, Object> zona(String nombre, List<Map<String, Double>> puntos) {
        return Map.of("nombre", nombre, "puntos", puntos);
    }

    private static Map<String, Object> zonaConId(String id, String nombre,
            List<Map<String, Double>> puntos) {
        return Map.of("id", id, "nombre", nombre, "puntos", puntos);
    }

    private static List<Map<String, Double>> cuadrado() {
        return List.of(punto(41.68, -0.89), punto(41.68, -0.88),
                punto(41.69, -0.88), punto(41.69, -0.89));
    }

    private static List<Map<String, Double>> cuadradoMovido() {
        return List.of(punto(41.70, -0.90), punto(41.70, -0.89),
                punto(41.71, -0.89), punto(41.71, -0.90));
    }

    private static Map<String, Double> punto(double lat, double lon) {
        return Map.of("lat", lat, "lon", lon);
    }

    private record Juego(String id, String codigo) {
    }
}
