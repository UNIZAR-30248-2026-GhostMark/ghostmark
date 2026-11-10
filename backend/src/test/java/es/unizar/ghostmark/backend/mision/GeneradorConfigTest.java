package es.unizar.ghostmark.backend.mision;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class GeneradorConfigTest {

    private final GeneradorConfig configuracion = new GeneradorConfig();

    @Test
    void sinClaveLaIaNoEstaDisponibleYNoSaleElSimulado() {
        GeneradorMisiones generador = configuracion.generadorMisiones("", "", false,
                GeneradorSimulado.Modo.NORMAL);

        assertThrows(GeneradorNoDisponible.class,
                () -> generador.generar(12, "Campus", List.of()));
    }

    @Test
    void elSimuladoSoloSaleSiSePideExpresamente() {
        assertInstanceOf(GeneradorSimulado.class,
                configuracion.generadorMisiones("", "", true, GeneradorSimulado.Modo.NORMAL));
    }

    @ParameterizedTest
    @EnumSource(value = GeneradorSimulado.Modo.class, names = {"CAIDO", "LENTO"})
    void elSimuladoArrancaConElModoConfigurado(GeneradorSimulado.Modo modo) {
        GeneradorMisiones generador = configuracion.generadorMisiones("", "", true, modo);

        assertThrows(GeneradorNoDisponible.class,
                () -> generador.generar(12, "Campus", List.of()));
    }

    @Test
    void conClaveYModeloSeUsaGemini() {
        assertInstanceOf(GeneradorGemini.class,
                configuracion.generadorMisiones("clave", "modelo", false,
                        GeneradorSimulado.Modo.NORMAL));
    }

    @Test
    void conClaveYSinModeloNoArranca() {
        assertThrows(IllegalStateException.class,
                () -> configuracion.generadorMisiones("clave", "", false,
                        GeneradorSimulado.Modo.NORMAL));
    }
}
