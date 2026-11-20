package es.unizar.ghostmark.backend.juego;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void alAnularNoJuntaAlCazadorConLosRevividosSiSePuede() {
        List<UUID> jugadores = jugadores(6);
        UUID cazador = jugadores.get(0);
        Set<UUID> revividos = Set.of(jugadores.get(1), jugadores.get(2));
        for (int i = 0; i < 200; i++) {
            List<UUID> orden = generador.formarEvitando(jugadores, cazador, revividos);
            assertEquals(Set.copyOf(jugadores), Set.copyOf(orden));
            assertFalse(GeneradorCiclo.emparejaImplicados(orden, cazador, revividos));
        }
    }

    @Test
    void siNoSePuedeEvitarSeQuedaConUnOrdenValido() {
        List<UUID> jugadores = jugadores(3);
        UUID cazador = jugadores.get(0);
        Set<UUID> revividos = Set.of(jugadores.get(1));
        List<UUID> orden = generador.formarEvitando(jugadores, cazador, revividos);
        assertEquals(Set.copyOf(jugadores), Set.copyOf(orden));
        assertTrue(GeneradorCiclo.emparejaImplicados(orden, cazador, revividos));
        List<UUID> dos = generador.formarEvitando(jugadores.subList(0, 2), cazador, revividos);
        assertEquals(Set.copyOf(jugadores.subList(0, 2)), Set.copyOf(dos));
    }

    @Test
    void detectaLasParejasEnLasDosDireccionesYSinCazadorNoHayParejas() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        UUID d = UUID.randomUUID();
        assertTrue(GeneradorCiclo.emparejaImplicados(List.of(a, b, c, d), a, Set.of(b)));
        assertTrue(GeneradorCiclo.emparejaImplicados(List.of(a, b, c, d), a, Set.of(d)));
        assertFalse(GeneradorCiclo.emparejaImplicados(List.of(a, b, c, d), a, Set.of(c)));
        assertFalse(GeneradorCiclo.emparejaImplicados(List.of(a, b, c, d), null, Set.of(b)));
    }

    private static List<UUID> jugadores(int cuantos) {
        return IntStream.range(0, cuantos).mapToObj(i -> UUID.randomUUID()).toList();
    }
}
