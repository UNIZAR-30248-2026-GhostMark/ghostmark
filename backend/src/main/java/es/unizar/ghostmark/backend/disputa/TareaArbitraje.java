package es.unizar.ghostmark.backend.disputa;

import java.time.Clock;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Reasigna las disputas que superan el plazo del arbitro actual. */
@Component
public class TareaArbitraje {

    private final ServicioArbitraje arbitraje;
    private final Clock reloj;

    public TareaArbitraje(ServicioArbitraje arbitraje, Clock reloj) {
        this.arbitraje = arbitraje;
        this.reloj = reloj;
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void revisarAhora() {
        revisar(Instant.now(reloj));
    }

    public void revisar(Instant ahora) {
        for (ServicioArbitraje.Pendiente pendiente : arbitraje.vencidas(ahora)) {
            arbitraje.revisarVencimiento(pendiente.partidaId(), pendiente.disputaId(), ahora);
        }
    }
}
