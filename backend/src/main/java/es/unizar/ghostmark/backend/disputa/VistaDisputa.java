package es.unizar.ghostmark.backend.disputa;

import es.unizar.ghostmark.backend.partida.Partida;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class VistaDisputa {

    private final JdbcTemplate jdbc;

    public VistaDisputa(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean esArbitro(UUID cuentaId, Partida partida, DisputaRepository.Fila disputa) {
        if (disputa.arbitroId() == null) {
            return cuentaId.equals(partida.getMasterId());
        }
        return Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from inscripcion"
                + " where id = ? and cuenta_id = ?)", Boolean.class, disputa.arbitroId(),
                cuentaId));
    }

    public UUID inscripcion(UUID cuentaId, DisputaRepository.Fila disputa) {
        return jdbc.queryForList("select id from inscripcion where cuenta_id = ? and id in (?, ?)",
                UUID.class, cuentaId, disputa.cazadorId(), disputa.victimaId()).stream()
                .findFirst().orElse(null);
    }

    public Map<String, Object> de(UUID cuentaId, Partida partida,
            DisputaRepository.Fila disputa) {
        final boolean arbitro = esArbitro(cuentaId, partida, disputa);
        final UUID implicado = inscripcion(cuentaId, disputa);
        final UUID responde = "ELIMINACION".equals(disputa.tipo()) ? disputa.cazadorId()
                : disputa.victimaId();
        List<Map<String, Object>> datos = jdbc.queryForList("select cc.alias as cazador,"
                + " cv.alias as victima, m.texto as mision,"
                + " e.datos->>'resultadoZona' as zona from disputa d"
                + " join inscripcion ic on ic.id = d.cazador_id"
                + " join inscripcion iv on iv.id = d.victima_id"
                + " left join cuenta cc on cc.id = ic.cuenta_id"
                + " left join cuenta cv on cv.id = iv.cuenta_id"
                + " left join evento e on e.id = d.evento_eliminacion_id"
                + " left join jugador_partida jp on jp.inscripcion_id = d.cazador_id"
                + " left join mision m on m.id = case when d.tipo = 'ELIMINACION'"
                + " then (e.datos->>'misionId')::uuid else jp.mision_id end"
                + " where d.id = ?", disputa.id());
        Map<String, Object> nombres = datos.getFirst();
        Map<String, Object> vista = new LinkedHashMap<>();
        vista.put("id", disputa.id());
        vista.put("tipo", disputa.tipo());
        vista.put("estado", disputa.estado());
        vista.put("aliasCazador", nombres.get("cazador"));
        vista.put("aliasVictima", nombres.get("victima"));
        vista.put("mision", nombres.get("mision"));
        vista.put("textoApertura", disputa.textoApertura());
        vista.put("textoRespuesta", disputa.textoRespuesta());
        vista.put("abiertaEn", disputa.abiertaEn());
        vista.put("plazoHasta", disputa.plazoHasta());
        vista.put("soyArbitro", arbitro);
        vista.put("puedoResponder", "ABIERTA".equals(disputa.estado())
                && disputa.textoRespuesta() == null && responde.equals(implicado));
        vista.put("resultadoZona", arbitro ? nombres.get("zona") : null);
        vista.put("razon", disputa.razon());
        vista.put("resueltaPor", disputa.resueltaPor());
        vista.put("cerradaEn", disputa.cerradaEn());
        return vista;
    }
}
