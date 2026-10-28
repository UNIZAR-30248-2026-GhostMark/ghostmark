// Bangers deja parte del último glifo fuera de su caja en Android. El relleno
// crece con el tamaño para que el texto conserve siempre una zona de seguridad.
// El 18 % inicial no cubría los tamaños altos ni la escala de texto del sistema.
export function bangers(fontSize: number) {
  return {
    fontFamily: "Bangers" as const,
    fontSize,
    paddingRight: Math.ceil(fontSize * 0.3),
  };
}
