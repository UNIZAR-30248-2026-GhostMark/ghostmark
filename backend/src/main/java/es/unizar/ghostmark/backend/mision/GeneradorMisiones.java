package es.unizar.ghostmark.backend.mision;

import java.util.List;

public interface GeneradorMisiones {

    List<MisionGenerada> generar(int cuantas, String contexto, List<String> evitar);
}
