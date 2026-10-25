package es.unizar.ghostmark.backend.juego;

import es.unizar.ghostmark.backend.partida.PartidaRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TareaArranque {

    private final PartidaRepository partidas;
    private final ServicioArranque arranque;
    private final Clock reloj;

    public TareaArranque(PartidaRepository partidas, ServicioArranque arranque, Clock reloj) {
        this.partidas = partidas;
        this.arranque = arranque;
        this.reloj = reloj;
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void revisarAhora() {
        revisar(Instant.now(reloj));
    }

    public void revisar(Instant ahora) {
        for (UUID partidaId : partidas.pendientesDeArrancar(ahora)) {
            arranque.aplicarInicio(partidaId, ahora);
        }
    }
}
