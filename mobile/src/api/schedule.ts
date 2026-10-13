import { request } from "./client";
import type { Schedule } from "../types/schedule";

export function updateSchedule(schedule: Schedule) {
  return request<Schedule>("/api/v1/cuentas/yo/horario", {
    body: schedule,
    method: "PUT",
  });
}
