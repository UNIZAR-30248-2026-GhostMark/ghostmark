export function formatGameDate(value: string) {
  const date = new Date(value);
  const day = date.toLocaleDateString("es-ES", { day: "numeric", month: "short", weekday: "short" }).replace(",", "");
  const time = date.toLocaleTimeString("es-ES", { hour: "2-digit", hour12: false, minute: "2-digit" });
  return `${day} · ${time}`;
}
