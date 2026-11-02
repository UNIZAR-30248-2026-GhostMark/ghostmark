package es.unizar.ghostmark.backend.juego;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GeneradorCodigoVidaTest {

    private final GeneradorCodigoVida generador = new GeneradorCodigoVida();
    private final byte[] secreto = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);

    @ParameterizedTest
    @CsvSource({"118, 287082", "2222222218, 081804", "2222222222, 050471",
            "2469135780, 005924", "4000000000, 279037", "40000000000, 353130"})
    void coincideConLosVectoresSha1DeRfc6238(long segundos, String esperado) {
        // Tiempos duplicados para usar pasos de 60 s en lugar de los 30 s del RFC
        CodigoVida codigo = generador.generar(secreto, Instant.ofEpochSecond(segundos));
        assertEquals(esperado, codigo.codigo());
        assertEquals(Instant.ofEpochSecond((segundos / 60 + 1) * 60), codigo.caducaEn());
    }

    @Test
    void mantieneElCodigoHastaElLimiteYLoRotaEnElSiguientePaso() {
        CodigoVida primero = generador.generar(secreto, Instant.EPOCH);
        assertEquals("755224", primero.codigo());
        assertEquals(Instant.ofEpochSecond(60), primero.caducaEn());
        assertEquals(primero, generador.generar(secreto, Instant.ofEpochSecond(60).minusNanos(1)));
        CodigoVida siguiente = generador.generar(secreto, Instant.ofEpochSecond(60));
        assertEquals("287082", siguiente.codigo());
        assertNotEquals(primero.codigo(), siguiente.codigo());
        assertEquals(Instant.ofEpochSecond(120), siguiente.caducaEn());
    }
}
