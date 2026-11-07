package es.unizar.ghostmark.backend.mision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class GeneradorSimuladoTest {

    private final GeneradorSimulado generador = new GeneradorSimulado();

    @Test
    void normalDevuelveLasPedidasSinRepetirNiLasQueHayQueEvitar() {
        List<String> evitar = List.of("QUE TE DIGA CUÁL ES SU RINCÓN FAVORITO DEL CAMPUS");
        List<MisionGenerada> misiones = generador.generar(20, "Campus", evitar);
        assertEquals(20, misiones.size());
        Set<String> textos = misiones.stream().map(m -> m.texto().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        assertEquals(20, textos.size());
        assertFalse(textos.contains(evitar.get(0).toLowerCase(Locale.ROOT)));
    }

    @Test
    void formatoInvalidoYProhibidasDevuelvenLoQueElFiltroDebeQuitar() {
        generador.usar(GeneradorSimulado.Modo.FORMATO_INVALIDO);
        assertTrue(generador.generar(12, "Campus", List.of()).stream()
                .allMatch(m -> m.texto() == null || m.texto().length() < 10
                        || m.texto().contains("\n")));
        generador.usar(GeneradorSimulado.Modo.PROHIBIDAS);
        List<MisionGenerada> prohibidas = generador.generar(12, "Campus", List.of());
        assertEquals(12, prohibidas.size());
        assertTrue(prohibidas.stream().anyMatch(m -> "CONTACTO_FISICO".equals(m.categoria())));
        assertTrue(prohibidas.stream().anyMatch(m -> m.texto().contains("cerveza")));
    }

    @ParameterizedTest
    @EnumSource(value = GeneradorSimulado.Modo.class, names = {"CAIDO", "LENTO"})
    void caidoYLentoNoEstanDisponibles(GeneradorSimulado.Modo modo) {
        generador.usar(modo);
        assertThrows(GeneradorNoDisponible.class,
                () -> generador.generar(12, "Campus", List.of()));
    }
}
