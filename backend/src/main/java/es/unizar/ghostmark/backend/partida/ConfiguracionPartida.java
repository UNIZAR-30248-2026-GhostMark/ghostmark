package es.unizar.ghostmark.backend.partida;

import java.time.Instant;

public record ConfiguracionPartida(
        String nombre,
        Instant inicioEn,
        Instant finEn,
        String tipoMisiones,
        Integer plazoDisputaHoras,
        Boolean entradasTardias,
        Boolean masterJuega) {
}
