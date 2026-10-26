import { request } from "./client";
import type { Game, GameConfiguration, GameSummary, PublicGame } from "../types/game";

export type MissionCard = {
  aliasObjetivo: string | null;
  mision: string;
};

export function createGame(configuration: GameConfiguration) {
  return request<Game>("/api/v1/partidas", { body: configuration, method: "POST" });
}

export function getMyGames() {
  return request<GameSummary[]>("/api/v1/cuentas/yo/partidas");
}

export function startGame(gameId: string) {
  return request<GameSummary>(`/api/v1/partidas/${encodeURIComponent(gameId)}/arranque`, { method: "POST" });
}

export function getGameByCode(code: string) {
  return request<PublicGame>(`/api/v1/partidas/codigo/${encodeURIComponent(code)}`);
}

export function getMyMissionCard(gameId: string) {
  return request<MissionCard>(`/api/v1/partidas/${encodeURIComponent(gameId)}/yo/carta`);
}
