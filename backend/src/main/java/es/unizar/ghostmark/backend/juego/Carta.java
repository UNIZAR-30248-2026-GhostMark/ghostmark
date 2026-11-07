package es.unizar.ghostmark.backend.juego;

import java.time.Instant;

public record Carta(String estado, String aliasObjetivo, String mision, int eliminaciones,
        Integer intentosRestantes, Instant bloqueadoHasta) {
}
