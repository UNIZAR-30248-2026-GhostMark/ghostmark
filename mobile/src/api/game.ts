import { request } from "./client";
import type { Game, GameConfiguration, GameSummary, PublicGame } from "../types/game";

export function createGame(configuration: GameConfiguration) {
  return request<Game>("/api/v1/partidas", { body: configuration, method: "POST" });
}

export function getMyGames() {
  return request<GameSummary[]>("/api/v1/cuentas/yo/partidas");
}

export function getGameByCode(code: string) {
  return request<PublicGame>(`/api/v1/partidas/codigo/${encodeURIComponent(code)}`);
}
