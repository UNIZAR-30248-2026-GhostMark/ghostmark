package es.unizar.ghostmark.backend.partida;

import es.unizar.ghostmark.backend.cuenta.Cuenta;
import es.unizar.ghostmark.backend.cuenta.CuentaRepository;
import es.unizar.ghostmark.backend.evento.ServicioEvento;
import es.unizar.ghostmark.backend.inscripcion.ErrorInscripcion;
import es.unizar.ghostmark.backend.inscripcion.InscripcionRepository;
import es.unizar.ghostmark.backend.inscripcion.ServicioInscripcion;
import es.unizar.ghostmark.backend.sesion.DatosNoValidosException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioPartida {

    private static final Duration ANTELACION_MINIMA = Duration.ofMinutes(15);
    private static final Set<String> TIPOS_MISIONES = Set.of("BIBLIOTECA", "IA");
    private static final Set<Integer> PLAZOS_DISPUTA = Set.of(2, 6, 12, 24);
    private static final String ALFABETO_CODIGO = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    private static final int LONGITUD_CODIGO = 8;
    private static final String INSERTAR_PARTIDA_CREADA = "insert into evento"
            + " (id, partida_id, secuencia, tipo, datos, ocurrido_en)"
            + " values (?, ?, 1, 'PARTIDA_CREADA', jsonb_build_object("
            + "'nombre', ?::text, 'inicioEn', ?::text, 'finEn', ?::text,"
            + " 'tipoMisiones', ?::text, 'plazoDisputaHoras', ?::int,"
            + " 'entradasTardias', ?::boolean, 'masterJuega', ?::boolean), ?)";
    private static final String INSERTAR_CONFIGURACION_CAMBIADA = "insert into evento"
            + " (id, partida_id, secuencia, tipo, datos, ocurrido_en)"
            + " select ?, ?, coalesce(max(secuencia), 0) + 1, 'CONFIGURACION_CAMBIADA',"
            + " jsonb_strip_nulls(jsonb_build_object("
            + "'nombre', ?::text, 'inicioEn', ?::text, 'finEn', ?::text,"
            + " 'tipoMisiones', ?::text, 'plazoDisputaHoras', ?::int,"
            + " 'entradasTardias', ?::boolean, 'masterJuega', ?::boolean)), ?"
            + " from evento where partida_id = ?";

    private final PartidaRepository partidas;
    private final CuentaRepository cuentas;
    private final JdbcTemplate jdbc;
    private final ServicioInscripcion servicioInscripcion;
    private final InscripcionRepository inscripciones;
    private final ServicioEvento eventos;
    private final SecureRandom aleatorio = new SecureRandom();

    public ServicioPartida(PartidaRepository partidas, CuentaRepository cuentas,
            JdbcTemplate jdbc, ServicioInscripcion servicioInscripcion,
            InscripcionRepository inscripciones, ServicioEvento eventos) {
        this.partidas = partidas;
        this.cuentas = cuentas;
        this.jdbc = jdbc;
        this.servicioInscripcion = servicioInscripcion;
        this.inscripciones = inscripciones;
        this.eventos = eventos;
    }

    @Transactional
    public Map<String, Object> crear(UUID masterId, ConfiguracionPartida configuracion,
            Instant ahora) {
        if (cuentas.bloquearPorId(masterId).isEmpty()) {
            throw new ErrorInscripcion(HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO",
                    "La cuenta no está disponible");
        }
        validar(configuracion, ahora);
        Partida partida = new Partida(masterId, configuracion, nuevoCodigo(), ahora);
        partidas.saveAndFlush(partida);
        jdbc.update(
                INSERTAR_PARTIDA_CREADA,
                UUID.randomUUID(),
                partida.getId(),
                partida.getNombre(),
                partida.getInicioEn().toString(),
                partida.getFinEn().toString(),
                partida.getTipoMisiones(),
                partida.getPlazoDisputaHoras().intValue(),
                partida.isEntradasTardias(),
                partida.isMasterJuega(),
                OffsetDateTime.ofInstant(ahora, ZoneOffset.UTC));
        if (partida.isMasterJuega()) {
            servicioInscripcion.alta(partida.getId(), masterId, ahora);
        }
        return vistaMaster(partida);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> misPartidas(UUID cuentaId) {
        return partidas.visiblesPara(cuentaId).stream()
                .map(partida -> vistaResumen(partida, cuentaId))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtener(UUID cuentaId, UUID partidaId) {
        Partida partida = partidas.findById(partidaId)
                .orElseThrow(PartidaNoEncontradaException::new);
        if (!cuentaId.equals(partida.getMasterId())) {
            if (inscripciones.existe(partidaId, cuentaId)) {
                return vistaResumen(partida, cuentaId);
            }
            throw new PartidaNoEncontradaException();
        }
        return vistaMaster(partida);
    }

    @Transactional
    public Map<String, Object> editar(UUID cuentaId, UUID partidaId, CambiosPartida cambios,
            Instant ahora) {
        // Cuenta y después partida, el mismo orden que la inscripción (inscripcion.md).
        if (cuentas.bloquearPorId(cuentaId).isEmpty()) {
            throw new ErrorInscripcion(HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO",
                    "La cuenta no está disponible");
        }
        Partida partida = partidas.bloquearPorId(partidaId)
                .orElseThrow(PartidaNoEncontradaException::new);
        if (!cuentaId.equals(partida.getMasterId())) {
            throw new NoEsMasterException();
        }
        if (!"EN_INSCRIPCION".equals(partida.getEstado())) {
            throw new PartidaNoEditableException();
        }
        CambiosPartida reales = soloLoQueCambia(partida, cambios);
        validar(partida, cambios, reales, ahora);
        if (reales.vacio()) {
            return vistaMaster(partida);
        }
        partida.editar(reales, ahora);
        partidas.saveAndFlush(partida);
        jdbc.update(
                INSERTAR_CONFIGURACION_CAMBIADA,
                UUID.randomUUID(),
                partida.getId(),
                reales.nombre(),
                reales.inicioEn() == null ? null : reales.inicioEn().toString(),
                reales.finEn() == null ? null : reales.finEn().toString(),
                reales.tipoMisiones(),
                reales.plazoDisputaHoras(),
                reales.entradasTardias(),
                reales.masterJuega(),
                OffsetDateTime.ofInstant(ahora, ZoneOffset.UTC),
                partida.getId());
        if (Boolean.TRUE.equals(reales.masterJuega())
                && !inscripciones.existe(partida.getId(), cuentaId)) {
            servicioInscripcion.alta(partida.getId(), cuentaId, ahora);
        } else if (Boolean.FALSE.equals(reales.masterJuega())) {
            servicioInscripcion.retirarMaster(partida.getId(), cuentaId, ahora);
        }
        return vistaMaster(partida);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerPorCodigo(String codigo) {
        Partida partida = partidas.findByCodigoInvitacion(codigo)
                .orElseThrow(PartidaNoEncontradaException::new);
        String aliasMaster = partida.getMasterId() == null ? null
                : cuentas.findById(partida.getMasterId()).map(Cuenta::getAlias).orElse(null);
        Map<String, Object> vista = new LinkedHashMap<>();
        vista.put("nombre", partida.getNombre());
        vista.put("estado", partida.getEstado());
        vista.put("inicioEn", partida.getInicioEn().toString());
        vista.put("finEn", partida.getFinEn().toString());
        vista.put("aliasMaster", aliasMaster);
        vista.put("numInscritos", inscripciones.contar(partida.getId()));
        return vista;
    }

    @Transactional
    public void cancelarPorBorradoDeMaster(UUID masterId, Instant ahora) {
        for (Partida partida : partidas.findByMasterIdAndEstadoOrderByIdAsc(masterId,
                "EN_INSCRIPCION")) {
            partida.cancelar(ahora);
            eventos.guardar(partida.getId(), "PARTIDA_CANCELADA",
                    "{\"motivo\":\"MASTER_SIN_CUENTA\",\"numJugadores\":"
                            + inscripciones.contar(partida.getId()) + "}", ahora);
        }
    }

    private static CambiosPartida soloLoQueCambia(Partida partida, CambiosPartida cambios) {
        return new CambiosPartida(
                distinto(cambios.nombre(), partida.getNombre()),
                distinto(cambios.inicioEn(), partida.getInicioEn()),
                distinto(cambios.finEn(), partida.getFinEn()),
                distinto(cambios.tipoMisiones(), partida.getTipoMisiones()),
                distinto(cambios.plazoDisputaHoras(), partida.getPlazoDisputaHoras().intValue()),
                distinto(cambios.entradasTardias(), partida.isEntradasTardias()),
                distinto(cambios.masterJuega(), partida.isMasterJuega()));
    }

    private static <T> T distinto(T nuevo, T actual) {
        return nuevo == null || nuevo.equals(actual) ? null : nuevo;
    }

    private static void validar(Partida partida, CambiosPartida enviados, CambiosPartida reales,
            Instant ahora) {
        if (enviados.vacio()) {
            throw new DatosNoValidosException(Map.of("cuerpo", "Debe cambiar al menos un campo"));
        }
        Map<String, String> errores = new LinkedHashMap<>();
        if (enviados.nombre() != null
                && (enviados.nombre().length() < 3 || enviados.nombre().length() > 40)) {
            errores.put("nombre", "Debe tener entre 3 y 40 caracteres");
        }
        if (reales.inicioEn() != null
                && reales.inicioEn().isBefore(ahora.plus(ANTELACION_MINIMA))) {
            errores.put("inicioEn", "Debe ser al menos 15 minutos en el futuro");
        }
        Instant inicio = enviados.inicioEn() != null ? enviados.inicioEn() : partida.getInicioEn();
        Instant fin = enviados.finEn() != null ? enviados.finEn() : partida.getFinEn();
        if (!fin.isAfter(inicio)) {
            errores.put("finEn", "Debe ser posterior al inicio");
        }
        if (enviados.tipoMisiones() != null && !TIPOS_MISIONES.contains(enviados.tipoMisiones())) {
            errores.put("tipoMisiones", "Debe ser BIBLIOTECA o IA");
        }
        if (enviados.plazoDisputaHoras() != null
                && !PLAZOS_DISPUTA.contains(enviados.plazoDisputaHoras())) {
            errores.put("plazoDisputaHoras", "Debe ser 2, 6, 12 o 24");
        }
        if (!errores.isEmpty()) {
            throw new DatosNoValidosException(errores);
        }
    }

    private static void validar(ConfiguracionPartida configuracion, Instant ahora) {
        Map<String, String> errores = new LinkedHashMap<>();
        String nombre = configuracion.nombre();
        if (nombre == null || nombre.length() < 3 || nombre.length() > 40) {
            errores.put("nombre", "Debe tener entre 3 y 40 caracteres");
        }
        Instant inicio = configuracion.inicioEn();
        if (inicio == null || inicio.isBefore(ahora.plus(ANTELACION_MINIMA))) {
            errores.put("inicioEn", "Debe ser al menos 15 minutos en el futuro");
        }
        Instant fin = configuracion.finEn();
        if (fin == null || (inicio != null && !fin.isAfter(inicio))) {
            errores.put("finEn", "Debe ser posterior al inicio");
        }
        if (configuracion.tipoMisiones() == null
                || !TIPOS_MISIONES.contains(configuracion.tipoMisiones())) {
            errores.put("tipoMisiones", "Debe ser BIBLIOTECA o IA");
        }
        if (configuracion.plazoDisputaHoras() == null
                || !PLAZOS_DISPUTA.contains(configuracion.plazoDisputaHoras())) {
            errores.put("plazoDisputaHoras", "Debe ser 2, 6, 12 o 24");
        }
        if (configuracion.entradasTardias() == null) {
            errores.put("entradasTardias", "Es obligatorio");
        }
        if (configuracion.masterJuega() == null) {
            errores.put("masterJuega", "Es obligatorio");
        }
        if (!errores.isEmpty()) {
            throw new DatosNoValidosException(errores);
        }
    }

    private String nuevoCodigo() {
        String codigo;
        do {
            StringBuilder texto = new StringBuilder(LONGITUD_CODIGO);
            for (int i = 0; i < LONGITUD_CODIGO; i++) {
                texto.append(ALFABETO_CODIGO.charAt(aleatorio.nextInt(ALFABETO_CODIGO.length())));
            }
            codigo = texto.toString();
        } while (partidas.existsByCodigoInvitacion(codigo));
        return codigo;
    }

    public Map<String, Object> vistaResumen(Partida partida, UUID cuentaId) {
        Map<String, Object> vista = new LinkedHashMap<>();
        vista.put("id", partida.getId().toString());
        vista.put("nombre", partida.getNombre());
        vista.put("estado", partida.getEstado());
        vista.put("inicioEn", partida.getInicioEn().toString());
        vista.put("finEn", partida.getFinEn().toString());
        vista.put("soyMaster", cuentaId.equals(partida.getMasterId()));
        vista.put("numInscritos", inscripciones.contar(partida.getId()));
        vista.put("motivoCancelacion", motivoCancelacion(partida));
        return vista;
    }

    private String motivoCancelacion(Partida partida) {
        if (!"CANCELADA".equals(partida.getEstado())) {
            return null;
        }
        return jdbc.queryForList("select datos->>'motivo' from evento where partida_id = ?"
                + " and tipo = 'PARTIDA_CANCELADA' order by secuencia desc limit 1",
                String.class, partida.getId()).stream().findFirst().orElse(null);
    }

    private Map<String, Object> vistaMaster(Partida partida) {
        Map<String, Object> vista = new LinkedHashMap<>();
        vista.put("id", partida.getId().toString());
        vista.put("nombre", partida.getNombre());
        vista.put("inicioEn", partida.getInicioEn().toString());
        vista.put("finEn", partida.getFinEn().toString());
        vista.put("tipoMisiones", partida.getTipoMisiones());
        vista.put("plazoDisputaHoras", partida.getPlazoDisputaHoras().intValue());
        vista.put("entradasTardias", partida.isEntradasTardias());
        vista.put("masterJuega", partida.isMasterJuega());
        vista.put("estado", partida.getEstado());
        vista.put("codigoInvitacion", partida.getCodigoInvitacion());
        vista.put("enlace", "ghostmark://unirse/" + partida.getCodigoInvitacion());
        vista.put("numInscritos", inscripciones.contar(partida.getId()));
        vista.put("creadaEn", partida.getCreadaEn().toString());
        return vista;
    }
}
