package es.unizar.ghostmark.backend.juego;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

public class GeneradorCodigoVida {

    static final Duration DURACION = Duration.ofSeconds(60);

    private final SecureRandom aleatorio = new SecureRandom();

    public CodigoVida generar(Instant ahora) {
        return new CodigoVida(String.format(Locale.ROOT, "%06d", aleatorio.nextInt(1_000_000)),
                ahora.plus(DURACION));
    }
}
