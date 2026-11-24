export function bangers(fontSize: number) {
  return {
    fontFamily: "Bangers" as const,
    fontSize,
    // Android recorta la segunda línea de esta fuente si se deja su métrica por defecto.
    // Todas las pantallas que usan Bangers parten de una caja vertical con holgura.
    includeFontPadding: true,
    lineHeight: Math.ceil(fontSize * 1.22),
  };
}
