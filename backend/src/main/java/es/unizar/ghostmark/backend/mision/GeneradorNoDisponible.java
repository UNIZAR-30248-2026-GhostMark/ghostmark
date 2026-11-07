package es.unizar.ghostmark.backend.mision;

public class GeneradorNoDisponible extends RuntimeException {

    public GeneradorNoDisponible(String mensaje) {
        super(mensaje);
    }

    public GeneradorNoDisponible(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
