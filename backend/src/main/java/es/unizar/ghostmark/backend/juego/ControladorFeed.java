package es.unizar.ghostmark.backend.juego;

import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/partidas/{partidaId}/feed")
public class ControladorFeed {

    private final ServicioFeed servicio;

    public ControladorFeed(ServicioFeed servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<EntradaFeed> obtener(Authentication autenticacion, @PathVariable UUID partidaId) {
        return servicio.obtener(UUID.fromString(autenticacion.getName()), partidaId);
    }
}
