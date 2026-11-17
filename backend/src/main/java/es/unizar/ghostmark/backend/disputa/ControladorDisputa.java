package es.unizar.ghostmark.backend.disputa;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/partidas/{partidaId}/disputas")
public class ControladorDisputa {

    private final ServicioDisputa servicio;

    public ControladorDisputa(ServicioDisputa servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<Map<String, Object>> listar(Authentication autenticacion,
            @PathVariable UUID partidaId) {
        return servicio.listar(UUID.fromString(autenticacion.getName()), partidaId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> abrir(Authentication autenticacion, @PathVariable UUID partidaId,
            @RequestBody NuevaDisputa peticion) {
        return servicio.abrir(UUID.fromString(autenticacion.getName()), partidaId, peticion);
    }

    @GetMapping("/{disputaId}")
    public Map<String, Object> obtener(Authentication autenticacion,
            @PathVariable UUID partidaId, @PathVariable UUID disputaId) {
        return servicio.obtener(UUID.fromString(autenticacion.getName()), partidaId, disputaId);
    }

    @PostMapping("/{disputaId}/respuesta")
    public Map<String, Object> responder(Authentication autenticacion,
            @PathVariable UUID partidaId, @PathVariable UUID disputaId,
            @RequestBody TextoDisputa peticion) {
        return servicio.responder(UUID.fromString(autenticacion.getName()), partidaId,
                disputaId, peticion);
    }
}
