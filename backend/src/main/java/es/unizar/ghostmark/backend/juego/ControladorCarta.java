package es.unizar.ghostmark.backend.juego;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/partidas/{partidaId}/yo/carta")
public class ControladorCarta {

    private final ServicioCarta servicio;

    public ControladorCarta(ServicioCarta servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public Carta obtener(Authentication autenticacion, @PathVariable UUID partidaId) {
        return servicio.obtener(UUID.fromString(autenticacion.getName()), partidaId);
    }
}
