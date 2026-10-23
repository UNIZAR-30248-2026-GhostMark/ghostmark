import * as SecureStore from "expo-secure-store";

import { ApiError, isExpiredAccessResponse, request, setAccessToken, setSessionRefresher } from "./client";

export type RegisterRequest = {
  alias: string;
  email: string;
  contrasena: string;
};

export type LoginRequest = Pick<RegisterRequest, "email" | "contrasena">;

export type AuthResponse = {
  tokenAcceso: string;
  tokenRefresco: string;
  accesoCaducaEn: string;
  cuenta: {
    id: string;
    alias: string;
    email: string;
    ubicacionAceptada: boolean;
    avisoDisponibilidadAceptado: boolean;
  };
};

const refreshKey = "ghostmark_refresh_token";
let restoration: Promise<boolean> | undefined;
let onSessionExpired: (() => void) | undefined;

export function setSessionExpiredHandler(handler?: () => void) {
  onSessionExpired = handler;
}

export function registerAccount(payload: RegisterRequest) {
  return request<AuthResponse>("/api/v1/cuentas", { method: "POST", body: payload });
}

export function login(payload: LoginRequest) {
  return request<AuthResponse>("/api/v1/sesiones", { method: "POST", body: payload });
}

export async function saveSession(session: AuthResponse) {
  await SecureStore.setItemAsync(refreshKey, session.tokenRefresco);
  setAccessToken(session.tokenAcceso);
}

export function restoreSession() {
  restoration ??= performRestore().finally(() => { restoration = undefined; });
  return restoration;
}

async function performRestore() {
  const tokenRefresco = await SecureStore.getItemAsync(refreshKey);
  if (!tokenRefresco) return false;
  try {
    const session = await request<AuthResponse>("/api/v1/sesiones/refresco", { method: "POST", body: { tokenRefresco } });
    await saveSession(session);
    return true;
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      await clearSession();
      return false;
    }
    throw error;
  }
}

setSessionRefresher(async () => {
  if (!await restoreSession()) {
    onSessionExpired?.();
    throw new ApiError("La sesión ha caducado. Vuelve a iniciar sesión.", 401);
  }
});

export async function clearSession() {
  setAccessToken(undefined);
  await SecureStore.deleteItemAsync(refreshKey);
}

export async function signOut() {
  const tokenRefresco = await SecureStore.getItemAsync(refreshKey);
  if (tokenRefresco) {
    try {
      await request<void>("/api/v1/sesiones/cierre", { method: "POST", body: { tokenRefresco } });
    } catch (error) {
      if (!(error instanceof ApiError) || !isExpiredAccessResponse(error.status)) throw error;
      if (await restoreSession()) {
        const newToken = await SecureStore.getItemAsync(refreshKey);
        await request<void>("/api/v1/sesiones/cierre", { method: "POST", body: { tokenRefresco: newToken } });
      }
    }
  }
  await clearSession();
}
