export function lifeCodeSecondsRemaining(caducaEn: string, now = Date.now()) {
  return Math.max(0, Math.ceil((Date.parse(caducaEn) - now) / 1000));
}
