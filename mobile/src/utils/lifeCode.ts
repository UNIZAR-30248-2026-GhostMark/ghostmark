export function lifeCodeSecondsRemaining(caducaEn: string, now = Date.now()) {
  return Math.max(0, Math.ceil((Date.parse(caducaEn) - now) / 1000));
}

export function formatLifeCodeCountdown(seconds: number) {
  const minutes = Math.floor(seconds / 60).toString().padStart(2, "0");
  const remainder = (seconds % 60).toString().padStart(2, "0");
  return `${minutes}:${remainder}`;
}
