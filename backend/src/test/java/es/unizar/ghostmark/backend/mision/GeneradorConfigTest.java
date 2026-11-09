package es.unizar.ghostmark.backend.mision;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class GeneradorConfigTest {

    private final GeneradorConfig configuracion = new GeneradorConfig();

    @Test
    void sinClaveLaIaNoEstaDisponibleYNoSaleElSimulado() {
        GeneradorMisiones generador = configuracion.generadorMisiones("", "", false);

        assertThrows(GeneradorNoDisponible.class,
                () -> generador.generar(12, "Campus", List.of()));
    }

    @Test
    void elSimuladoSoloSaleSiSePideExpresamente() {
        assertInstanceOf(GeneradorSimulado.class,
                configuracion.generadorMisiones("", "", true));
    }

    @Test
    void conClaveYModeloSeUsaGemini() {
        assertInstanceOf(GeneradorGemini.class,
                configuracion.generadorMisiones("clave", "modelo", false));
    }

    @Test
    void conClaveYSinModeloNoArranca() {
        assertThrows(IllegalStateException.class,
                () -> configuracion.generadorMisiones("clave", "", false));
    }
}
