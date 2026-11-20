package es.unizar.ghostmark.backend.disputa;

import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/partidas/{partidaId}/disputas/{disputaId}/decision")
public class ControladorArbitraje {

    private final ServicioArbitraje arbitraje;

    public ControladorArbitraje(ServicioArbitraje arbitraje) {
        this.arbitraje = arbitraje;
    }

    @PostMapping
    public Map<String, Object> decidir(Authentication autenticacion, @PathVariable UUID partidaId,
            @PathVariable UUID disputaId, @RequestBody DecisionDisputa peticion) {
        return arbitraje.decidir(UUID.fromString(autenticacion.getName()), partidaId, disputaId,
                peticion.razon());
    }
}
