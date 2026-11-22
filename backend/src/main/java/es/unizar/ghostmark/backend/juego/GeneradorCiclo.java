package es.unizar.ghostmark.backend.juego;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GeneradorCiclo {

    static final int MAXIMO_ORDENES = 100;

    private final SecureRandom aleatorio = new SecureRandom();

    public List<UUID> formar(List<UUID> jugadores) {
        List<UUID> orden = new ArrayList<>(jugadores);
        Collections.shuffle(orden, aleatorio);
        return orden;
    }

    public List<UUID> formarEvitando(List<UUID> jugadores, UUID cazador, Set<UUID> revividos) {
        List<UUID> orden = formar(jugadores);
        for (int intento = 1; intento < MAXIMO_ORDENES
                && emparejaImplicados(orden, cazador, revividos); intento++) {
            orden = formar(jugadores);
        }
        return orden;
    }

    static boolean emparejaImplicados(List<UUID> orden, UUID cazador, Set<UUID> revividos) {
        if (cazador == null) {
            return false;
        }
        for (int i = 0; i < orden.size(); i++) {
            UUID uno = orden.get(i);
            UUID otro = orden.get((i + 1) % orden.size());
            if ((cazador.equals(uno) && revividos.contains(otro))
                    || (cazador.equals(otro) && revividos.contains(uno))) {
                return true;
            }
        }
        return false;
    }
}
