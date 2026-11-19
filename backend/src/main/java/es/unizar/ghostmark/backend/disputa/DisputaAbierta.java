package es.unizar.ghostmark.backend.disputa;

import java.time.Instant;
import java.util.UUID;

public record DisputaAbierta(UUID partidaId, UUID disputaId, Instant ahora) {
}
