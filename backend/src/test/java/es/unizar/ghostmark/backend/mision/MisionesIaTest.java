package es.unizar.ghostmark.backend.mision;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import es.unizar.ghostmark.backend.TestcontainersConfiguration;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MisionesIaTest {

    private static final String RUTA = "/api/v1/partidas/{id}/misiones-ia";

    @LocalServerPort
    int puerto;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    GeneradorMisiones generador;

    @AfterEach
    void restaurar() {
        simulado().usar(GeneradorSimulado.Modo.NORMAL);
    }

    @Test
    void elMasterPideUnaTandaYRevisaSusMisiones() {
        Persona master = registrar();
        Juego juego = crear(master, "IA");
        Response tanda = tanda(master, juego, "Campus").then().statusCode(201)
                .body("tanda", equalTo(1))
                .body("misiones", hasSize(8))
                .body("misiones.estado", everyItem(equalTo("PROPUESTA")))
                .body("misiones.tanda", everyItem(equalTo(1)))
                .body("descartadas", equalTo(0))
                .body("tandasRestantes", equalTo(4))
                .extract().response();
        List<String> ids = tanda.path("misiones.id");
        revisar(master, juego, ids.get(0), "APROBADA").then().statusCode(200)
                .body("id", equalTo(ids.get(0)))
                .body("estado", equalTo("APROBADA"))
                .body("tanda", equalTo(1))
                .body("texto", equalTo(tanda.path("misiones[0].texto")));
        revisar(master, juego, ids.get(1), "RECHAZADA").then().statusCode(200)
                .body("estado", equalTo("RECHAZADA"));
        revisar(master, juego, ids.get(0), "RECHAZADA").then().statusCode(409)
                .body("codigo", equalTo("MISION_YA_REVISADA"));
        listar(master, juego).then().statusCode(200)
                .body("misiones", hasSize(8))
                .body("tandasRestantes", equalTo(4))
                .body("misiones.find { it.id == '" + ids.get(0) + "' }.estado",
                        equalTo("APROBADA"))
                .body("misiones.find { it.id == '" + ids.get(1) + "' }.estado",
                        equalTo("RECHAZADA"))
                .body("misiones.find { it.id == '" + ids.get(2) + "' }.estado",
                        equalTo("PROPUESTA"));
        assertEquals(List.of("1|Campus|8|0"), jdbc.queryForList("select (datos->>'tanda') || '|'"
                + " || (datos->>'contexto') || '|' || jsonb_array_length(datos->'misionIds')"
                + " || '|' || (datos->>'descartadas') from evento where partida_id = ?"
                + " and tipo = 'MISIONES_GENERADAS'", String.class, juego.id()));
        assertEquals(List.of(ids.get(0) + "|APROBADA", ids.get(1) + "|RECHAZADA"),
                jdbc.queryForList("select (datos->>'misionId') || '|' || (datos->>'decision')"
                        + " from evento where partida_id = ? and tipo = 'MISION_REVISADA'"
                        + " order by secuencia", String.class, juego.id()));
        assertEquals(2, jdbc.queryForObject("select count(*) from mision where partida_id = ?"
                + " and revisada_en is not null", Integer.class, juego.id()));
    }

    @Test
    void lasDescartadasNoLleganAlMasterPeroLaTandaCuenta() {
        Persona master = registrar();
        Juego juego = crear(master, "IA");
        // El simulado mezcla cuatro propuestas válidas con otras de categoría o texto prohibido
        String[] validas = {
            "Que te cuente qué asignatura le cuesta más",
            "Que te preste un lápiz durante un momento",
            "Que te recomiende una cafetería cerca de la facultad",
            "Que te diga una palabra que empiece por la letra q"
        };
        simulado().usar(GeneradorSimulado.Modo.PROHIBIDAS);
        tanda(master, juego, "Fiesta de bienvenida").then().statusCode(201)
                .body("misiones.texto", containsInAnyOrder(validas))
                .body("descartadas", equalTo(8))
                .body("tandasRestantes", equalTo(4));
        simulado().usar(GeneradorSimulado.Modo.FORMATO_INVALIDO);
        tanda(master, juego, "Semana de exámenes").then().statusCode(201)
                .body("tanda", equalTo(2))
                .body("misiones", hasSize(0))
                .body("descartadas", equalTo(12))
                .body("tandasRestantes", equalTo(3));
        // Al volver a cargar la lista siguen apareciendo solo las propuestas válidas
        listar(master, juego).then().statusCode(200)
                .body("misiones.texto", containsInAnyOrder(validas))
                .body("tandasRestantes", equalTo(3));
        assertEquals(4, jdbc.queryForObject("select count(*) from mision where partida_id = ?",
                Integer.class, juego.id()));
        assertEquals(2, tandasRegistradas(juego));
    }

    @ParameterizedTest
    @EnumSource(value = GeneradorSimulado.Modo.class, names = {"CAIDO", "LENTO"})
    void siLaIaNoRespondeLaTandaNoCuenta(GeneradorSimulado.Modo modo) {
        Persona master = registrar();
        Juego juego = crear(master, "IA");
        simulado().usar(modo);
        tanda(master, juego, "Campus").then().statusCode(503)
                .body("codigo", equalTo("IA_NO_DISPONIBLE"));
        assertEquals(0, tandasRegistradas(juego));
        listar(master, juego).then().statusCode(200)
                .body("misiones", hasSize(0))
                .body("tandasRestantes", equalTo(5));
        simulado().usar(GeneradorSimulado.Modo.NORMAL);
        tanda(master, juego, "Campus").then().statusCode(201).body("tanda", equalTo(1));
    }

    @Test
    void comoMuchoCincoTandasSinRepetirMisiones() {
        Persona master = registrar();
        Juego juego = crear(master, "IA");
        for (int i = 1; i <= 5; i++) {
            tanda(master, juego, "Campus").then().statusCode(201)
                    .body("tanda", equalTo(i))
                    .body("tandasRestantes", equalTo(5 - i));
        }
        tanda(master, juego, "Campus").then().statusCode(409)
                .body("codigo", equalTo("LIMITE_TANDAS"));
        List<String> textos = listar(master, juego).then().statusCode(200)
                .body("tandasRestantes", equalTo(0))
                .extract().path("misiones.texto");
        assertEquals(40, textos.size());
        Set<String> distintos = new HashSet<>();
        textos.forEach(texto -> distintos.add(texto.toLowerCase(Locale.ROOT)));
        assertEquals(40, distintos.size());
        assertEquals(5, tandasRegistradas(juego));
    }

    @Test
    void soloConMisionesDeIaYMientrasEstaEnInscripcion() {
        Persona master = registrar();
        Juego biblioteca = crear(master, "BIBLIOTECA");
        tanda(master, biblioteca, "Campus").then().statusCode(409)
                .body("codigo", equalTo("MISIONES_DE_BIBLIOTECA"));
        Juego juego = crear(master, "IA");
        String mision = tanda(master, juego, "Campus").then().statusCode(201)
                .extract().path("misiones[0].id");
        apuntar(registrar(), juego);
        apuntar(registrar(), juego);
        arrancarYa(master, juego);
        tanda(master, juego, "Campus").then().statusCode(409)
                .body("codigo", equalTo("PARTIDA_NO_ABIERTA"));
        revisar(master, juego, mision, "APROBADA").then().statusCode(409)
                .body("codigo", equalTo("PARTIDA_NO_ABIERTA"));
        listar(master, juego).then().statusCode(200).body("misiones", hasSize(8));
    }

    @Test
    void unaMisionDeOtraPartidaNoSePuedeRevisar() {
        Persona master = registrar();
        Juego juego = crear(master, "IA");
        Juego otro = crear(master, "IA");
        String ajena = tanda(master, otro, "Campus").then().statusCode(201)
                .extract().path("misiones[0].id");
        revisar(master, juego, ajena, "APROBADA").then().statusCode(404);
        revisar(master, juego, UUID.randomUUID().toString(), "APROBADA").then().statusCode(404);
        assertEquals("PROPUESTA", jdbc.queryForObject("select estado from mision where id = ?",
                String.class, UUID.fromString(ajena)));
    }

    @ParameterizedTest
    @EnumSource(value = GeneradorSimulado.Modo.class, names = {"CAIDO", "LENTO"})
    void elMasterRecibeElAvisoYCambiaLaMismaPartidaABiblioteca(GeneradorSimulado.Modo modo) {
        simulado().usar(modo);
        Persona master = registrar();
        Juego juego = crear(master, "IA");
        tanda(master, juego, "Campus").then().statusCode(503)
                .body("codigo", equalTo("IA_NO_DISPONIBLE"))
                .body("detail", equalTo("La IA no responde: se usarán misiones de la biblioteca"));
        assertEquals(0, tandasRegistradas(juego));

        // La app continúa con Biblioteca editando la partida que ya había creado
        given().port(puerto).auth().oauth2(master.token()).contentType(ContentType.JSON)
                .body(Map.of("tipoMisiones", "BIBLIOTECA"))
                .patch("/api/v1/partidas/{id}", juego.id()).then().statusCode(200)
                .body("id", equalTo(juego.id().toString()))
                .body("codigoInvitacion", equalTo(juego.codigo()))
                .body("estado", equalTo("EN_INSCRIPCION"))
                .body("tipoMisiones", equalTo("BIBLIOTECA"));

        Persona segundo = registrar();
        Persona tercero = registrar();
        apuntar(segundo, juego);
        apuntar(tercero, juego);
        arrancarYa(master, juego);

        // Leemos la misión desde la carta de cada jugador con el generador aún caído o lento
        List<String> biblioteca = jdbc.queryForList(
                "select texto from mision where activa and partida_id is null", String.class);
        for (Persona jugador : List.of(master, segundo, tercero)) {
            String mision = given().port(puerto).auth().oauth2(jugador.token())
                    .get("/api/v1/partidas/{id}/yo/carta", juego.id()).then().statusCode(200)
                    .body("estado", equalTo("VIVO")).extract().path("mision");
            assertTrue(biblioteca.contains(mision), "La carta debe tener una misión de biblioteca");
        }
    }

    @Test
    void elContextoTieneDeTresACuarentaCaracteres() {
        Persona master = registrar();
        Juego juego = crear(master, "IA");
        for (String contexto : List.of("ab", "   ab   ", "a".repeat(41), "Cam\npus")) {
            tanda(master, juego, contexto).then().statusCode(400)
                    .body("codigo", equalTo("DATOS_NO_VALIDOS"))
                    .body("errores[0].campo", equalTo("contexto"));
        }
        given().port(puerto).auth().oauth2(master.token()).contentType(ContentType.JSON)
                .body(Map.of()).post(RUTA + "/tandas", juego.id()).then().statusCode(400);
        assertEquals(0, tandasRegistradas(juego));
        tanda(master, juego, "abc").then().statusCode(201);
        tanda(master, juego, "a".repeat(40)).then().statusCode(201);
    }

    @Test
    void laRevisionSoloAceptaAprobadaORechazada() {
        Persona master = registrar();
        Juego juego = crear(master, "IA");
        String mision = tanda(master, juego, "Campus").then().statusCode(201)
                .extract().path("misiones[0].id");
        for (String estado : List.of("PROPUESTA", "aprobada", "OTRA")) {
            revisar(master, juego, mision, estado).then().statusCode(400)
                    .body("codigo", equalTo("DATOS_NO_VALIDOS"))
                    .body("errores[0].campo", equalTo("estado"));
        }
        given().port(puerto).auth().oauth2(master.token()).contentType(ContentType.JSON)
                .body(Map.of()).patch(RUTA + "/{mision}", juego.id(), mision).then()
                .statusCode(400);
        assertEquals("PROPUESTA", jdbc.queryForObject("select estado from mision where id = ?",
                String.class, UUID.fromString(mision)));
    }

    @Test
    void soloElMasterGestionaLasMisionesDeLaIa() {
        Persona master = registrar();
        Juego juego = crear(master, "IA");
        String mision = tanda(master, juego, "Campus").then().statusCode(201)
                .extract().path("misiones[0].id");
        Persona jugador = registrar();
        apuntar(jugador, juego);
        Persona otra = registrar();
        listar(jugador, juego).then().statusCode(403).body("codigo", equalTo("NO_ES_MASTER"));
        tanda(jugador, juego, "Campus").then().statusCode(403)
                .body("codigo", equalTo("NO_ES_MASTER"));
        revisar(jugador, juego, mision, "APROBADA").then().statusCode(403)
                .body("codigo", equalTo("NO_ES_MASTER"));
        listar(otra, juego).then().statusCode(404);
        tanda(otra, juego, "Campus").then().statusCode(404);
        revisar(otra, juego, mision, "APROBADA").then().statusCode(404);
        given().port(puerto).get(RUTA, juego.id()).then().statusCode(401);
        assertEquals(1, tandasRegistradas(juego));
        assertEquals("PROPUESTA", jdbc.queryForObject("select estado from mision where id = ?",
                String.class, UUID.fromString(mision)));
    }

    @Test
    void conLaCuentaBorradaElMasterYaNoPuede() {
        Persona master = registrar();
        Juego juego = crear(master, "IA");
        tanda(master, juego, "Campus").then().statusCode(201);
        given().port(puerto).auth().oauth2(master.token()).delete("/api/v1/cuentas/yo").then()
                .statusCode(204);
        listar(master, juego).then().statusCode(401);
        tanda(master, juego, "Campus").then().statusCode(401);
        assertEquals(1, tandasRegistradas(juego));
    }

    @Test
    void dosTandasALaVezNoPasanDelLimite() throws Exception {
        Persona master = registrar();
        Juego juego = crear(master, "IA");
        for (int i = 0; i < 4; i++) {
            tanda(master, juego, "Campus").then().statusCode(201);
        }
        CyclicBarrier salida = new CyclicBarrier(2);
        Callable<Integer> pedir = () -> {
            salida.await();
            return tanda(master, juego, "Campus").statusCode();
        };
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> primera = hilos.submit(pedir);
            Future<Integer> segunda = hilos.submit(pedir);
            List<Integer> estados = new ArrayList<>(List.of(primera.get(), segunda.get()));
            estados.sort(null);
            assertEquals(List.of(201, 409), estados);
        } finally {
            hilos.shutdown();
        }
        assertEquals(5, tandasRegistradas(juego));
        assertEquals(List.of(1, 2, 3, 4, 5), jdbc.queryForList("select (datos->>'tanda')::int"
                + " from evento where partida_id = ? and tipo = 'MISIONES_GENERADAS'"
                + " order by secuencia", Integer.class, juego.id()));
    }

    @Test
    void lasAprobadasSeRepartenAlArrancar() {
        Persona master = registrar();
        Juego juego = crear(master, "IA");
        List<String> ids = tanda(master, juego, "Campus").then().statusCode(201)
                .extract().path("misiones.id");
        revisar(master, juego, ids.get(0), "APROBADA").then().statusCode(200);
        revisar(master, juego, ids.get(1), "APROBADA").then().statusCode(200);
        revisar(master, juego, ids.get(2), "RECHAZADA").then().statusCode(200);
        apuntar(registrar(), juego);
        apuntar(registrar(), juego);
        arrancarYa(master, juego);
        List<String> repartidas = jdbc.queryForList("select a.valor->>'misionId' from evento e,"
                + " jsonb_array_elements(e.datos->'asignaciones') as a(valor)"
                + " where e.partida_id = ? and e.tipo = 'MISIONES_REPARTIDAS'",
                String.class, juego.id());
        assertEquals(3, repartidas.size());
        assertTrue(repartidas.containsAll(ids.subList(0, 2)));
        for (String id : ids.subList(2, ids.size())) {
            assertFalse(repartidas.contains(id));
        }
        // La plaza que falta sale de la biblioteca activa y no de las propuestas sin revisar
        String deBiblioteca = repartidas.stream().filter(id -> !ids.contains(id))
                .findFirst().orElseThrow();
        assertEquals(1, jdbc.queryForObject("select count(*) from mision"
                + " where id = ? and partida_id is null and activa", Integer.class,
                UUID.fromString(deBiblioteca)));
    }

    private GeneradorSimulado simulado() {
        return (GeneradorSimulado) generador;
    }

    private int tandasRegistradas(Juego juego) {
        return jdbc.queryForObject("select count(*) from evento where partida_id = ?"
                + " and tipo = 'MISIONES_GENERADAS'", Integer.class, juego.id());
    }

    private Response tanda(Persona persona, Juego juego, String contexto) {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("contexto", contexto);
        return given().port(puerto).auth().oauth2(persona.token()).contentType(ContentType.JSON)
                .body(cuerpo).post(RUTA + "/tandas", juego.id());
    }

    private Response listar(Persona persona, Juego juego) {
        return given().port(puerto).auth().oauth2(persona.token()).get(RUTA, juego.id());
    }

    private Response revisar(Persona persona, Juego juego, String mision, String estado) {
        return given().port(puerto).auth().oauth2(persona.token()).contentType(ContentType.JSON)
                .body(Map.of("estado", estado)).patch(RUTA + "/{mision}", juego.id(), mision);
    }

    private Persona registrar() {
        String alias = "a_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Response respuesta = given().port(puerto).contentType(ContentType.JSON)
                .body(Map.of("alias", alias, "email", alias + "@example.com",
                        "contrasena", "fantasma2026"))
                .post("/api/v1/cuentas").then().statusCode(201).extract().response();
        return new Persona(UUID.fromString(respuesta.path("cuenta.id")),
                respuesta.path("tokenAcceso"));
    }

    private Juego crear(Persona master, String tipoMisiones) {
        Instant inicio = Instant.now().plus(Duration.ofDays(1));
        Response respuesta = given().port(puerto).auth().oauth2(master.token())
                .contentType(ContentType.JSON).body(Map.of("nombre", "Partida Misiones IA",
                        "inicioEn", inicio.toString(), "finEn", inicio.plus(Duration.ofDays(7))
                                .toString(), "tipoMisiones", tipoMisiones,
                        "plazoDisputaHoras", 6, "entradasTardias", false, "masterJuega", true))
                .post("/api/v1/partidas").then().statusCode(201).extract().response();
        return new Juego(UUID.fromString(respuesta.path("id")), respuesta.path("codigoInvitacion"));
    }

    private void apuntar(Persona jugador, Juego juego) {
        given().port(puerto).auth().oauth2(jugador.token()).contentType(ContentType.JSON)
                .body(Map.of("codigo", juego.codigo(), "aceptaNormas", true))
                .post("/api/v1/inscripciones").then().statusCode(201);
    }

    private void arrancarYa(Persona master, Juego juego) {
        given().port(puerto).auth().oauth2(master.token())
                .post("/api/v1/partidas/{id}/arranque", juego.id()).then().statusCode(200);
    }

    private record Persona(UUID id, String token) {
    }

    private record Juego(UUID id, String codigo) {
    }
}
