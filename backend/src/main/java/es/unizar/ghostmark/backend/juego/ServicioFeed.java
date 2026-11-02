package es.unizar.ghostmark.backend.juego;

import es.unizar.ghostmark.backend.cuenta.CuentaRepository;
import es.unizar.ghostmark.backend.inscripcion.InscripcionRepository;
import es.unizar.ghostmark.backend.partida.Partida;
import es.unizar.ghostmark.backend.partida.PartidaNoEncontradaException;
import es.unizar.ghostmark.backend.partida.PartidaRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioFeed {

    private final CuentaRepository cuentas;
    private final PartidaRepository partidas;
    private final InscripcionRepository inscripciones;
    private final FeedRepository feed;

    public ServicioFeed(CuentaRepository cuentas, PartidaRepository partidas,
            InscripcionRepository inscripciones, FeedRepository feed) {
        this.cuentas = cuentas;
        this.partidas = partidas;
        this.inscripciones = inscripciones;
        this.feed = feed;
    }

    @Transactional
    public List<EntradaFeed> obtener(UUID cuentaId, UUID partidaId) {
        if (cuentas.bloquearPorId(cuentaId).isEmpty()) {
            throw new ErrorJuego(HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO",
                    "La cuenta no está disponible");
        }
        Partida partida = partidas.findById(partidaId)
                .orElseThrow(PartidaNoEncontradaException::new);
        boolean jugador = inscripciones.existe(partidaId, cuentaId);
        boolean master = cuentaId.equals(partida.getMasterId());
        if (!jugador && !master) {
            throw new PartidaNoEncontradaException();
        }
        if (!"EN_JUEGO".equals(partida.getEstado())
                && !"CERRADA".equals(partida.getEstado())) {
            throw new ErrorJuego(HttpStatus.CONFLICT, "PARTIDA_NO_EN_JUEGO",
                    "La partida no está en juego");
        }
        return feed.eliminaciones(partidaId);
    }
}
