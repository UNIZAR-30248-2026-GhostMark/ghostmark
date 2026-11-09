package es.unizar.ghostmark.backend.mision;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/partidas/{partidaId}/misiones-ia")
public class ControladorMisionesIa {

    private final ServicioMisionesIa servicio;

    public ControladorMisionesIa(ServicioMisionesIa servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public MisionesIa listar(Authentication autenticacion, @PathVariable UUID partidaId) {
        return servicio.listar(UUID.fromString(autenticacion.getName()), partidaId);
    }

    @PostMapping("/tandas")
    @ResponseStatus(HttpStatus.CREATED)
    public TandaMisiones pedirTanda(Authentication autenticacion, @PathVariable UUID partidaId,
            @RequestBody NuevaTanda peticion) {
        return servicio.pedirTanda(UUID.fromString(autenticacion.getName()), partidaId,
                peticion);
    }

    @PatchMapping("/{misionId}")
    public MisionIa revisar(Authentication autenticacion, @PathVariable UUID partidaId,
            @PathVariable UUID misionId, @RequestBody RevisionMision peticion) {
        return servicio.revisar(UUID.fromString(autenticacion.getName()), partidaId, misionId,
                peticion);
    }
}
