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
@RequestMapping("/api/v1/partidas/{partidaId}/cierre")
public class ControladorCierre {

    private final ServicioCierre cierre;
    private final Clock reloj;

    public ControladorCierre(ServicioCierre cierre, Clock reloj) {
        this.cierre = cierre;
        this.reloj = reloj;
    }

    @PostMapping
    public Map<String, Object> cerrar(Authentication autenticacion,
            @PathVariable UUID partidaId) {
        return cierre.cerrarPorMaster(UUID.fromString(autenticacion.getName()), partidaId,
                Instant.now(reloj));
    }
}
