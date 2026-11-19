import type { SafeZone, ZonePoint } from "../types/game";

export type ZoneResult = "DENTRO" | "FUERA" | "NO_CONCLUYENTE";

// Precisión peor que esta (radio en metros) no concluye nada (R12-18).
export const MAX_ACCURACY_METERS = 50;
const EARTH_RADIUS_METERS = 6_371_000;

type Xy = { x: number; y: number };

// Proyección local en metros alrededor de la posición: a la escala de un campus, el error es despreciable.
function project(point: ZonePoint, origin: ZonePoint): Xy {
  const toRad = Math.PI / 180;
  return {
    x: (point.lon - origin.lon) * toRad * Math.cos(origin.lat * toRad) * EARTH_RADIUS_METERS,
    y: (point.lat - origin.lat) * toRad * EARTH_RADIUS_METERS,
  };
}

// Distancia del origen (la posición) al segmento a-b.
function distanceToSegment(a: Xy, b: Xy) {
  const dx = b.x - a.x;
  const dy = b.y - a.y;
  const length = dx * dx + dy * dy;
  const t = length === 0 ? 0 : Math.max(0, Math.min(1, -(a.x * dx + a.y * dy) / length));
  return Math.hypot(a.x + t * dx, a.y + t * dy);
}

// Lados del polígono: cada punto con el siguiente, y el último con el primero.
function edges(ring: Xy[]): [Xy, Xy][] {
  return ring.map((a, index) => [a, ring[(index + 1) % ring.length] ?? a]);
}

// Rayo hacia +x desde el origen: dentro si cruza un número impar de lados.
function containsOrigin(ring: Xy[]) {
  let inside = false;
  for (const [a, b] of edges(ring)) {
    if ((a.y > 0) !== (b.y > 0) && 0 < ((b.x - a.x) * -a.y) / (b.y - a.y) + a.x) inside = !inside;
  }
  return inside;
}

/**
 * Resultado de zona al meter el código (diseño de la 10b): DENTRO si el círculo de la precisión cae
 * entero dentro de alguna zona, FUERA si cae entero fuera de todas y NO_CONCLUYENTE si cruza un borde,
 * no hay posición o la precisión es peor de 50 m. Sin zonas, FUERA. La posición no sale de aquí.
 */
export function zoneResult(position: ZonePoint | null, accuracy: number | null, zones: SafeZone[]): ZoneResult {
  if (zones.length === 0) return "FUERA";
  if (!position || accuracy === null || !Number.isFinite(accuracy) || accuracy > MAX_ACCURACY_METERS) return "NO_CONCLUYENTE";
  let crosses = false;
  for (const zone of zones) {
    const ring = zone.puntos.map((point) => project(point, position));
    const inside = containsOrigin(ring);
    const nearest = Math.min(...edges(ring).map(([a, b]) => distanceToSegment(a, b)));
    if (nearest < accuracy) crosses = true;
    else if (inside) return "DENTRO";
  }
  return crosses ? "NO_CONCLUYENTE" : "FUERA";
}
