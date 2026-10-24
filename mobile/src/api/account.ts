import { request } from "./client";

export type AccountConsentsRequest = {
  ubicacion: true;
  avisoDisponibilidad: true;
};

export type AccountConsentsResponse = {
  ubicacionAceptada: boolean;
  avisoDisponibilidadAceptado: boolean;
};

export function saveAccountConsents(payload: AccountConsentsRequest) {
  return request<AccountConsentsResponse>("/api/v1/cuentas/yo/aceptaciones", {
    body: payload,
    method: "PUT",
  });
}

export function deleteAccount() {
  return request<void>("/api/v1/cuentas/yo", { method: "DELETE" });
}
