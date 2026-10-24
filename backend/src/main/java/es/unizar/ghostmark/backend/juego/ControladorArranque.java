package es.unizar.ghostmark.backend.juego;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/partidas/{partidaId}/arranque")
public class ControladorArranque {

    private final ServicioArranque arranque;
    private final Clock reloj;

    public ControladorArranque(ServicioArranque arranque, Clock reloj) {
        this.arranque = arranque;
        this.reloj = reloj;
    }

    @PostMapping
    public Map<String, Object> arrancarYa(Authentication autenticacion,
            @PathVariable UUID partidaId) {
        return arranque.arrancarYa(UUID.fromString(autenticacion.getName()), partidaId,
                Instant.now(reloj));
    }
}
