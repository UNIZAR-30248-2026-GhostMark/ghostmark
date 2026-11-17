package es.unizar.ghostmark.backend.juego;

import es.unizar.ghostmark.backend.cuenta.CuentaRepository;
import es.unizar.ghostmark.backend.evento.ServicioEvento;
import es.unizar.ghostmark.backend.inscripcion.InscripcionRepository;
import es.unizar.ghostmark.backend.partida.Partida;
import es.unizar.ghostmark.backend.partida.PartidaNoEncontradaException;
import es.unizar.ghostmark.backend.partida.PartidaRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioEliminacion {

    private static final int INTENTOS = 3;
    private static final long PASO_SEGUNDOS = 60;
    private static final long TOLERANCIA_SEGUNDOS = 15;
    private static final int PASOS_CADUCADOS = 10;
    private static final Set<String> RESULTADOS_ZONA = Set.of("DENTRO", "FUERA",
            "NO_CONCLUYENTE");

    private final CuentaRepository cuentas;
    private final PartidaRepository partidas;
    private final InscripcionRepository inscripciones;
    private final JugadorPartidaRepository jugadores;
    private final CartaRepository cartas;
    private final ServicioEvento eventos;
    private final ServicioCierre cierre;
    private final List<ReglaEliminacion> reglas;
    private final ApplicationEventPublisher publicador;
    private final Clock reloj;
    private final GeneradorCodigoVida generador = new GeneradorCodigoVida();

    public ServicioEliminacion(CuentaRepository cuentas, PartidaRepository partidas,
            InscripcionRepository inscripciones, JugadorPartidaRepository jugadores,
            CartaRepository cartas, ServicioEvento eventos, ServicioCierre cierre,
            List<ReglaEliminacion> reglas, ApplicationEventPublisher publicador, Clock reloj) {
        this.cuentas = cuentas;
        this.partidas = partidas;
        this.inscripciones = inscripciones;
        this.jugadores = jugadores;
        this.cartas = cartas;
        this.eventos = eventos;
        this.cierre = cierre;
        this.reglas = reglas;
        this.publicador = publicador;
        this.reloj = reloj;
    }

    @Transactional(noRollbackFor = ErrorJuego.class)
    public Eliminacion eliminar(UUID cuentaId, UUID partidaId, NuevaEliminacion peticion) {
        comprobarFormato(peticion);
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
        ContextoEliminacion contexto = new ContextoEliminacion(partidaId, inscripcionId,
                victimaId, ahora);
        for (ReglaEliminacion regla : reglas) {
            Optional<ErrorJuego> rechazo = regla.comprobar(contexto);
            if (rechazo.isPresent()) {
                throw rechazo.get();
            }
        }

        String codigo = peticion.getCodigo();
        if ("VIVO".equals(victima.estado()) && coincide(victima.secreto(), codigo, ahora)) {
            return eliminar(partidaId, inscripcionId, victimaId, cazador, victima,
                    peticion.getResultadoZona(), ahora);
        }
        throw fallo(partidaId, inscripcionId, victima, codigo, ahora);
    }

    private Eliminacion eliminar(UUID partidaId, UUID cazadorId, UUID victimaId,
            JugadorPartidaRepository.EstadoJugador cazador,
            JugadorPartidaRepository.EstadoJugador victima, String resultadoZona, Instant ahora) {
        boolean unicoVivo = cazadorId.equals(victima.objetivoId());
        UUID objetivoHeredado = unicoVivo ? null : victima.objetivoId();
        UUID misionHeredada = unicoVivo ? null : victima.misionId();
        jugadores.eliminar(partidaId, victimaId, ahora);
        jugadores.heredar(partidaId, cazadorId, objetivoHeredado, misionHeredada);
        eventos.guardar(partidaId, "JUGADOR_ELIMINADO",
                datos(cazadorId, victimaId, cazador.misionId(), objetivoHeredado, misionHeredada,
                        resultadoZona), ahora);
        publicador.publishEvent(new EliminacionHecha(partidaId, ahora));
        cierre.cerrarSiUltimoVivo(partidaId, ahora);
        Carta carta = cartas.obtener(partidaId, cazadorId, ahora)
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

    private void comprobarFormato(NuevaEliminacion peticion) {
        List<Map<String, String>> errores = new ArrayList<>();
        for (String campo : peticion.camposNoPermitidos()) {
            errores.add(Map.of("campo", campo, "mensaje", "Campo no permitido"));
        }
        String codigo = peticion.getCodigo();
        if (codigo == null || !codigo.matches("[0-9]{6}")) {
            errores.add(Map.of("campo", "codigo", "mensaje", "Debe tener 6 dígitos"));
        }
        String resultadoZona = peticion.getResultadoZona();
        if (resultadoZona == null || !RESULTADOS_ZONA.contains(resultadoZona)) {
            errores.add(Map.of("campo", "resultadoZona",
                    "mensaje", "Debe ser DENTRO, FUERA o NO_CONCLUYENTE"));
        }
        if (!errores.isEmpty()) {
            throw new ErrorJuego(HttpStatus.BAD_REQUEST, "DATOS_NO_VALIDOS",
                    "Algún campo no cumple sus reglas", Map.of("errores", errores));
        }
    }

    private static String datos(UUID cazadorId, UUID victimaId, UUID misionId,
            UUID objetivoHeredadoId, UUID misionHeredadaId, String resultadoZona) {
        return "{\"cazadorId\":\"" + cazadorId + "\",\"victimaId\":\"" + victimaId + "\","
                + "\"misionId\":" + valor(misionId) + ","
                + "\"objetivoHeredadoId\":" + valor(objetivoHeredadoId) + ","
                + "\"misionHeredadaId\":" + valor(misionHeredadaId) + ","
                + "\"resultadoZona\":\"" + resultadoZona + "\"}";
    }

    private static String valor(UUID id) {
        return id == null ? "null" : "\"" + id + "\"";
    }
}
