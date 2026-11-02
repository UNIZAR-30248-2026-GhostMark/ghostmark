package es.unizar.ghostmark.backend.juego;

import java.time.Instant;

public record EntradaFeed(String tipo, String alias, Instant ocurridoEn) {
}
