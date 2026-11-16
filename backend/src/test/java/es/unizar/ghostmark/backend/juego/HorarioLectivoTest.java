package es.unizar.ghostmark.backend.juego;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class HorarioLectivoTest {

    private static HorarioLectivo.Franja franja(int dia, String inicio) {
        return new HorarioLectivo.Franja(dia, LocalTime.parse(inicio));
    }

    private static Instant en(String madrid) {
        return LocalDateTime.parse(madrid).atZone(HorarioLectivo.ZONA).toInstant();
    }

    @Test
    void estaEnClaseDesdeQueEmpiezaLaFranjaHastaJustoAntesDeQueAcabe() {
        Set<HorarioLectivo.Franja> martes = Set.of(franja(2, "10:00"));
        assertTrue(HorarioLectivo.enClase(martes, en("2026-11-17T10:00:00")));
        assertTrue(HorarioLectivo.enClase(martes, en("2026-11-17T10:29:59")));
        assertFalse(HorarioLectivo.enClase(martes, en("2026-11-17T10:30:00")));
        assertFalse(HorarioLectivo.enClase(martes, en("2026-11-17T09:59:59")));
        assertFalse(HorarioLectivo.enClase(martes, en("2026-11-18T10:10:00")));
    }

    @Test
    void elFinDeSemanaYSinFranjasNadieEstaEnClase() {
        Set<HorarioLectivo.Franja> todos = IntStream.rangeClosed(1, 5)
                .mapToObj(dia -> franja(dia, "10:00")).collect(Collectors.toSet());
        assertFalse(HorarioLectivo.enClase(todos, en("2026-11-21T10:10:00")));
        assertFalse(HorarioLectivo.enClase(todos, en("2026-11-22T10:10:00")));
        assertFalse(HorarioLectivo.enClase(Set.of(), en("2026-11-17T10:10:00")));
    }

    @Test
    void sinNadieEnClaseEstaDisponible() {
        assertEquals(Optional.empty(), HorarioLectivo.calcular(Set.of(franja(2, "12:00")),
                Set.of(), en("2026-11-17T10:10:00")));
    }

    @Test
    void lasFranjasSeguidasDelObjetivoFormanUnSoloTramo() {
        Set<HorarioLectivo.Franja> objetivo = Set.of(franja(2, "10:00"), franja(2, "10:30"),
                franja(2, "11:00"));
        assertEquals(Optional.of(new NoDisponible("OBJETIVO", en("2026-11-17T11:30:00"))),
                HorarioLectivo.calcular(Set.of(), objetivo, en("2026-11-17T10:10:00")));
    }

    @Test
    void siLosDosEstanEnClaseElMotivoEsElCazadorYSeEsperaAQueAcabenLosDos() {
        assertEquals(Optional.of(new NoDisponible("CAZADOR", en("2026-11-17T11:00:00"))),
                HorarioLectivo.calcular(Set.of(franja(2, "10:00")),
                        Set.of(franja(2, "10:00"), franja(2, "10:30")),
                        en("2026-11-17T10:10:00")));
    }

    @Test
    void losTramosDeLosDosSeEncadenan() {
        assertEquals(Optional.of(new NoDisponible("CAZADOR", en("2026-11-17T11:00:00"))),
                HorarioLectivo.calcular(Set.of(franja(2, "10:00")), Set.of(franja(2, "10:30")),
                        en("2026-11-17T10:10:00")));
    }

    @Test
    void laUltimaFranjaDelViernesAcabaALasNueve() {
        assertEquals(Optional.of(new NoDisponible("OBJETIVO", en("2026-11-20T21:00:00"))),
                HorarioLectivo.calcular(Set.of(), Set.of(franja(5, "20:30")),
                        en("2026-11-20T20:40:00")));
    }

    @Test
    void conElCambioDeHoraLaFranjaSigueEnLaHoraDeMadrid() {
        Set<HorarioLectivo.Franja> lunes = Set.of(franja(1, "09:00"));
        assertTrue(HorarioLectivo.enClase(lunes, Instant.parse("2026-10-19T07:10:00Z")));
        assertFalse(HorarioLectivo.enClase(lunes, Instant.parse("2026-10-26T07:10:00Z")));
        assertTrue(HorarioLectivo.enClase(lunes, Instant.parse("2026-10-26T08:10:00Z")));
    }

    @Test
    void elSiguienteTramoEsElPrimerEnPuntoOYMediaPosterior() {
        assertEquals(Instant.parse("2026-11-17T09:30:00Z"),
                HorarioLectivo.siguienteTramo(Instant.parse("2026-11-17T09:10:30Z")));
        assertEquals(Instant.parse("2026-11-17T10:00:00Z"),
                HorarioLectivo.siguienteTramo(Instant.parse("2026-11-17T09:30:00Z")));
    }
}
