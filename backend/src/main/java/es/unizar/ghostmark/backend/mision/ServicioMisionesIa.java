package es.unizar.ghostmark.backend.mision;

import es.unizar.ghostmark.backend.evento.ServicioEvento;
import es.unizar.ghostmark.backend.juego.ErrorJuego;
import es.unizar.ghostmark.backend.partida.PartidaNoEncontradaException;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

@Service
public class ServicioMisionesIa {

    static final int PEDIDAS = 12;
    static final int MAXIMO_TANDAS = 5;
    private static final Set<String> DECISIONES = Set.of("APROBADA", "RECHAZADA");

    private final MisionIaRepository misiones;
    private final GeneradorMisiones generador;
    private final ServicioEvento eventos;
    private final TransactionTemplate transaccion;
    private final Clock reloj;
    private final FiltroMisiones filtro = new FiltroMisiones();

    public ServicioMisionesIa(MisionIaRepository misiones, GeneradorMisiones generador,
            ServicioEvento eventos, PlatformTransactionManager transacciones, Clock reloj) {
        this.misiones = misiones;
        this.generador = generador;
        this.eventos = eventos;
        this.transaccion = new TransactionTemplate(transacciones);
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public MisionesIa listar(UUID cuentaId, UUID partidaId) {
        comprobarMaster(cuentaId, partidaId, false);
        return new MisionesIa(misiones.dePartida(partidaId),
                MAXIMO_TANDAS - misiones.tandas(partidaId));
    }

    public TandaMisiones pedirTanda(UUID cuentaId, UUID partidaId, NuevaTanda peticion) {
        String contexto = contexto(peticion);
        List<String> existentes = transaccion.execute(estado -> {
            comprobarTanda(comprobarMaster(cuentaId, partidaId, false), partidaId);
            return misiones.textos(partidaId);
        });
        List<MisionGenerada> generadas;
        try {
            generadas = generador.generar(PEDIDAS, contexto, existentes);
        } catch (GeneradorNoDisponible error) {
            throw new ErrorJuego(HttpStatus.SERVICE_UNAVAILABLE, "IA_NO_DISPONIBLE",
                    "La IA no responde: se usarán misiones de la biblioteca");
        }
        return transaccion.execute(estado -> guardarTanda(cuentaId, partidaId, contexto,
                generadas));
    }

    @Transactional
    public MisionIa revisar(UUID cuentaId, UUID partidaId, UUID misionId,
            RevisionMision peticion) {
        String decision = peticion.estado();
        if (decision == null || !DECISIONES.contains(decision)) {
            throw datosNoValidos("estado", "Debe ser APROBADA o RECHAZADA");
        }
        comprobarAbierta(comprobarMaster(cuentaId, partidaId, true));
        MisionIa mision = misiones.bloquear(partidaId, misionId).orElseThrow(
                () -> new ErrorJuego(HttpStatus.NOT_FOUND, "NO_ENCONTRADA",
                        "Misión no encontrada"));
        if (!"PROPUESTA".equals(mision.estado())) {
            throw new ErrorJuego(HttpStatus.CONFLICT, "MISION_YA_REVISADA",
                    "La misión ya está revisada");
        }
        Instant ahora = Instant.now(reloj);
        misiones.revisar(misionId, decision, ahora);
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("misionId", misionId.toString());
        datos.put("decision", decision);
        eventos.guardar(partidaId, "MISION_REVISADA", JsonMapper.shared().writeValueAsString(datos),
                ahora);
        return new MisionIa(misionId, mision.texto(), decision, mision.tanda());
    }

    private TandaMisiones guardarTanda(UUID cuentaId, UUID partidaId, String contexto,
            List<MisionGenerada> generadas) {
        int tanda = comprobarTanda(comprobarMaster(cuentaId, partidaId, true), partidaId) + 1;
        FiltroMisiones.Resultado resultado = filtro.filtrar(generadas,
                misiones.textos(partidaId));
        Instant ahora = Instant.now(reloj);
        List<MisionIa> nuevas = resultado.aceptadas().stream()
                .map(mision -> new MisionIa(misiones.crear(partidaId, mision.texto(), tanda, ahora),
                        mision.texto(), "PROPUESTA", tanda))
                .toList();
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("tanda", tanda);
        datos.put("contexto", contexto);
        datos.put("misionIds", nuevas.stream().map(mision -> mision.id().toString()).toList());
        datos.put("descartadas", resultado.descartadas());
        eventos.guardar(partidaId, "MISIONES_GENERADAS",
                JsonMapper.shared().writeValueAsString(datos), ahora);
        return new TandaMisiones(tanda, nuevas, resultado.descartadas(), MAXIMO_TANDAS - tanda);
    }

    private MisionIaRepository.DatosPartida comprobarMaster(UUID cuentaId, UUID partidaId,
            boolean bloquear) {
        if (!misiones.cuentaExiste(cuentaId, bloquear)) {
            throw new ErrorJuego(HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO",
                    "La cuenta no está disponible");
        }
        MisionIaRepository.DatosPartida partida = misiones.partida(partidaId, bloquear)
                .orElseThrow(PartidaNoEncontradaException::new);
        if (!cuentaId.equals(partida.masterId())) {
            if (misiones.inscrita(partidaId, cuentaId)) {
                throw new ErrorJuego(HttpStatus.FORBIDDEN, "NO_ES_MASTER",
                        "Solo el máster puede gestionar las misiones de la IA");
            }
            throw new PartidaNoEncontradaException();
        }
        return partida;
    }

    private static void comprobarAbierta(MisionIaRepository.DatosPartida partida) {
        if (!"EN_INSCRIPCION".equals(partida.estado())) {
            throw new ErrorJuego(HttpStatus.CONFLICT, "PARTIDA_NO_ABIERTA",
                    "La partida ya no está en inscripción");
        }
    }

    private int comprobarTanda(MisionIaRepository.DatosPartida partida, UUID partidaId) {
        comprobarAbierta(partida);
        if (!"IA".equals(partida.tipoMisiones())) {
            throw new ErrorJuego(HttpStatus.CONFLICT, "MISIONES_DE_BIBLIOTECA",
                    "La partida usa las misiones de la biblioteca");
        }
        int tandas = misiones.tandas(partidaId);
        if (tandas >= MAXIMO_TANDAS) {
            throw new ErrorJuego(HttpStatus.CONFLICT, "LIMITE_TANDAS",
                    "La partida ya ha pedido " + MAXIMO_TANDAS + " tandas");
        }
        return tandas;
    }

    private static String contexto(NuevaTanda peticion) {
        String contexto = peticion.contexto() == null ? "" : peticion.contexto().strip();
        int longitud = contexto.codePointCount(0, contexto.length());
        if (longitud < 3 || longitud > 40 || contexto.chars().anyMatch(Character::isISOControl)) {
            throw datosNoValidos("contexto", "Debe tener de 3 a 40 caracteres en una línea");
        }
        return contexto;
    }

    private static ErrorJuego datosNoValidos(String campo, String mensaje) {
        return new ErrorJuego(HttpStatus.BAD_REQUEST, "DATOS_NO_VALIDOS",
                "Algún campo no cumple sus reglas",
                Map.of("errores", List.of(Map.of("campo", campo, "mensaje", mensaje))));
    }
}
