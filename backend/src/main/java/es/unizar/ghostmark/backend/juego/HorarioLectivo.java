package es.unizar.ghostmark.backend.juego;

import es.unizar.ghostmark.backend.inscripcion.InscripcionRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class HorarioLectivo {

    static final ZoneId ZONA = ZoneId.of("Europe/Madrid");
    static final Duration TRAMO = Duration.ofMinutes(30);
    private static final int MAXIMO_TRAMOS = 7 * 48;
    private static final int ULTIMO_DIA_LECTIVO = 5;

    private final InscripcionRepository inscripciones;

    public HorarioLectivo(InscripcionRepository inscripciones) {
        this.inscripciones = inscripciones;
    }

    public Optional<NoDisponible> noDisponible(UUID cazadorId, UUID objetivoId, Instant ahora) {
        return calcular(franjas(cazadorId), franjas(objetivoId), ahora);
    }

    static Optional<NoDisponible> calcular(Set<Franja> cazador, Set<Franja> objetivo,
            Instant ahora) {
        boolean cazadorEnClase = enClase(cazador, ahora);
        if (!cazadorEnClase && !enClase(objetivo, ahora)) {
            return Optional.empty();
        }
        Instant disponible = siguienteTramo(ahora);
        for (int tramo = 0; tramo < MAXIMO_TRAMOS
                && (enClase(cazador, disponible) || enClase(objetivo, disponible)); tramo++) {
            disponible = disponible.plus(TRAMO);
        }
        return Optional.of(new NoDisponible(cazadorEnClase ? "CAZADOR" : "OBJETIVO",
                disponible));
    }

    static boolean enClase(Set<Franja> franjas, Instant instante) {
        ZonedDateTime local = instante.atZone(ZONA);
        int dia = local.getDayOfWeek().getValue();
        if (dia > ULTIMO_DIA_LECTIVO) {
            return false;
        }
        LocalTime hora = local.toLocalTime();
        return franjas.stream().anyMatch(franja -> franja.dia() == dia
                && !hora.isBefore(franja.inicio())
                && hora.isBefore(franja.inicio().plus(TRAMO)));
    }

    static Instant siguienteTramo(Instant ahora) {
        long segundos = TRAMO.getSeconds();
        return Instant.ofEpochSecond((Math.floorDiv(ahora.getEpochSecond(), segundos) + 1)
                * segundos);
    }

    private Set<Franja> franjas(UUID inscripcionId) {
        return inscripciones.horario(inscripcionId).franjas().stream()
                .map(franja -> new Franja(franja.dia(), LocalTime.parse(franja.horaInicio())))
                .collect(Collectors.toSet());
    }

    record Franja(int dia, LocalTime inicio) {
    }
}
