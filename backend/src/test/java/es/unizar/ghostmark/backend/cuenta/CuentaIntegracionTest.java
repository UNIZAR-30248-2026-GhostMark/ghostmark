package es.unizar.ghostmark.backend.cuenta;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import es.unizar.ghostmark.backend.TestcontainersConfiguration;
import es.unizar.ghostmark.backend.cuenta.repositories.FranjaHorarioRepository;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import java.time.Instant;
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

/**
 * Tests de aceptación de la PBI 1 (#2): uno por criterio de satisfacción, más el de la contraseña
 * guardada con hash. El de HTTPS está en {@code security/HttpsTest}.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CuentaIntegracionTest {

    private static final String RUTA_CUENTAS = "/api/v1/cuentas";
    private static final String RUTA_SESIONES = "/api/v1/sesiones";
    private static final String RUTA_HORARIO = "/api/v1/cuentas/yo/horario";
    private static final String RUTA_ACEPTACIONES = "/api/v1/cuentas/yo/aceptaciones";
    private static final String CONTRASENA = "fantasma2026";

    @LocalServerPort
    int puerto;

    @Autowired
    CuentaRepository cuentas;

    @Autowired
    FranjaHorarioRepository franjas;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void criterio1CrearCuentaEIniciarSesionYRechazarDatosIncorrectos() {
        String alias = aliasNuevo();
        String email = alias + "@example.com";

        Response registro = registrar(alias, email);
        assertNotNull(registro.path("tokenAcceso"));
        assertNotNull(registro.path("tokenRefresco"));

        given()
                .port(puerto)
                .contentType(ContentType.JSON)
                .body(Map.of("email", email, "contrasena", CONTRASENA))
                .when()
                .post(RUTA_SESIONES)
                .then()
                .statusCode(200)
                .body("cuenta.alias", equalTo(alias));

        // Contraseña equivocada.
        iniciarSesionFallida(email, "equivocada2026");
        // Email que no existe.
        iniciarSesionFallida("nadie-" + alias + "@example.com", CONTRASENA);
    }

    @Test
    void contrasenaNoSeGuardaLegible() {
        String alias = aliasNuevo();
        String email = alias + "@example.com";

        Map<String, Object> cuentaPublica = registrar(alias, email).path("cuenta");
        assertFalse(cuentaPublica.containsKey("contrasena"));
        assertFalse(cuentaPublica.containsKey("hashContrasena"));

        String guardada = cuentas.findByEmail(email).orElseThrow().getHashContrasena();
        assertNotEquals(CONTRASENA, guardada);
        assertFalse(guardada.contains(CONTRASENA));
        assertTrue(guardada.startsWith("$2"), "La contraseña se guarda con bcrypt");
    }

    @Test
    void criterio2HorarioYAceptacionesQueSePidenUnaSolaVez() {
        String alias = aliasNuevo();
        String email = alias + "@example.com";
        String token = registrar(alias, email).path("tokenAcceso");

        given()
                .port(puerto)
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("franjas", List.of(
                        Map.of("dia", 1, "horaInicio", "08:30"),
                        Map.of("dia", 3, "horaInicio", "15:00"))))
                .when()
                .put(RUTA_HORARIO)
                .then()
                .statusCode(200);

        given()
                .port(puerto)
                .header("Authorization", "Bearer " + token)
                .when()
                .get(RUTA_HORARIO)
                .then()
                .statusCode(200)
                .body("franjas.size()", equalTo(2))
                .body("franjas[0].dia", equalTo(1))
                .body("franjas[0].horaInicio", equalTo("08:30"));

        aceptar(token)
                .body("ubicacionAceptada", equalTo(true))
                .body("avisoDisponibilidadAceptado", equalTo(true));

        UUID cuentaId = cuentas.findByEmail(email).orElseThrow().getId();
        Cuenta primera = cuentas.findById(cuentaId).orElseThrow();
        Instant ubicacion = primera.getUbicacionAceptadaEn();
        Instant aviso = primera.getAvisoDisponibilidadAceptadoEn();
        assertNotNull(ubicacion);
        assertNotNull(aviso);

        // Aceptar otra vez no cambia nada: se piden una sola vez.
        aceptar(token);
        Cuenta segunda = cuentas.findById(cuentaId).orElseThrow();
        assertEquals(ubicacion, segunda.getUbicacionAceptadaEn());
        assertEquals(aviso, segunda.getAvisoDisponibilidadAceptadoEn());
    }

    @Test
    void criterio2LaCuentaSoloGuardaAliasDatosDeAccesoYAceptaciones() {
        // Columnas de modelo-datos.md. El horario va en franja_horario y las sesiones en
        // token_refresco; ninguna columna guarda una posición.
        Set<String> esperadas = Set.of("id", "alias", "email", "hash_contrasena",
                "ubicacion_aceptada_en", "aviso_disponibilidad_aceptado_en",
                "creada_en", "actualizada_en");

        List<String> columnas = jdbc.queryForList(
                "select column_name from information_schema.columns"
                        + " where table_schema = current_schema() and table_name = 'cuenta'",
                String.class);

        assertEquals(esperadas, Set.copyOf(columnas));
    }

    @Test
    void criterio3BorrarLaCuentaImpideIniciarSesionYLiberaElAlias() {
        String alias = aliasNuevo();
        String email = alias + "@example.com";
        Response registro = registrar(alias, email);
        String token = registro.path("tokenAcceso");
        String tokenRefresco = registro.path("tokenRefresco");
        UUID cuentaId = cuentas.findByEmail(email).orElseThrow().getId();

        given()
                .port(puerto)
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("franjas", List.of(Map.of("dia", 2, "horaInicio", "10:00"))))
                .when()
                .put(RUTA_HORARIO)
                .then()
                .statusCode(200);

        given()
                .port(puerto)
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/api/v1/cuentas/yo")
                .then()
                .statusCode(204);

        assertTrue(cuentas.findById(cuentaId).isEmpty());
        assertTrue(franjas.buscarPorCuentaId(cuentaId).isEmpty());
        assertFalse(cuentas.existsByAliasIgnoreCase(alias));

        iniciarSesionFallida(email, CONTRASENA);

        given()
                .port(puerto)
                .contentType(ContentType.JSON)
                .body(Map.of("tokenRefresco", tokenRefresco))
                .when()
                .post("/api/v1/sesiones/refresco")
                .then()
                .statusCode(401);

        // El alias ha desaparecido: otra persona puede registrarse con él.
        registrar(alias, "otra-" + email);
    }

    private static String aliasNuevo() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private Response registrar(String alias, String email) {
        return given()
                .port(puerto)
                .contentType(ContentType.JSON)
                .body(Map.of("alias", alias, "email", email, "contrasena", CONTRASENA))
                .when()
                .post(RUTA_CUENTAS)
                .then()
                .statusCode(201)
                .body("cuenta.alias", equalTo(alias))
                .extract()
                .response();
    }

    private void iniciarSesionFallida(String email, String contrasena) {
        given()
                .port(puerto)
                .contentType(ContentType.JSON)
                .body(Map.of("email", email, "contrasena", contrasena))
                .when()
                .post(RUTA_SESIONES)
                .then()
                .statusCode(401)
                .body("codigo", equalTo("CREDENCIALES_INCORRECTAS"));
    }

    private ValidatableResponse aceptar(String token) {
        return given()
                .port(puerto)
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("ubicacion", true, "avisoDisponibilidad", true))
                .when()
                .put(RUTA_ACEPTACIONES)
                .then()
                .statusCode(200);
    }
}
