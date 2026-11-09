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
            @Value("${GEMINI_MODELO:}") String modelo,
            @Value("${GENERADOR_SIMULADO:false}") boolean simulado,
            @Value("${GENERADOR_SIMULADO_MODO:NORMAL}") GeneradorSimulado.Modo modo) {
        if (simulado) {
            // En local se puede probar el aviso desde la app sin llamar a Gemini
            GeneradorSimulado generador = new GeneradorSimulado();
            generador.usar(modo);
            return generador;
        }
        if (clave.isBlank()) {
            // Sin clave, la IA cuenta como no disponible y la partida sigue con la biblioteca
            // (R11-32): las misiones fijas del simulado nunca llegan a un máster de verdad.
            return (cuantas, contexto, evitar) -> {
                throw new GeneradorNoDisponible("Falta GEMINI_CLAVE en el entorno");
            };
        }
        if (modelo.isBlank()) {
            throw new IllegalStateException("Hay GEMINI_CLAVE pero falta GEMINI_MODELO");
        }
        return new GeneradorGemini(clave, modelo, API_GEMINI, LIMITE);
    }
}
