package es.unizar.ghostmark.backend.partida;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/partidas")
public class ControladorPartida {

    private final ServicioPartida partidas;
    private final Clock reloj;

    public ControladorPartida(ServicioPartida partidas, Clock reloj) {
        this.partidas = partidas;
        this.reloj = reloj;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> crear(Authentication autenticacion,
            @RequestBody ConfiguracionPartida configuracion) {
        Map<String, Object> partida = partidas.crear(
                UUID.fromString(autenticacion.getName()), configuracion, Instant.now(reloj));
        return ResponseEntity.status(HttpStatus.CREATED).body(partida);
    }

    @GetMapping("/{partidaId}")
    public Map<String, Object> obtener(Authentication autenticacion,
            @PathVariable String partidaId) {
        return partidas.obtener(UUID.fromString(autenticacion.getName()), idPartida(partidaId));
    }

    @PatchMapping("/{partidaId}")
    public Map<String, Object> editar(Authentication autenticacion,
            @PathVariable String partidaId, @RequestBody CambiosPartida cambios) {
        return partidas.editar(UUID.fromString(autenticacion.getName()), idPartida(partidaId),
                cambios, Instant.now(reloj));
    }

    @GetMapping("/codigo/{codigo}")
    public Map<String, Object> obtenerPorCodigo(@PathVariable String codigo) {
        return partidas.obtenerPorCodigo(codigo);
    }

    private static UUID idPartida(String partidaId) {
        try {
            return UUID.fromString(partidaId);
        } catch (IllegalArgumentException error) {
            throw new PartidaNoEncontradaException();
        }
    }
}
