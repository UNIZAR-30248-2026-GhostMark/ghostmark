package es.unizar.ghostmark.backend.zonas;

import es.unizar.ghostmark.backend.cuenta.CuentaRepository;
import es.unizar.ghostmark.backend.evento.ServicioEvento;
import es.unizar.ghostmark.backend.inscripcion.ErrorInscripcion;
import es.unizar.ghostmark.backend.inscripcion.InscripcionRepository;
import es.unizar.ghostmark.backend.partida.NoEsMasterException;
import es.unizar.ghostmark.backend.partida.Partida;
import es.unizar.ghostmark.backend.partida.PartidaNoEditableException;
import es.unizar.ghostmark.backend.partida.PartidaNoEncontradaException;
import es.unizar.ghostmark.backend.partida.PartidaRepository;
import es.unizar.ghostmark.backend.sesion.DatosNoValidosException;
import es.unizar.ghostmark.backend.zonas.Zonas.Punto;
import es.unizar.ghostmark.backend.zonas.Zonas.Zona;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
public class ServicioZonas {

    private static final int MAXIMO_ZONAS = 10;
    private static final int MAXIMO_PUNTOS = 20;

    private final PartidaRepository partidas;
    private final CuentaRepository cuentas;
    private final InscripcionRepository inscripciones;
    private final JdbcTemplate jdbc;
    private final ServicioEvento eventos;

    public ServicioZonas(PartidaRepository partidas, CuentaRepository cuentas,
            InscripcionRepository inscripciones, JdbcTemplate jdbc, ServicioEvento eventos) {
        this.partidas = partidas;
        this.cuentas = cuentas;
        this.inscripciones = inscripciones;
        this.jdbc = jdbc;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    public Zonas listar(UUID cuentaId, UUID partidaId) {
        Partida partida = partidas.findById(partidaId)
                .orElseThrow(PartidaNoEncontradaException::new);
        if (!cuentaId.equals(partida.getMasterId())
                && !inscripciones.existe(partidaId, cuentaId)) {
            throw new PartidaNoEncontradaException();
        }
        return new Zonas(cargar(partidaId).stream().map(ZonaGuardada::zona).toList());
    }

    @Transactional
    public Zonas guardar(UUID cuentaId, UUID partidaId, Zonas peticion, Instant ahora) {
        // Orden de bloqueo habitual del proyecto: cuenta, partida y después evento.
        if (cuentas.bloquearPorId(cuentaId).isEmpty()) {
            throw new ErrorInscripcion(HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO",
                    "La cuenta no está disponible");
        }
        Partida partida = partidas.bloquearPorId(partidaId)
                .orElseThrow(PartidaNoEncontradaException::new);
        if (!cuentaId.equals(partida.getMasterId())) {
            if (inscripciones.existe(partidaId, cuentaId)) {
                throw new NoEsMasterException();
            }
            throw new PartidaNoEncontradaException();
        }
        if (!"EN_INSCRIPCION".equals(partida.getEstado())) {
            throw new PartidaNoEditableException();
        }

        List<ZonaGuardada> anteriores = cargar(partidaId);
        List<ZonaParaGuardar> nuevas = validar(peticion, anteriores, ahora);

        // Primero se elimina todo: renombrar dos zonas intercambiando nombres es válido.
        jdbc.update("delete from zona_segura where partida_id = ?", partidaId);
        for (ZonaParaGuardar zona : nuevas) {
            jdbc.update("insert into zona_segura"
                            + " (id, partida_id, nombre, poligono, creada_en)"
                            + " values (?, ?, ?, ST_GeomFromText(?, 4326), ?)",
                    zona.id(), partidaId, zona.nombre(), zona.poligono(),
                    OffsetDateTime.ofInstant(zona.creadaEn(), ZoneOffset.UTC));
        }

        Zonas resultado = new Zonas(cargar(partidaId).stream()
                .map(ZonaGuardada::zona).toList());
        eventos.guardar(partidaId, "ZONAS_CAMBIADAS",
                JsonMapper.shared().writeValueAsString(resultado), ahora);
        return resultado;
    }

    private List<ZonaParaGuardar> validar(Zonas peticion, List<ZonaGuardada> anteriores,
            Instant ahora) {
        if (peticion == null || peticion.zonas() == null) {
            throw new DatosNoValidosException(Map.of("zonas", "La lista es obligatoria"));
        }
        List<Zona> zonas = peticion.zonas();
        Map<String, String> errores = new LinkedHashMap<>();
        if (zonas.size() > MAXIMO_ZONAS) {
            errores.put("zonas", "Como máximo se permiten 10 zonas");
        }
        Map<UUID, Instant> existentes = new HashMap<>();
        for (ZonaGuardada zona : anteriores) {
            existentes.put(zona.id(), zona.creadaEn());
        }
        Set<UUID> ids = new HashSet<>();
        Set<String> nombres = new HashSet<>();
        List<ZonaParaGuardar> nuevas = new ArrayList<>();
        for (int i = 0; i < zonas.size(); i++) {
            String campo = "zonas[" + i + "]";
            Zona zona = zonas.get(i);
            if (zona == null) {
                errores.put(campo, "La zona es obligatoria");
                continue;
            }
            UUID id = zona.id();
            if (id != null && (!existentes.containsKey(id) || !ids.add(id))) {
                errores.put(campo + ".id", "Id inexistente en la partida o repetido");
            }
            String nombre = zona.nombre() == null ? "" : zona.nombre().strip();
            if (nombre.isEmpty() || nombre.length() > 30) {
                errores.put(campo + ".nombre", "Debe tener entre 1 y 30 caracteres");
            } else if (!nombres.add(nombre.toLowerCase(Locale.ROOT))) {
                errores.put(campo + ".nombre", "Nombre repetido en la partida");
            }
            String poligono = validarPuntos(zona.puntos(), campo, errores);
            if (poligono != null && !Boolean.TRUE.equals(jdbc.queryForObject(
                    "select ST_IsValid(ST_GeomFromText(?, 4326))",
                    Boolean.class, poligono))) {
                errores.put(campo + ".puntos", "El polígono se cruza o no es válido");
            }
            nuevas.add(new ZonaParaGuardar(id == null ? UUID.randomUUID() : id,
                    nombre, poligono, id == null ? ahora : existentes.get(id)));
        }
        if (!errores.isEmpty()) {
            throw new DatosNoValidosException(errores);
        }
        return nuevas;
    }

    private static String validarPuntos(List<Punto> puntos, String campo,
            Map<String, String> errores) {
        if (puntos == null || puntos.size() < 3 || puntos.size() > MAXIMO_PUNTOS) {
            errores.put(campo + ".puntos", "Se necesitan entre 3 y 20 puntos");
            return null;
        }
        Set<Punto> vistos = new HashSet<>();
        StringBuilder poligono = new StringBuilder("POLYGON((");
        String primero = null;
        for (Punto punto : puntos) {
            if (punto == null || punto.lat() == null || punto.lon() == null
                    || !Double.isFinite(punto.lat()) || !Double.isFinite(punto.lon())
                    || punto.lat() < -90 || punto.lat() > 90
                    || punto.lon() < -180 || punto.lon() > 180) {
                errores.put(campo + ".puntos", "Hay coordenadas no válidas");
                return null;
            }
            // -0.0 y 0.0 representan el mismo punto geográfico.
            Punto normalizado = new Punto(punto.lat() == 0 ? 0.0 : punto.lat(),
                    punto.lon() == 0 ? 0.0 : punto.lon());
            if (!vistos.add(normalizado)) {
                errores.put(campo + ".puntos", "No pueden repetirse los puntos");
                return null;
            }
            String coordenada = normalizado.lon() + " " + normalizado.lat();
            if (primero == null) {
                primero = coordenada;
            } else {
                poligono.append(", ");
            }
            poligono.append(coordenada);
        }
        return poligono.append(", ").append(primero).append("))").toString();
    }

    private List<ZonaGuardada> cargar(UUID partidaId) {
        return jdbc.query("select id, nombre, creada_en, ST_AsGeoJSON(poligono, 15) as geojson"
                        + " from zona_segura where partida_id = ? order by nombre, id",
                (fila, numero) -> new ZonaGuardada(
                        fila.getObject("id", UUID.class), fila.getString("nombre"),
                        leerPuntos(fila.getString("geojson")),
                        fila.getTimestamp("creada_en").toInstant()), partidaId);
    }

    private static List<Punto> leerPuntos(String geojson) {
        JsonNode anillo = JsonMapper.shared().readTree(geojson).path("coordinates").path(0);
        List<Punto> puntos = new ArrayList<>();
        // PostGIS cierra el anillo: el contrato devuelve los vértices sin repetir el primero.
        for (int i = 0; i < anillo.size() - 1; i++) {
            JsonNode par = anillo.path(i);
            puntos.add(new Punto(par.path(1).doubleValue(), par.path(0).doubleValue()));
        }
        return puntos;
    }

    private record ZonaGuardada(UUID id, String nombre, List<Punto> puntos, Instant creadaEn) {
        Zona zona() {
            return new Zona(id, nombre, puntos);
        }
    }

    private record ZonaParaGuardar(UUID id, String nombre, String poligono, Instant creadaEn) {
    }
}
