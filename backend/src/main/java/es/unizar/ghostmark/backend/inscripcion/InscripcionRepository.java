package es.unizar.ghostmark.backend.inscripcion;

import es.unizar.ghostmark.backend.cuenta.dto.Horario;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class InscripcionRepository {

    private final JdbcTemplate jdbc;

    public InscripcionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean existe(UUID partidaId, UUID cuentaId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from inscripcion"
                + " where partida_id = ? and cuenta_id = ?)", Boolean.class, partidaId, cuentaId));
    }

    public Optional<UUID> idDe(UUID partidaId, UUID cuentaId) {
        return jdbc.queryForList("select id from inscripcion"
                + " where partida_id = ? and cuenta_id = ?", UUID.class, partidaId, cuentaId)
                .stream().findFirst();
    }

    public int contar(UUID partidaId) {
        return jdbc.queryForObject("select count(*) from inscripcion where partida_id = ?",
                Integer.class, partidaId);
    }

    public UUID crear(UUID partidaId, UUID cuentaId, Instant ahora) {
        UUID id = UUID.randomUUID();
        OffsetDateTime fecha = OffsetDateTime.ofInstant(ahora, ZoneOffset.UTC);
        jdbc.update("insert into inscripcion"
                        + " (id, partida_id, cuenta_id, normas_aceptadas_en, inscrita_en)"
                        + " values (?, ?, ?, ?, ?)", id, partidaId, cuentaId, fecha, fecha);
        jdbc.update("insert into franja_horario_inscripcion (inscripcion_id, dia, hora_inicio)"
                        + " select ?, dia, hora_inicio from franja_horario where cuenta_id = ?",
                id, cuentaId);
        return id;
    }

    public Horario horario(UUID id) {
        return new Horario(jdbc.query("select dia, to_char(hora_inicio, 'HH24:MI') as hora"
                        + " from franja_horario_inscripcion where inscripcion_id = ?"
                        + " order by dia, hora_inicio",
                (fila, numero) -> new Horario.Franja(fila.getInt("dia"), fila.getString("hora")),
                id));
    }

    public Optional<UUID> idDeCuentaEnPartida(UUID partidaId, UUID cuentaId) {
        return jdbc.query("select id from inscripcion where partida_id = ? and cuenta_id = ?",
                (fila, numero) -> fila.getObject("id", UUID.class), partidaId, cuentaId)
                .stream().findFirst();
    }

    public void reemplazarHorario(UUID id, Horario horario) {
        borrarHorario(id);
        for (Horario.Franja franja : horario.franjas()) {
            jdbc.update("insert into franja_horario_inscripcion"
                            + " (inscripcion_id, dia, hora_inicio) values (?, ?, ?)",
                    id, franja.dia(), java.time.LocalTime.parse(franja.horaInicio()));
        }
    }

    public List<Map<String, Object>> listar(UUID partidaId) {
        return jdbc.query("select i.id, c.alias, coalesce(i.cuenta_id = p.master_id, false)"
                        + " as es_master from inscripcion i join partida p on p.id = i.partida_id"
                        + " left join cuenta c on c.id = i.cuenta_id where i.partida_id = ?"
                        + " order by i.inscrita_en, i.id", (fila, numero) -> {
                            Map<String, Object> vista = new LinkedHashMap<>();
                            vista.put("id", fila.getObject("id", UUID.class).toString());
                            vista.put("alias", fila.getString("alias"));
                            vista.put("esMaster", fila.getBoolean("es_master"));
                            return vista;
                        }, partidaId);
    }

    public void bloquearPartidasDeCuenta(UUID cuentaId) {
        // Un orden común evita interbloqueos entre borrados que afectan a varias partidas.
        jdbc.queryForList("select p.id from partida p where p.master_id = ? or exists"
                        + " (select 1 from inscripcion i where i.partida_id = p.id"
                        + " and i.cuenta_id = ?) order by p.id for update of p",
                UUID.class, cuentaId, cuentaId);
    }

    public List<Retirada> deCuenta(UUID cuentaId) {
        return jdbc.query("select i.id, i.partida_id, p.estado from inscripcion i"
                        + " join partida p on p.id = i.partida_id where i.cuenta_id = ?"
                        + " order by p.id", (fila, numero) -> new Retirada(
                            fila.getObject("id", UUID.class),
                            fila.getObject("partida_id", UUID.class),
                            fila.getString("estado")), cuentaId);
    }

    public void borrarHorario(UUID id) {
        jdbc.update("delete from franja_horario_inscripcion where inscripcion_id = ?", id);
    }

    public void desvincular(UUID id) {
        jdbc.update("update inscripcion set cuenta_id = null where id = ?", id);
    }

    public void borrar(UUID id) {
        jdbc.update("delete from inscripcion where id = ?", id);
    }

    public List<UUID> idsDePartida(UUID partidaId) {
        return jdbc.queryForList("select id from inscripcion where partida_id = ? order by id",
                UUID.class, partidaId);
    }

    public record Retirada(UUID id, UUID partidaId, String estado) {
    }
}
