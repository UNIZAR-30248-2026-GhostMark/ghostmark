package es.unizar.ghostmark.backend.mision;

import java.net.URI;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeneradorConfig {

    static final URI API_GEMINI = URI.create("https://generativelanguage.googleapis.com/v1beta/");
    static final Duration LIMITE = Duration.ofSeconds(15);

    @Bean
    GeneradorMisiones generadorMisiones(@Value("${GEMINI_CLAVE:}") String clave,
            @Value("${GEMINI_MODELO:}") String modelo) {
        if (clave.isBlank()) {
            return new GeneradorSimulado();
        }
        if (modelo.isBlank()) {
            throw new IllegalStateException("Hay GEMINI_CLAVE pero falta GEMINI_MODELO");
        }
        return new GeneradorGemini(clave, modelo, API_GEMINI, LIMITE);
    }
}
