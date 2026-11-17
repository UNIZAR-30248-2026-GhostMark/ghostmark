package es.unizar.ghostmark.backend.juego;

import java.util.Optional;

public interface ReglaEliminacion {

    Optional<ErrorJuego> comprobar(ContextoEliminacion contexto);
}
