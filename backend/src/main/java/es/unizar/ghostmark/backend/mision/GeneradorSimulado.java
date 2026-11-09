package es.unizar.ghostmark.backend.mision;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class GeneradorSimulado implements GeneradorMisiones {

    public enum Modo {
        NORMAL, FORMATO_INVALIDO, PROHIBIDAS, CAIDO, LENTO
    }

    private static final List<String> TEXTOS = List.of(
            "Que te diga cuál es su rincón favorito del campus",
            "Que te recomiende un libro que le haya gustado",
            "Que te cuente qué asignatura le cuesta más",
            "Que te enseñe la última foto de un paisaje que hizo",
            "Que te diga qué canción escucha más estos días",
            "Que te preste un lápiz durante un momento",
            "Que te diga en qué edificio tiene su próxima clase",
            "Que te cuente cuál fue su primer día en la universidad",
            "Que te recomiende una cafetería cerca de la facultad",
            "Que te diga qué serie está viendo ahora",
            "Que te dibuje un fantasma en un papel",
            "Que te diga una palabra que empiece por la letra q",
            "Que te cuente qué haría con un día libre",
            "Que te diga cuál es su comida favorita del comedor");

    private volatile Modo modo = Modo.NORMAL;

    public void usar(Modo modo) {
        this.modo = modo;
    }

    @Override
    public List<MisionGenerada> generar(int cuantas, String contexto, List<String> evitar) {
        switch (modo) {
            case CAIDO -> throw new GeneradorNoDisponible("El generador no responde");
            case LENTO -> throw new GeneradorNoDisponible("El generador tarda más de 15 s");
            case FORMATO_INVALIDO -> {
                return invalidas(cuantas);
            }
            case PROHIBIDAS -> {
                return prohibidas(cuantas, evitar);
            }
            default -> {
                return validas(cuantas, evitar);
            }
        }
    }

    private static List<MisionGenerada> validas(int cuantas, List<String> evitar) {
        Set<String> usadas = evitar.stream().map(texto -> texto.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        List<MisionGenerada> misiones = new ArrayList<>();
        int extra = 1;
        for (String texto : TEXTOS) {
            if (misiones.size() == cuantas) {
                return misiones;
            }
            if (usadas.add(texto.toLowerCase(Locale.ROOT))) {
                misiones.add(new MisionGenerada(texto, "CONVERSACION"));
            }
        }
        while (misiones.size() < cuantas) {
            String texto = "Que te cuente una curiosidad número " + extra++;
            if (usadas.add(texto.toLowerCase(Locale.ROOT))) {
                misiones.add(new MisionGenerada(texto, "OTRA"));
            }
        }
        return misiones;
    }

    private static List<MisionGenerada> invalidas(int cuantas) {
        List<MisionGenerada> misiones = new ArrayList<>();
        for (int i = 0; i < cuantas; i++) {
            MisionGenerada mision = switch (i % 3) {
                case 0 -> new MisionGenerada("Corta", "OTRA");
                case 1 -> new MisionGenerada("Que te diga\nalgo en dos líneas " + i, "OTRA");
                default -> new MisionGenerada(null, null);
            };
            misiones.add(mision);
        }
        return misiones;
    }

    private static List<MisionGenerada> prohibidas(int cuantas, List<String> evitar) {
        List<MisionGenerada> buenas = validas(cuantas, evitar);
        List<MisionGenerada> misiones = new ArrayList<>();
        for (int i = 0; i < cuantas; i++) {
            MisionGenerada mision = switch (i % 3) {
                case 0 -> new MisionGenerada("Que te dé la mano al saludarte " + i,
                        "CONTACTO_FISICO");
                case 1 -> new MisionGenerada("Que se tome una cerveza contigo " + i, "OTRA");
                default -> buenas.get(i);
            };
            misiones.add(mision);
        }
        return misiones;
    }
}
