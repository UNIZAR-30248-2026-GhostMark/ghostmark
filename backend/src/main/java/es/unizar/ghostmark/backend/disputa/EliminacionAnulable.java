package es.unizar.ghostmark.backend.disputa;

import java.time.Instant;
import java.util.UUID;

/** Evento sincrono para conectar con la anulacion de la tarea 11b.1 (#296). */
public record EliminacionAnulable(UUID partidaId, UUID eventoEliminacionId, Instant ahora) {
}
