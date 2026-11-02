package es.unizar.ghostmark.backend.juego;

import es.unizar.ghostmark.backend.cuenta.CuentaRepository;
import es.unizar.ghostmark.backend.evento.ServicioEvento;
import es.unizar.ghostmark.backend.inscripcion.InscripcionRepository;
import es.unizar.ghostmark.backend.partida.Partida;
import es.unizar.ghostmark.backend.partida.PartidaNoEncontradaException;
import es.unizar.ghostmark.backend.partida.PartidaRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioEliminacion {

    private static final int INTENTOS = 3;
    private static final long PASO_SEGUNDOS = 60;
    private static final long TOLERANCIA_SEGUNDOS = 15;
    private static final int PASOS_CADUCADOS = 10;

    private final CuentaRepository cuentas;
    private final PartidaRepository partidas;
    private final InscripcionRepository inscripciones;
    private final JugadorPartidaRepository jugadores;
    private final CartaRepository cartas;
    private final ServicioEvento eventos;
    private final Clock reloj;
    private final GeneradorCodigoVida generador = new GeneradorCodigoVida();

    public ServicioEliminacion(CuentaRepository cuentas, PartidaRepository partidas,
            InscripcionRepository inscripciones, JugadorPartidaRepository jugadores,
            CartaRepository cartas, ServicioEvento eventos, Clock reloj) {
        this.cuentas = cuentas;
        this.partidas = partidas;
        this.inscripciones = inscripciones;
        this.jugadores = jugadores;
        this.cartas = cartas;
        this.eventos = eventos;
        this.reloj = reloj;
    }

    @Transactional(noRollbackFor = ErrorJuego.class)
    public Eliminacion eliminar(UUID cuentaId, UUID partidaId, String codigo) {
        comprobarFormato(codigo);
        if (cuentas.bloquearPorId(cuentaId).isEmpty()) {
            throw new ErrorJuego(HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO",
                    "La cuenta no está disponible");
        }
        Partida partida = partidas.bloquearPorId(partidaId)
                .orElseThrow(PartidaNoEncontradaException::new);
        UUID inscripcionId = inscripciones.idDeCuentaEnPartida(partidaId, cuentaId)
                .orElseThrow(PartidaNoEncontradaException::new);
        if (!"EN_JUEGO".equals(partida.getEstado())) {
            throw new ErrorJuego(HttpStatus.CONFLICT, "PARTIDA_NO_EN_JUEGO",
                    "La partida no está en juego");
        }
        JugadorPartidaRepository.EstadoJugador cazador = jugadores
                .bloquear(partidaId, inscripcionId)
                .orElseThrow(() -> new IllegalStateException("La partida no tiene al jugador"));
        if (!"VIVO".equals(cazador.estado())) {
            throw new ErrorJuego(HttpStatus.CONFLICT, "JUGADOR_ELIMINADO",
                    "El jugador está eliminado");
        }
        Instant ahora = Instant.now(reloj);
        if (cazador.bloqueadoHasta() != null && ahora.isBefore(cazador.bloqueadoHasta())) {
            throw new ErrorJuego(HttpStatus.CONFLICT, "INTENTOS_BLOQUEADOS",
                    "Los intentos están bloqueados hasta que rote el código",
                    Map.of("bloqueadoHasta", cazador.bloqueadoHasta()));
        }
        UUID victimaId = cazador.objetivoId();
        if (victimaId == null) {
            throw new IllegalStateException("El jugador vivo no tiene objetivo");
        }
        JugadorPartidaRepository.EstadoJugador victima = jugadores.obtener(partidaId, victimaId)
                .orElseThrow(() -> new IllegalStateException("La partida no tiene a la víctima"));

        if ("VIVO".equals(victima.estado()) && coincide(victima.secreto(), codigo, ahora)) {
            return eliminar(partidaId, inscripcionId, victimaId, cazador, victima, ahora);
        }
        throw fallo(partidaId, inscripcionId, victima, codigo, ahora);
    }

    private Eliminacion eliminar(UUID partidaId, UUID cazadorId, UUID victimaId,
            JugadorPartidaRepository.EstadoJugador cazador,
            JugadorPartidaRepository.EstadoJugador victima, Instant ahora) {
        boolean unicoVivo = cazadorId.equals(victima.objetivoId());
        UUID objetivoHeredado = unicoVivo ? null : victima.objetivoId();
        UUID misionHeredada = unicoVivo ? null : victima.misionId();
        jugadores.eliminar(partidaId, victimaId, ahora);
        jugadores.heredar(partidaId, cazadorId, objetivoHeredado, misionHeredada);
        eventos.guardar(partidaId, "JUGADOR_ELIMINADO",
                datos(cazadorId, victimaId, cazador.misionId(), objetivoHeredado, misionHeredada),
                ahora);
        Carta carta = cartas.obtener(partidaId, cazadorId)
                .orElseThrow(() -> new IllegalStateException("La partida no tiene la carta"));
        return new Eliminacion(inscripciones.aliasDeInscripcion(victimaId), carta);
    }

    private ErrorJuego fallo(UUID partidaId, UUID inscripcionId,
            JugadorPartidaRepository.EstadoJugador victima, String codigo, Instant ahora) {
        int fallos = jugadores.sumarFallo(partidaId, inscripcionId);
        if (fallos >= INTENTOS) {
            Instant siguientePaso = Instant.ofEpochSecond(
                    (Math.floorDiv(ahora.getEpochSecond(), PASO_SEGUNDOS) + 1) * PASO_SEGUNDOS);
            jugadores.bloquearIntentos(partidaId, inscripcionId, siguientePaso);
            return new ErrorJuego(HttpStatus.CONFLICT, "INTENTOS_BLOQUEADOS",
                    "Los intentos están bloqueados hasta que rote el código",
                    Map.of("bloqueadoHasta", siguientePaso));
        }
        if (esCaducado(victima.secreto(), codigo, ahora)) {
            return new ErrorJuego(HttpStatus.UNPROCESSABLE_ENTITY, "CODIGO_CADUCADO",
                    "El código ha caducado", Map.of("intentosRestantes", INTENTOS - fallos));
        }
        return new ErrorJuego(HttpStatus.UNPROCESSABLE_ENTITY, "CODIGO_INCORRECTO",
                "El código no es el de tu objetivo",
                Map.of("intentosRestantes", INTENTOS - fallos));
    }

    private boolean coincide(byte[] secreto, String codigo, Instant ahora) {
        if (generador.generar(secreto, ahora).codigo().equals(codigo)) {
            return true;
        }
        boolean principioDelPaso = ahora.getEpochSecond() % PASO_SEGUNDOS < TOLERANCIA_SEGUNDOS;
        return principioDelPaso
                && generador.generar(secreto, ahora.minusSeconds(PASO_SEGUNDOS))
                        .codigo().equals(codigo);
    }

    private boolean esCaducado(byte[] secreto, String codigo, Instant ahora) {
        for (int paso = 1; paso <= PASOS_CADUCADOS; paso++) {
            if (generador.generar(secreto, ahora.minusSeconds(PASO_SEGUNDOS * paso))
                    .codigo().equals(codigo)) {
                return true;
            }
        }
        return false;
    }

    private void comprobarFormato(String codigo) {
        if (codigo == null || !codigo.matches("[0-9]{6}")) {
            throw new ErrorJuego(HttpStatus.BAD_REQUEST, "DATOS_NO_VALIDOS",
                    "Algún campo no cumple sus reglas",
                    Map.of("errores", List.of(Map.of("campo", "codigo",
                            "mensaje", "Debe tener 6 dígitos"))));
        }
    }

    private static String datos(UUID cazadorId, UUID victimaId, UUID misionId,
            UUID objetivoHeredadoId, UUID misionHeredadaId) {
        return "{\"cazadorId\":\"" + cazadorId + "\",\"victimaId\":\"" + victimaId + "\","
                + "\"misionId\":" + valor(misionId) + ","
                + "\"objetivoHeredadoId\":" + valor(objetivoHeredadoId) + ","
                + "\"misionHeredadaId\":" + valor(misionHeredadaId) + "}";
    }

    private static String valor(UUID id) {
        return id == null ? "null" : "\"" + id + "\"";
    }
}
