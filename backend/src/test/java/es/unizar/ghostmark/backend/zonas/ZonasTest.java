package es.unizar.ghostmark.backend.zonas;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import es.unizar.ghostmark.backend.TestcontainersConfiguration;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
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

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ZonasTest {

    private static final String RUTA = "/api/v1/partidas/{id}/zonas";

    @LocalServerPort
    int puerto;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void masterCreaConsultaRenombraIntercambiaYBorraZonas() {
        String master = registrar();
        Juego juego = crear(master, false);
        List<Map<String, Object>> originales = List.of(
                zona("Biblioteca", cuadrado()), zona("Aulario", cuadradoDesplazado()));
        Response respuesta = guardar(master, juego, originales)
                .then().statusCode(200)
                .body("zonas", hasSize(2))
                .body("zonas.nombre", hasItems("Biblioteca", "Aulario"))
                .extract().response();
        List<String> ids = respuesta.path("zonas.id");
        assertNotEquals(ids.get(0), ids.get(1));
        assertEquals(2, contar(juego));
        assertEquals(1, eventos(juego));
        Instant creado = jdbc.queryForObject(
                "select creada_en from zona_segura where id = ?", java.sql.Timestamp.class,
                UUID.fromString(ids.get(0))).toInstant();

        // Intercambiar los nombres no debe violar el índice único temporalmente.
        List<Map<String, Object>> cambiadas = List.of(
                zonaConId(ids.get(0), "Biblioteca", cuadradoDesplazado()),
                zonaConId(ids.get(1), "Aulario", cuadrado()));
        guardar(master, juego, cambiadas).then().statusCode(200)
                .body("zonas", hasSize(2));
        assertEquals(creado, jdbc.queryForObject(
                "select creada_en from zona_segura where id = ?", java.sql.Timestamp.class,
                UUID.fromString(ids.get(0))).toInstant());
        ver(master, juego).then().statusCode(200).body("zonas", hasSize(2))
                .body("zonas[0].puntos", hasSize(4));

        guardar(master, juego, List.of()).then().statusCode(200)
                .body("zonas", hasSize(0));
        assertEquals(0, contar(juego));
        assertEquals(3, eventos(juego));
        assertEquals(0, jdbc.queryForObject("select jsonb_array_length(datos->'zonas')"
                        + " from evento where partida_id = ? and tipo = 'ZONAS_CAMBIADAS'"
                        + " order by secuencia desc limit 1", Integer.class,
                UUID.fromString(juego.id())).intValue());
    }

    @Test
    void listadoVisibleParaMasterYJugadorPeroNoParaOtraCuenta() {
        String master = registrar();
        Juego juego = crear(master, false);
        String jugador = registrar();
        String ajeno = registrar();
        inscribir(jugador, juego).then().statusCode(201);
        guardar(master, juego, List.of(zona("Facultad", cuadrado())))
                .then().statusCode(200);

        ver(master, juego).then().statusCode(200)
                .body("zonas[0].nombre", equalTo("Facultad"));
        ver(jugador, juego).then().statusCode(200)
                .body("zonas[0].puntos", hasSize(4));
        ver(ajeno, juego).then().statusCode(404)
                .body("codigo", equalTo("NO_ENCONTRADA"));
        guardar(jugador, juego, List.of()).then().statusCode(403)
                .body("codigo", equalTo("NO_ES_MASTER"));
        guardar(ajeno, juego, List.of()).then().statusCode(404)
                .body("codigo", equalTo("NO_ENCONTRADA"));
        assertEquals(1, contar(juego));
    }

    @Test
    void noSePermiteAccederSinAutenticacionNiAUnaPartidaInexistente() {
        String token = registrar();
        String inexistente = UUID.randomUUID().toString();
        given().port(puerto).when().get(RUTA, inexistente)
                .then().statusCode(401).body("codigo", equalTo("NO_AUTENTICADO"));
        given().port(puerto).contentType(ContentType.JSON).body(Map.of("zonas", List.of()))
                .when().put(RUTA, inexistente).then().statusCode(401);
        given().port(puerto).auth().oauth2(token).when().get(RUTA, inexistente)
                .then().statusCode(404).body("codigo", equalTo("NO_ENCONTRADA"));
    }

    @Test
    void trasArrancarLasZonasSePuedenLeerPeroNoModificar() {
        String master = registrar();
        Juego juego = crear(master, false);
        guardar(master, juego, List.of(zona("Biblioteca", cuadrado())))
                .then().statusCode(200);
        jdbc.update("update partida set estado = 'EN_JUEGO' where id = ?",
                UUID.fromString(juego.id()));

        guardar(master, juego, List.of()).then().statusCode(409)
                .body("codigo", equalTo("PARTIDA_NO_EDITABLE"));
        ver(master, juego).then().statusCode(200).body("zonas", hasSize(1));
        assertEquals(1, contar(juego));
        assertEquals(1, eventos(juego));
    }

    @Test
    void nombresRepetidosYLimiteDeZonasDan400SinGuardarNada() {
        String master = registrar();
        Juego juego = crear(master, false);
        guardar(master, juego, List.of(zona("  Biblioteca  ", cuadrado())))
                .then().statusCode(200)
                .body("zonas[0].nombre", equalTo("Biblioteca"));

        guardar(master, juego, List.of(zona("Biblioteca", cuadrado()),
                        zona(" biblioteca ", cuadradoDesplazado())))
                .then().statusCode(400).body("codigo", equalTo("DATOS_NO_VALIDOS"))
                .body("errores.campo", hasItems("zonas[1].nombre"));
        List<Map<String, Object>> demasiadas = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            demasiadas.add(zona("Zona" + i, cuadrado()));
        }
        guardar(master, juego, demasiadas).then().statusCode(400)
                .body("errores.campo", hasItems("zonas"));
        guardar(master, juego, List.of(zona(" ", cuadrado())))
                .then().statusCode(400).body("errores.campo", hasItems("zonas[0].nombre"));
        ver(master, juego).then().statusCode(200).body("zonas", hasSize(1));
        assertEquals(1, eventos(juego));
    }

    @Test
    void geometriasInvalidasDan400YSinEfectosParciales() {
        String master = registrar();
        Juego juego = crear(master, false);
        List<List<Map<String, Double>>> invalidas = List.of(
                List.of(punto(41.0, -1.0), punto(42.0, -2.0)),
                List.of(punto(41.0, -1.0), punto(41.0, -1.0), punto(42.0, -2.0)),
                List.of(punto(0.0, 0.0), punto(1.0, 1.0),
                        punto(0.0, 1.0), punto(1.0, 0.0)),
                List.of(punto(91.0, 0.0), punto(41.0, -1.0), punto(42.0, -2.0)));
        for (List<Map<String, Double>> puntos : invalidas) {
            guardar(master, juego, List.of(zona("Invalida", puntos)))
                    .then().statusCode(400).body("codigo", equalTo("DATOS_NO_VALIDOS"))
                    .body("errores.campo", hasItems("zonas[0].puntos"));
        }
        List<Map<String, Double>> veintiuno = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            double angulo = 2 * Math.PI * i / 21;
            veintiuno.add(punto(Math.sin(angulo), Math.cos(angulo)));
        }
        guardar(master, juego, List.of(zona("Demasiados", veintiuno)))
                .then().statusCode(400).body("errores.campo", hasItems("zonas[0].puntos"));
        assertEquals(0, contar(juego));
        assertEquals(0, eventos(juego));
    }

    @Test
    void idsRepetidosYDeOtraPartidaNoSeAceptan() {
        String master = registrar();
        Juego primera = crear(master, false);
        Juego segunda = crear(master, false);
        String idPrimera = guardar(master, primera, List.of(zona("Una", cuadrado())))
                .then().statusCode(200).extract().path("zonas[0].id");

        guardar(master, segunda, List.of(zonaConId(idPrimera, "Copia", cuadrado())))
                .then().statusCode(400).body("errores.campo", hasItems("zonas[0].id"));
        guardar(master, primera, List.of(zonaConId(idPrimera, "A", cuadrado()),
                        zonaConId(idPrimera, "B", cuadradoDesplazado())))
                .then().statusCode(400).body("errores.campo", hasItems("zonas[1].id"));
        assertEquals(0, contar(segunda));
        assertEquals(1, contar(primera));
    }

    private Response guardar(String token, Juego juego, List<Map<String, Object>> zonas) {
        return given().port(puerto).auth().oauth2(token)
                .contentType(ContentType.JSON).body(Map.of("zonas", zonas))
                .when().put(RUTA, juego.id());
    }

    private Response ver(String token, Juego juego) {
        return given().port(puerto).auth().oauth2(token).when().get(RUTA, juego.id());
    }

    private Response inscribir(String token, Juego juego) {
        return given().port(puerto).auth().oauth2(token).contentType(ContentType.JSON)
                .body(Map.of("codigo", juego.codigo(), "aceptaNormas", true))
                .when().post("/api/v1/inscripciones");
    }

    private Juego crear(String token, boolean masterJuega) {
        Instant inicio = Instant.now().plus(Duration.ofDays(2));
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("nombre", "Partida zonas");
        cuerpo.put("inicioEn", inicio.toString());
        cuerpo.put("finEn", inicio.plus(Duration.ofDays(5)).toString());
        cuerpo.put("tipoMisiones", "BIBLIOTECA");
        cuerpo.put("plazoDisputaHoras", 6);
        cuerpo.put("entradasTardias", false);
        cuerpo.put("masterJuega", masterJuega);
        Response respuesta = given().port(puerto).auth().oauth2(token)
                .contentType(ContentType.JSON).body(cuerpo)
                .when().post("/api/v1/partidas")
                .then().statusCode(201).extract().response();
        return new Juego(respuesta.path("id"), respuesta.path("codigoInvitacion"));
    }

    private String registrar() {
        String alias = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
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

    private int eventos(Juego juego) {
        return jdbc.queryForObject("select count(*) from evento where partida_id = ?"
                        + " and tipo = 'ZONAS_CAMBIADAS'", Integer.class,
                UUID.fromString(juego.id()));
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

    private static List<Map<String, Double>> cuadradoDesplazado() {
        return List.of(punto(41.70, -0.90), punto(41.70, -0.89),
                punto(41.71, -0.89), punto(41.71, -0.90));
    }

    private static Map<String, Double> punto(double lat, double lon) {
        return Map.of("lat", lat, "lon", lon);
    }

    private record Juego(String id, String codigo) {
    }
}
