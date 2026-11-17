package es.unizar.ghostmark.backend.disputa;

import es.unizar.ghostmark.backend.juego.ContextoEliminacion;
import es.unizar.ghostmark.backend.juego.ErrorJuego;
import es.unizar.ghostmark.backend.juego.ReglaEliminacion;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class ReglaDisputaAbierta implements ReglaEliminacion {

    private final DisputaRepository disputas;

    public ReglaDisputaAbierta(DisputaRepository disputas) {
        this.disputas = disputas;
    }

    @Override
    public Optional<ErrorJuego> comprobar(ContextoEliminacion contexto) {
        if (!disputas.bloqueaAlCazador(contexto.cazadorId())) {
            return Optional.empty();
        }
        return Optional.of(new ErrorJuego(HttpStatus.CONFLICT, "DISPUTA_ABIERTA",
                "Tienes una disputa abierta sobre una eliminación tuya"));
    }
}
