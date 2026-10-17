import { request } from "./client";

export type HealthResponse = { status: "UP" };

export function getHealth() {
  return request<HealthResponse>("/api/v1/health");
}
