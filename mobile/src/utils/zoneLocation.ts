import * as Location from "expo-location";

import type { SafeZone } from "../types/game";
import { type ZoneResult, zoneResult } from "./zoneCheck";

export const LOCATION_TIMEOUT_MS = 10_000;

/**
 * Pide la posición una vez y devuelve solo el resultado de zona; la posición se descarta aquí.
 * Sin zonas no se consulta (R12-19). Si no se pudieron cargar las zonas (`null`), sin posición en
 * 10 s o con cualquier error, NO_CONCLUYENTE (R12-18, R12-37).
 */
export async function measureZone(zones: SafeZone[] | null): Promise<ZoneResult> {
  if (zones === null) return "NO_CONCLUYENTE";
  if (zones.length === 0) return "FUERA";
  let timer: ReturnType<typeof setTimeout> | undefined;
  try {
    const timeout = new Promise<null>((resolve) => { timer = setTimeout(() => resolve(null), LOCATION_TIMEOUT_MS); });
    const position = await Promise.race([Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.High }), timeout]);
    if (!position) return "NO_CONCLUYENTE";
    return zoneResult({ lat: position.coords.latitude, lon: position.coords.longitude }, position.coords.accuracy, zones);
  } catch {
    return "NO_CONCLUYENTE";
  } finally {
    clearTimeout(timer);
  }
}
