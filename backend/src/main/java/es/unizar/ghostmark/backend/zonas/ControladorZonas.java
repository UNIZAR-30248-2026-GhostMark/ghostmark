package es.unizar.ghostmark.backend.zonas;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/partidas/{partidaId}/zonas")
public class ControladorZonas {

    private final ServicioZonas servicio;
    private final Clock reloj;

    public ControladorZonas(ServicioZonas servicio, Clock reloj) {
        this.servicio = servicio;
        this.reloj = reloj;
    }

    @GetMapping
    public Zonas listar(Authentication autenticacion, @PathVariable UUID partidaId) {
        return servicio.listar(UUID.fromString(autenticacion.getName()), partidaId);
    }

    @PutMapping
    public Zonas guardar(Authentication autenticacion, @PathVariable UUID partidaId,
            @RequestBody Zonas peticion) {
        return servicio.guardar(UUID.fromString(autenticacion.getName()), partidaId,
                peticion, Instant.now(reloj));
    }
}
