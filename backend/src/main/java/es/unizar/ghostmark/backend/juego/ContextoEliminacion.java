package es.unizar.ghostmark.backend.juego;

import java.time.Instant;
import java.util.UUID;

public record ContextoEliminacion(UUID partidaId, UUID cazadorId, UUID victimaId, Instant ahora) {
}
