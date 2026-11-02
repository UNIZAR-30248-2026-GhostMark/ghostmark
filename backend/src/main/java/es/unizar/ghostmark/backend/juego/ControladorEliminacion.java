package es.unizar.ghostmark.backend.juego;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/partidas/{partidaId}/eliminaciones")
public class ControladorEliminacion {

    private final ServicioEliminacion servicio;

    public ControladorEliminacion(ServicioEliminacion servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public Eliminacion eliminar(Authentication autenticacion, @PathVariable UUID partidaId,
            @RequestBody NuevaEliminacion peticion) {
        return servicio.eliminar(UUID.fromString(autenticacion.getName()), partidaId,
                peticion.codigo());
    }
}
