import { request } from "./client";
import type { Inscription, Player } from "../types/inscription";
import type { Schedule } from "../types/schedule";

export function joinGame(code: string) {
  return request<Inscription>("/api/v1/inscripciones", { body: { codigo: code, aceptaNormas: true }, method: "POST" });
}

export function getPlayers(gameId: string) {
  return request<Player[]>(`/api/v1/partidas/${encodeURIComponent(gameId)}/inscritos`);
}

export function getGameSchedule(gameId: string) {
  return request<Schedule>(`/api/v1/partidas/${encodeURIComponent(gameId)}/yo/horario`);
}

export function updateGameSchedule(gameId: string, schedule: Schedule) {
  return request<Schedule>(`/api/v1/partidas/${encodeURIComponent(gameId)}/yo/horario`, { body: schedule, method: "PUT" });
}
