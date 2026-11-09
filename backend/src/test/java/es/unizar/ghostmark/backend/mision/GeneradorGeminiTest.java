package es.unizar.ghostmark.backend.mision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class GeneradorGeminiTest {

    private HttpServer servidor;
    private final AtomicReference<String> recibido = new AtomicReference<>();
    private final AtomicReference<String> clave = new AtomicReference<>();
    private volatile int estado = 200;
    private volatile String respuesta = "";
    private volatile long espera = 0;

    @BeforeEach
    void arrancar() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", intercambio -> {
            recibido.set(intercambio.getRequestURI().getPath() + " "
                    + new String(intercambio.getRequestBody().readAllBytes(),
                            StandardCharsets.UTF_8));
            clave.set(intercambio.getRequestHeaders().getFirst("x-goog-api-key"));
            try {
                Thread.sleep(espera);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
            }
            byte[] cuerpo = respuesta.getBytes(StandardCharsets.UTF_8);
            intercambio.sendResponseHeaders(estado, cuerpo.length);
            try (OutputStream salida = intercambio.getResponseBody()) {
                salida.write(cuerpo);
            }
        });
        servidor.start();
    }

    @AfterEach
    void parar() {
        servidor.stop(0);
    }

    private GeneradorGemini generador(Duration limite) {
        return new GeneradorGemini("clave-de-prueba", "modelo-flash",
                URI.create("http://127.0.0.1:" + servidor.getAddress().getPort() + "/v1beta/"),
                limite);
    }

    @Test
    void unaRespuestaCorrectaSeConvierteEnMisiones() {
        String misiones = "{\"misiones\":[{\"texto\":\"Que te diga la hora\",\"categoria\":"
                + "\"CONVERSACION\"},{\"texto\":\"Que te abrace\",\"categoria\":"
                + "\"CONTACTO_FISICO\"}]}";
        respuesta = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":"
                + JsonMapper.shared().writeValueAsString(misiones) + "}]}}]}";
        List<MisionGenerada> generadas = generador(Duration.ofSeconds(5))
                .generar(12, "Campus", List.of("Que te preste un bolígrafo"));
        assertEquals(List.of(new MisionGenerada("Que te diga la hora", "CONVERSACION"),
                new MisionGenerada("Que te abrace", "CONTACTO_FISICO")), generadas);
        assertEquals("clave-de-prueba", clave.get());
        assertTrue(recibido.get().startsWith("/v1beta/models/modelo-flash:generateContent "));
        assertTrue(recibido.get().contains("Campus"));
        assertTrue(recibido.get().contains("Que te preste un bolígrafo"));
        assertTrue(recibido.get().contains("responseSchema"));
        assertFalse(recibido.get().contains("clave-de-prueba"));
    }

    @Test
    void sinCuotaNoEstaDisponible() {
        estado = 429;
        respuesta = "{\"error\":{\"code\":429}}";
        assertThrows(GeneradorNoDisponible.class,
                () -> generador(Duration.ofSeconds(5)).generar(12, "Campus", List.of()));
    }

    @Test
    void siTardaMasDelLimiteNoEstaDisponible() {
        espera = 2000;
        respuesta = "{}";
        assertThrows(GeneradorNoDisponible.class,
                () -> generador(Duration.ofSeconds(1)).generar(12, "Campus", List.of()));
    }

    @Test
    void unJsonQueNoSeEntiendeNoDaMisiones() {
        respuesta = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"no es json\"}]}}]}";
        assertEquals(List.of(), generador(Duration.ofSeconds(5)).generar(12, "Campus", List.of()));
        respuesta = "esto tampoco";
        assertEquals(List.of(), generador(Duration.ofSeconds(5)).generar(12, "Campus", List.of()));
    }
}
