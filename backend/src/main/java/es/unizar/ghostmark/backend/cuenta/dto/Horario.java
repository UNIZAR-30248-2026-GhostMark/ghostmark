package es.unizar.ghostmark.backend.cuenta.dto;

import java.util.List;

public record Horario(List<Franja> franjas) {

    public record Franja(Integer dia, String horaInicio) {
    }
}
