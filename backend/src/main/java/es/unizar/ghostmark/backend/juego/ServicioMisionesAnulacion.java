package es.unizar.ghostmark.backend.juego;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Sustituye misiones durante una anulacion, sin reconstruir el ciclo (11b.1). */
@Service
public class ServicioMisionesAnulacion {

    private final JdbcTemplate jdbc;
    private final CartaRepository cartas;
    private final SecureRandom aleatorio = new SecureRandom();

    public ServicioMisionesAnulacion(JdbcTemplate jdbc, CartaRepository cartas) {
        this.jdbc = jdbc;
        this.cartas = cartas;
    }

    /** Resultado para el campo "misiones" del evento ELIMINACION_ANULADA. */
    public record Asignacion(UUID inscripcionId, UUID misionId) {
    }

    private record Jugador(UUID inscripcionId, UUID misionId) {
    }

    /**
     * Se llama desde la transaccion de anulacion, despues de revivir a los afectados.
     * El llamador mantiene bloqueada la partida y guarda el evento ELIMINACION_ANULADA.
     * No modifica el ciclo ni crea eventos: ambas cosas pertenecen a 11b.1.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<Asignacion> sustituir(UUID partidaId, UUID cazadorId, List<UUID> revividos) {
        Objects.requireNonNull(partidaId, "partidaId");
        Objects.requireNonNull(cazadorId, "cazadorId");
        Objects.requireNonNull(revividos, "revividos");
        Set<UUID> afectados = new LinkedHashSet<>();
        afectados.add(cazadorId);
        for (UUID revivido : revividos) {
            afectados.add(Objects.requireNonNull(revivido, "revivido"));
        }
        if (afectados.size() != revividos.size() + 1) {
            throw new IllegalArgumentException("Los afectados no pueden repetirse");
        }

        // El ciclo ya se ha restablecido. Bloquear los vivos estabiliza las misiones actuales.
        List<Jugador> vivos = jdbc.query("""
                select inscripcion_id, mision_id from jugador_partida
                where partida_id = ? and estado = 'VIVO'
                order by inscripcion_id for update
                """, (fila, numero) -> new Jugador(
                        fila.getObject("inscripcion_id", UUID.class),
                        fila.getObject("mision_id", UUID.class)), partidaId);
        if (!vivos.stream().map(Jugador::inscripcionId).collect(
                java.util.stream.Collectors.toSet()).containsAll(afectados)) {
            throw new IllegalStateException("Hay afectados que no estan vivos en la partida");
        }

        List<UUID> biblioteca = new ArrayList<>(cartas.misionesActivas());
        if (biblioteca.isEmpty()) {
            throw new IllegalStateException("No hay misiones activas en la biblioteca");
        }

        // Primero se reservan las misiones de los vivos no implicados.
        Set<UUID> ocupadas = new LinkedHashSet<>();
        for (Jugador vivo : vivos) {
            if (!afectados.contains(vivo.inscripcionId()) && vivo.misionId() != null) {
                ocupadas.add(vivo.misionId());
            }
        }
        List<Asignacion> nuevas = new ArrayList<>();
        for (UUID afectado : afectados) {
            UUID anterior = vivos.stream().filter(j -> j.inscripcionId().equals(afectado))
                    .findFirst().orElseThrow().misionId();
            List<UUID> sinRepetir = biblioteca.stream()
                    .filter(id -> !ocupadas.contains(id) && !id.equals(anterior)).toList();
            List<UUID> alternativas = biblioteca.stream()
                    .filter(id -> !id.equals(anterior)).toList();
            // Se repite una mision ocupada solo cuando no queda otra libre distinta.
            List<UUID> candidatas = sinRepetir.isEmpty() ? alternativas : sinRepetir;
            if (candidatas.isEmpty()) {
                candidatas = biblioteca;
            }
            UUID elegida = candidatas.get(aleatorio.nextInt(candidatas.size()));
            int actualizadas = jdbc.update("update jugador_partida set mision_id = ?"
                    + " where partida_id = ? and inscripcion_id = ? and estado = 'VIVO'",
                    elegida, partidaId, afectado);
            if (actualizadas != 1) {
                throw new IllegalStateException("No se pudo cambiar la mision de un afectado");
            }
            ocupadas.add(elegida);
            nuevas.add(new Asignacion(afectado, elegida));
        }
        return List.copyOf(nuevas);
    }
}
