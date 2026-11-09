package es.unizar.ghostmark.backend.mision;

import java.util.List;

public record TandaMisiones(int tanda, List<MisionIa> misiones, int descartadas,
        int tandasRestantes) {
}
