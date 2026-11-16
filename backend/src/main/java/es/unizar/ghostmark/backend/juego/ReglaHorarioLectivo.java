package es.unizar.ghostmark.backend.juego;

import java.util.Map;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class ReglaHorarioLectivo implements ReglaEliminacion {

    private final HorarioLectivo horario;

    public ReglaHorarioLectivo(HorarioLectivo horario) {
        this.horario = horario;
    }

    @Override
    public Optional<ErrorJuego> comprobar(ContextoEliminacion contexto) {
        return horario.noDisponible(contexto.cazadorId(), contexto.victimaId(), contexto.ahora())
                .map(noDisponible -> new ErrorJuego(HttpStatus.CONFLICT, "HORARIO_LECTIVO",
                        "CAZADOR".equals(noDisponible.motivo())
                                ? "Estás en tu horario lectivo"
                                : "Tu objetivo está en su horario lectivo",
                        Map.of("motivo", noDisponible.motivo(),
                                "disponibleEn", noDisponible.disponibleEn())));
    }
}
