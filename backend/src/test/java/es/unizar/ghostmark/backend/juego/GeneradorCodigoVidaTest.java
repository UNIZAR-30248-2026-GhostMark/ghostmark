package es.unizar.ghostmark.backend.juego;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GeneradorCodigoVidaTest {

    private final GeneradorCodigoVida generador = new GeneradorCodigoVida();

    @Test
    void daSeisDigitosQueCaducanSesentaSegundosDespuesDePedirlos() {
        Instant ahora = Instant.parse("2026-11-23T10:00:59Z");
        CodigoVida codigo = generador.generar(ahora);
        assertTrue(codigo.codigo().matches("[0-9]{6}"));
        assertEquals(ahora.plusSeconds(60), codigo.caducaEn());
    }

    @Test
    void cadaPeticionDaUnCodigoAlAzar() {
        Instant ahora = Instant.parse("2026-11-23T10:00:00Z");
        Set<String> distintos = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            distintos.add(generador.generar(ahora).codigo());
        }
        assertTrue(distintos.size() > 40);
    }
}
