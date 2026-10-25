package es.unizar.ghostmark.backend.inscripcion;

import es.unizar.ghostmark.backend.cuenta.dto.Horario;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/partidas/{partidaId}/yo/horario")
public class ControladorHorarioPartida {

    private final ServicioHorarioPartida servicio;

    public ControladorHorarioPartida(ServicioHorarioPartida servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public Horario obtener(Authentication autenticacion, @PathVariable UUID partidaId) {
        return servicio.obtener(UUID.fromString(autenticacion.getName()), partidaId);
    }

    @PutMapping
    public Horario guardar(Authentication autenticacion, @PathVariable UUID partidaId,
            @RequestBody Horario horario) {
        return servicio.guardar(UUID.fromString(autenticacion.getName()), partidaId, horario);
    }
}
