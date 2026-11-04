import type { GameResult } from "../types/game";

// Textos del resultado de R11-6: «Gana X con N eliminaciones», «X, último superviviente»
// o «Empate entre X e Y». Un alias nulo es una cuenta borrada.
export function winnerNames(result: GameResult) {
  return result.aliasGanadores.map((alias) => alias ?? "cuenta eliminada");
}

export function joinNames(names: string[]) {
  if (names.length <= 1) return names[0] ?? "";
  const last = names[names.length - 1] ?? "";
  // «y» pasa a «e» delante de una palabra que empieza por el sonido «i» («Marta e Inés»).
  const and = /^(i|hi)(?![aeiouáéíóú])/i.test(last) ? "e" : "y";
  return `${names.slice(0, -1).join(", ")} ${and} ${last}`;
}

export function kills(count: number) {
  return `${count} ${count === 1 ? "eliminación" : "eliminaciones"}`;
}

// Para la pantalla de fin: título grande y línea de debajo.
export function resultParts(result: GameResult): { title: string; detail: string } {
  const names = winnerNames(result);
  const first = names[0] ?? "";
  if (result.tipo === "EMPATE") return { title: "Empate", detail: `entre ${joinNames(names)}` };
  if (result.tipo === "SUPERVIVIENTE") return { title: first, detail: "último superviviente" };
  return { title: `Gana ${first}`, detail: `con ${kills(result.eliminaciones)}` };
}

// Para una línea (Inicio).
export function resultSentence(result: GameResult) {
  const names = winnerNames(result);
  if (result.tipo === "EMPATE") return `Empate entre ${joinNames(names)}`;
  if (result.tipo === "SUPERVIVIENTE") return `${names[0]}, último superviviente`;
  return `Gana ${names[0]} con ${kills(result.eliminaciones)}`;
}
