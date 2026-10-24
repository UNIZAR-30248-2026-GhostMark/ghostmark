package es.unizar.ghostmark.backend.partida;

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
    private static final String INSERTAR_PARTIDA_CANCELADA = "insert into evento"
            + " (id, partida_id, secuencia, tipo, datos, ocurrido_en)"
            + " select ?, ?, coalesce(max(secuencia), 0) + 1, 'PARTIDA_CANCELADA',"
            + " jsonb_build_object('motivo', 'MASTER_SIN_CUENTA', 'numJugadores', ?::int), ?"
            + " from evento where partida_id = ?";

    private final PartidaRepository partidas;
    private final JdbcTemplate jdbc;
    private final SecureRandom aleatorio = new SecureRandom();

    public ServicioPartida(PartidaRepository partidas, JdbcTemplate jdbc) {
        this.partidas = partidas;
        this.jdbc = jdbc;
    }

    @Transactional
    public Map<String, Object> crear(UUID masterId, ConfiguracionPartida configuracion,
            Instant ahora) {
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
        return vistaMaster(partida);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> misPartidas(UUID masterId) {
        return partidas.findByMasterId(masterId).stream()
                .map(ServicioPartida::vistaResumen)
                .toList();
    }

    @Transactional
    public void cancelarPorBorradoDeMaster(UUID masterId, Instant ahora) {
        for (Partida partida : partidas.findByMasterIdAndEstadoOrderByIdAsc(masterId,
                "EN_INSCRIPCION")) {
            partida.cancelar(ahora);
            jdbc.update(INSERTAR_PARTIDA_CANCELADA, UUID.randomUUID(), partida.getId(),
                    0, OffsetDateTime.ofInstant(ahora, ZoneOffset.UTC), partida.getId());
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

    private static Map<String, Object> vistaResumen(Partida partida) {
        Map<String, Object> vista = new LinkedHashMap<>();
        vista.put("id", partida.getId().toString());
        vista.put("nombre", partida.getNombre());
        vista.put("estado", partida.getEstado());
        vista.put("inicioEn", partida.getInicioEn().toString());
        vista.put("finEn", partida.getFinEn().toString());
        vista.put("soyMaster", true);
        vista.put("numInscritos", 0);
        return vista;
    }

    private static Map<String, Object> vistaMaster(Partida partida) {
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
        vista.put("numInscritos", 0);
        vista.put("creadaEn", partida.getCreadaEn().toString());
        return vista;
    }
}
