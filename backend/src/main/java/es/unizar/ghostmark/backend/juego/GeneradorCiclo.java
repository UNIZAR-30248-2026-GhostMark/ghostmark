package es.unizar.ghostmark.backend.juego;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GeneradorCiclo {

    private final SecureRandom aleatorio = new SecureRandom();

    public List<UUID> formar(List<UUID> jugadores) {
        List<UUID> orden = new ArrayList<>(jugadores);
        Collections.shuffle(orden, aleatorio);
        return orden;
    }
}
