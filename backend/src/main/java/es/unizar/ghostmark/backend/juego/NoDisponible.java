package es.unizar.ghostmark.backend.juego;

import java.time.Instant;

public record NoDisponible(String motivo, Instant disponibleEn) {
}
