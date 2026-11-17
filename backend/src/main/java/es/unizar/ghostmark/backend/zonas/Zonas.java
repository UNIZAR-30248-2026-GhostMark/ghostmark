package es.unizar.ghostmark.backend.zonas;

import java.util.List;
import java.util.UUID;

/** Lista de zonas de una partida, según el contrato OpenAPI de la 10a. */
public record Zonas(List<Zona> zonas) {

    public record Zona(UUID id, String nombre, List<Punto> puntos) {
    }

    public record Punto(Double lat, Double lon) {
    }
}
