import type { SafeZone, ZonePoint } from "../types/game";

type Point = { x: number; y: number };
const EPSILON = 1e-10;

function point(point: ZonePoint): Point { return { x: point.lon, y: point.lat }; }
function cross(a: Point, b: Point, c: Point) { return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x); }
function onSegment(a: Point, b: Point, candidate: Point) {
  return Math.min(a.x, b.x) - EPSILON <= candidate.x && candidate.x <= Math.max(a.x, b.x) + EPSILON
    && Math.min(a.y, b.y) - EPSILON <= candidate.y && candidate.y <= Math.max(a.y, b.y) + EPSILON;
}

/** Incluye tocar o compartir un tramo: dos zonas no pueden ni cruzarse ni pisarse. */
function segmentsMeet(a: Point, b: Point, c: Point, d: Point) {
  const abC = cross(a, b, c); const abD = cross(a, b, d);
  const cdA = cross(c, d, a); const cdB = cross(c, d, b);
  if (((abC > EPSILON && abD < -EPSILON) || (abC < -EPSILON && abD > EPSILON))
    && ((cdA > EPSILON && cdB < -EPSILON) || (cdA < -EPSILON && cdB > EPSILON))) return true;
  return (Math.abs(abC) <= EPSILON && onSegment(a, b, c))
    || (Math.abs(abD) <= EPSILON && onSegment(a, b, d))
    || (Math.abs(cdA) <= EPSILON && onSegment(c, d, a))
    || (Math.abs(cdB) <= EPSILON && onSegment(c, d, b));
}

function edges(points: ZonePoint[]) {
  return points.map((current, index) => [point(current), point(points[(index + 1) % points.length]!)] as const);
}

/** Un polígono simple no permite cruces entre lados no contiguos. */
export function hasCrossedSides(points: ZonePoint[]) {
  const polygonEdges = edges(points);
  return polygonEdges.some(([a, b], index) => polygonEdges.some(([c, d], otherIndex) => {
    if (otherIndex <= index) return false;
    const adjacent = otherIndex === index + 1 || (index === 0 && otherIndex === polygonEdges.length - 1);
    return !adjacent && segmentsMeet(a, b, c, d);
  }));
}

function isInsideOrOnEdge(candidate: Point, polygon: ZonePoint[]) {
  const polygonEdges = edges(polygon);
  if (polygonEdges.some(([a, b]) => Math.abs(cross(a, b, candidate)) <= EPSILON && onSegment(a, b, candidate))) return true;
  let inside = false;
  for (const [a, b] of polygonEdges) {
    if ((a.y > candidate.y) !== (b.y > candidate.y)
      && candidate.x < ((b.x - a.x) * (candidate.y - a.y)) / (b.y - a.y) + a.x) inside = !inside;
  }
  return inside;
}

/** Devuelve la primera zona que toca o contiene al borrador, si la hay. */
export function overlappingZone(points: ZonePoint[], zones: SafeZone[]) {
  if (!points.length) return undefined;
  for (const zone of zones) {
    if (!zone.puntos.length) continue;
    const touches = edges(points).some(([a, b]) => edges(zone.puntos).some(([c, d]) => segmentsMeet(a, b, c, d)));
    if (touches || isInsideOrOnEdge(point(points[0]!), zone.puntos) || isInsideOrOnEdge(point(zone.puntos[0]!), points)) return zone;
  }
  return undefined;
}
