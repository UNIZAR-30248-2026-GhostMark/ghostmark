package es.unizar.ghostmark.backend.mision;

import java.util.UUID;

public record MisionIa(UUID id, String texto, String estado, int tanda) {
}
