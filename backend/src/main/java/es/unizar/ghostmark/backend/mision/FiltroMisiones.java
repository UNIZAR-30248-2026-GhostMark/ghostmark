package es.unizar.ghostmark.backend.mision;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FiltroMisiones {

    public static final int MAXIMO = 8;
    static final int LONGITUD_MINIMA = 10;
    static final int LONGITUD_MAXIMA = 200;
    static final Set<String> PERMITIDAS = Set.of("CONVERSACION", "OBJETO", "FOTO", "GESTO",
            "OTRA");
    static final List<String> PALABRAS = List.of("beber", "cerveza", "tocar", "abrazar", "besar",
            "empujar", "saltar", "colarse", "reírse de", "arma", "pistola", "cuchillo", "espada");

    private static final Pattern PROHIBIDAS = Pattern.compile("(?<![\\p{L}\\p{N}])(?:"
            + PALABRAS.stream().map(palabra -> Pattern.quote(normalizar(palabra))
                    .replace(" ", "\\E\\s+\\Q")).collect(Collectors.joining("|"))
            + ")(?:s|es|se|le|la|lo|les|las|los|te|me|nos)?(?![\\p{L}\\p{N}])");

    public record Resultado(List<MisionGenerada> aceptadas, int descartadas) {
    }

    public Resultado filtrar(List<MisionGenerada> generadas, Collection<String> existentes) {
        Set<String> usadas = new HashSet<>();
        existentes.forEach(texto -> usadas.add(clave(texto)));
        List<MisionGenerada> aceptadas = new ArrayList<>();
        int descartadas = 0;
        for (MisionGenerada mision : generadas) {
            String texto = mision.texto() == null ? null : mision.texto().strip();
            if (!valida(texto, mision.categoria()) || !usadas.add(clave(texto))) {
                descartadas++;
            } else if (aceptadas.size() < MAXIMO) {
                aceptadas.add(new MisionGenerada(texto, mision.categoria()));
            }
        }
        return new Resultado(aceptadas, descartadas);
    }

    private static boolean valida(String texto, String categoria) {
        if (texto == null || categoria == null || !PERMITIDAS.contains(categoria)) {
            return false;
        }
        int longitud = texto.codePointCount(0, texto.length());
        return longitud >= LONGITUD_MINIMA && longitud <= LONGITUD_MAXIMA
                && texto.chars().noneMatch(Character::isISOControl)
                && !PROHIBIDAS.matcher(normalizar(texto)).find();
    }

    private static String clave(String texto) {
        return texto.strip().toLowerCase(Locale.ROOT);
    }

    private static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
