import { request } from "./client";
import type { AiMission, AiMissionStatus, Game, GameConfiguration, GameSummary, MissionBatch, MissionType, PublicGame } from "../types/game";

export type MissionCard = {
  estado: "VIVO" | "ELIMINADO";
  aliasObjetivo: string | null;
  mision: string | null;
  eliminaciones: number;
  /** Códigos que puede probar antes del bloqueo (3, 2 o 1); nulo si está eliminado o sin objetivo. */
  intentosRestantes: number | null;
  /** Hasta cuándo no puede eliminar tras 3 fallos seguidos; nulo si no está bloqueado. */
  bloqueadoHasta: string | null;
};

export type LifeCode = {
  codigo: string;
  caducaEn: string;
};

export type FeedEntry = {
  tipo: "ELIMINACION";
  alias: string | null;
  ocurridoEn: string;
};

export type EliminationResult = {
  aliasVictima: string | null;
  carta: MissionCard;
};

export function createGame(configuration: GameConfiguration) {
  return request<Game>("/api/v1/partidas", { body: configuration, method: "POST" });
}

export function changeMissionType(gameId: string, tipoMisiones: MissionType) {
  return request<Game>(`/api/v1/partidas/${encodeURIComponent(gameId)}`, { body: { tipoMisiones }, method: "PATCH" });
}

export function getMyGames() {
  return request<GameSummary[]>("/api/v1/cuentas/yo/partidas");
}

export function startGame(gameId: string) {
  return request<GameSummary>(`/api/v1/partidas/${encodeURIComponent(gameId)}/arranque`, { method: "POST" });
}

export function closeGame(gameId: string) {
  return request<GameSummary>(`/api/v1/partidas/${encodeURIComponent(gameId)}/cierre`, { method: "POST" });
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

export function requestAiMissionBatch(gameId: string, contexto: string) {
  return request<MissionBatch>(`/api/v1/partidas/${encodeURIComponent(gameId)}/misiones-ia/tandas`, { body: { contexto }, method: "POST" });
}

export function reviewAiMission(gameId: string, missionId: string, estado: Extract<AiMissionStatus, "APROBADA" | "RECHAZADA">) {
  return request<AiMission>(`/api/v1/partidas/${encodeURIComponent(gameId)}/misiones-ia/${encodeURIComponent(missionId)}`, { body: { estado }, method: "PATCH" });
}

export function getFeed(gameId: string) {
  return request<FeedEntry[]>(`/api/v1/partidas/${encodeURIComponent(gameId)}/feed`);
}
