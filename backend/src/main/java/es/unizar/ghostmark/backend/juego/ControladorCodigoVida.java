package es.unizar.ghostmark.backend.juego;

import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/partidas/{partidaId}/yo/codigo")
public class ControladorCodigoVida {

    private final ServicioCodigoVida servicio;

    public ControladorCodigoVida(ServicioCodigoVida servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public ResponseEntity<CodigoVida> obtener(Authentication autenticacion,
            @PathVariable UUID partidaId) {
        CodigoVida codigo = servicio.obtener(UUID.fromString(autenticacion.getName()), partidaId);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(codigo);
    }
}
