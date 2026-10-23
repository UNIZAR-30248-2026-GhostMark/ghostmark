package es.unizar.ghostmark.backend.partida;

import java.time.Instant;

public record CambiosPartida(
        String nombre,
        Instant inicioEn,
        Instant finEn,
        String tipoMisiones,
        Integer plazoDisputaHoras,
        Boolean entradasTardias,
        Boolean masterJuega) {

    boolean vacio() {
        return nombre == null && inicioEn == null && finEn == null && tipoMisiones == null
                && plazoDisputaHoras == null && entradasTardias == null && masterJuega == null;
    }
}
