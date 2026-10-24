package es.unizar.ghostmark.backend.partida;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cuentas/yo/partidas")
public class ControladorMisPartidas {

    private final ServicioPartida partidas;

    public ControladorMisPartidas(ServicioPartida partidas) {
        this.partidas = partidas;
    }

    @GetMapping
    public List<Map<String, Object>> listar(Authentication autenticacion) {
        return partidas.misPartidas(UUID.fromString(autenticacion.getName()));
    }
}
