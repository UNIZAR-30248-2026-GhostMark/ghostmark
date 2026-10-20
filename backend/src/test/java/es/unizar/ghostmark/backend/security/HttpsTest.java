
package es.unizar.ghostmark.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class HttpsTest {

    private static final String SERVIDOR =
            "ghostmark-api-kx1q.onrender.com/api/v1/health";

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
