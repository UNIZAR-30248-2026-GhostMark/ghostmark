package es.unizar.ghostmark.backend.juego;

import es.unizar.ghostmark.backend.partida.PartidaRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TareaCierre {

    private final PartidaRepository partidas;
    private final ServicioCierre cierre;
    private final Clock reloj;

    public TareaCierre(PartidaRepository partidas, ServicioCierre cierre, Clock reloj) {
        this.partidas = partidas;
        this.cierre = cierre;
        this.reloj = reloj;
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void revisarAhora() {
        revisar(Instant.now(reloj));
    }

    public void revisar(Instant ahora) {
        for (UUID partidaId : partidas.pendientesDeCerrar(ahora)) {
            cierre.aplicarCierre(partidaId, "FECHA_FIN", ahora);
        }
    }
}
