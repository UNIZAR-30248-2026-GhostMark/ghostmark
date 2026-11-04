const MONTHS = ["ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sept", "oct", "nov", "dic"];

function sameDay(a: Date, b: Date) {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
}

// Hora de una entrada del feed como en f-feed: «hace 5 min», «hace 1 h», «ayer, 18:40».
export function feedTime(iso: string, now = new Date()) {
  const date = new Date(iso);
  const minutes = Math.floor((now.getTime() - date.getTime()) / 60_000);
  if (minutes < 1) return "ahora";
  if (minutes < 60) return `hace ${minutes} min`;
  if (sameDay(date, now)) return `hace ${Math.floor(minutes / 60)} h`;
  const time = `${date.getHours()}:${String(date.getMinutes()).padStart(2, "0")}`;
  const yesterday = new Date(now);
  yesterday.setDate(now.getDate() - 1);
  if (sameDay(date, yesterday)) return `ayer, ${time}`;
  return `${date.getDate()} ${MONTHS[date.getMonth()]}, ${time}`;
}

// Nunca dice quién eliminó a quién (criterio 2 de la PBI 7a).
export function feedText(alias: string | null) {
  return alias ? `${alias} fue eliminado` : "Una cuenta eliminada fue eliminada";
}
