package es.unizar.ghostmark.backend.mision;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class FiltroMisionesTest {

    private final FiltroMisiones filtro = new FiltroMisiones();

    private static MisionGenerada valida(String texto) {
        return new MisionGenerada(texto, "CONVERSACION");
    }

    private List<MisionGenerada> aceptadas(String... textos) {
        return filtro.filtrar(List.of(textos).stream().map(FiltroMisionesTest::valida).toList(),
                List.of()).aceptadas();
    }

    @Test
    void aceptaLasValidasSinLosEspaciosDeLosExtremos() {
        FiltroMisiones.Resultado resultado = filtro.filtrar(
                List.of(new MisionGenerada("  Que te diga la hora  ", "GESTO")), List.of());
        assertEquals(List.of(new MisionGenerada("Que te diga la hora", "GESTO")),
                resultado.aceptadas());
        assertEquals(0, resultado.descartadas());
    }

    @Test
    void respetaLosLimitesDeLongitud() {
        String diez = "Que diga x";
        String doscientos = "Que " + "a".repeat(196);
        assertEquals(List.of(valida(diez), valida(doscientos)), aceptadas(diez, doscientos));
        assertEquals(List.of(), aceptadas("Que dig x", "Que " + "a".repeat(197)));
    }

    @Test
    void descartaLosSaltosDeLineaYLasCategoriasQueNoEstanPermitidas() {
        FiltroMisiones.Resultado resultado = filtro.filtrar(List.of(
                valida("Que te diga\nla hora"),
                valida("Que te diga\rla hora"),
                valida(null),
                new MisionGenerada("Que te diga la hora", null),
                new MisionGenerada("Que te diga la hora", "CONTACTO_FISICO"),
                new MisionGenerada("Que te diga la hora", "INVENTADA")), List.of());
        assertEquals(List.of(), resultado.aceptadas());
        assertEquals(6, resultado.descartadas());
    }

    @Test
    void ningunaPalabraDeLaListaPasaElFiltro() {
        for (String palabra : FiltroMisiones.PALABRAS) {
            assertEquals(List.of(), aceptadas("Que te hable de " + palabra + " un rato"), palabra);
        }
        assertEquals(List.of(), aceptadas("Que se ponga a REIRSE DE alguien",
                "Que te cuente si suele BEBERSE un café", "Que te enseñe sus ARMAS favoritas",
                "Que quiera besarte en la mejilla"));
    }

    @Test
    void lasPalabrasSoloCuentanSiSonPalabrasEnteras() {
        assertEquals(2, aceptadas("Que te enseñe su armario", "Que te hable de una armadura")
                .size());
    }

    @Test
    void descartaLasRepetidasEnLaPartidaYEnLaMismaTanda() {
        FiltroMisiones.Resultado resultado = filtro.filtrar(List.of(
                valida("QUE TE DIGA LA HORA"),
                valida("Que te preste un lápiz"),
                valida("que te preste un LÁPIZ")), List.of("Que te diga la hora"));
        assertEquals(List.of(valida("Que te preste un lápiz")), resultado.aceptadas());
        assertEquals(2, resultado.descartadas());
    }

    @Test
    void nuncaDevuelveMasDeOcho() {
        FiltroMisiones.Resultado resultado = filtro.filtrar(
                new GeneradorSimulado().generar(12, "Campus", List.of()), List.of());
        assertEquals(FiltroMisiones.MAXIMO, resultado.aceptadas().size());
        assertEquals(0, resultado.descartadas());
    }

    @Test
    void quitaLoQueDevuelveElSimuladoConFormatoInvalidoOProhibidas() {
        GeneradorSimulado generador = new GeneradorSimulado();
        generador.usar(GeneradorSimulado.Modo.FORMATO_INVALIDO);
        FiltroMisiones.Resultado invalidas = filtro.filtrar(
                generador.generar(12, "Campus", List.of()), List.of());
        assertEquals(List.of(), invalidas.aceptadas());
        assertEquals(12, invalidas.descartadas());
        generador.usar(GeneradorSimulado.Modo.PROHIBIDAS);
        FiltroMisiones.Resultado prohibidas = filtro.filtrar(
                generador.generar(12, "Campus", List.of()), List.of());
        assertEquals(4, prohibidas.aceptadas().size());
        assertEquals(8, prohibidas.descartadas());
    }
}
