import { request } from "./client";
import type { Game, GameConfiguration, GameSummary, PublicGame } from "../types/game";

export type MissionCard = {
  estado?: "VIVO" | "ELIMINADO";
  aliasObjetivo: string | null;
  mision: string | null;
  eliminaciones?: number;
};

export type LifeCode = {
  codigo: string;
  caducaEn: string;
};

export type EliminationResult = {
  aliasVictima: string | null;
  carta: MissionCard;
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

export function getMyLifeCode(gameId: string) {
  return request<LifeCode>(`/api/v1/partidas/${encodeURIComponent(gameId)}/yo/codigo`);
}

export function eliminateTarget(gameId: string, codigo: string) {
  return request<EliminationResult>(`/api/v1/partidas/${encodeURIComponent(gameId)}/eliminaciones`, {
    body: { codigo },
    method: "POST",
  });
}
