package es.unizar.ghostmark.backend.juego;

import java.time.Instant;
import java.util.UUID;

public record EliminacionHecha(UUID partidaId, Instant ahora) {
}
