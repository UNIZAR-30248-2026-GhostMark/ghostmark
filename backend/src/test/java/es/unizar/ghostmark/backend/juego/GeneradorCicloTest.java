package es.unizar.ghostmark.backend.juego;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class GeneradorCicloTest {

    private final GeneradorCiclo generador = new GeneradorCiclo();

    @Test
    void cadaOrdenEsUnaPermutacionDeLosJugadores() {
        List<UUID> jugadores = jugadores(10);
        for (int i = 0; i < 200; i++) {
            List<UUID> orden = generador.formar(jugadores);
            assertEquals(jugadores.size(), orden.size());
            assertEquals(Set.copyOf(jugadores), Set.copyOf(orden));
        }
    }

    @Test
    void barajaElOrden() {
        List<UUID> jugadores = jugadores(10);
        Set<List<UUID>> distintos = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            distintos.add(generador.formar(jugadores));
        }
        assertTrue(distintos.size() > 1);
    }

    @Test
    void noModificaLaListaDeEntrada() {
        List<UUID> jugadores = jugadores(10);
        List<UUID> copia = List.copyOf(jugadores);
        generador.formar(jugadores);
        assertEquals(copia, jugadores);
    }

    private static List<UUID> jugadores(int cuantos) {
        return IntStream.range(0, cuantos).mapToObj(i -> UUID.randomUUID()).toList();
    }
}
