package es.unizar.ghostmark.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Comprueba en producción que la API solo responde por HTTPS. Se ejecuta cuando existe la
 * variable {@code URL_PRODUCCION} (por ejemplo {@code ghostmark-api-kx1q.onrender.com}, sin
 * esquema), configurada tanto en la integración continua del backend como en el flujo de
 * despliegue a producción.
 */
@EnabledIfEnvironmentVariable(named = "URL_PRODUCCION", matches = ".+")
class HttpsTest {

    private static final String SERVIDOR = System.getenv("URL_PRODUCCION") + "/api/v1/health";

    private static final HttpClient CLIENTE = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @Test
    void redirigeHttpAHttps() throws Exception {
        // El acceso HTTP debe redirigir al mismo recurso mediante HTTPS.
        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create("http://" + SERVIDOR))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        HttpResponse<Void> respuesta = CLIENTE.send(
                peticion, HttpResponse.BodyHandlers.discarding());

        assertEquals(301, respuesta.statusCode());
        assertEquals("https://" + SERVIDOR, respuesta.headers().firstValue("location").orElseThrow());
    }

    @Test
    void respondeCorrectamentePorHttps() throws Exception {
        // Comprueba la respuesta segura y la cabecera HSTS.
        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create("https://" + SERVIDOR))
                .timeout(Duration.ofSeconds(150))
                .GET()
                .build();

        HttpResponse<String> respuesta = CLIENTE.send(
                peticion, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, respuesta.statusCode());
        assertTrue(respuesta.body().contains("\"status\":\"UP\""));
        assertTrue(respuesta.headers().firstValue("strict-transport-security").orElseThrow().contains("max-age="));
    }
}
