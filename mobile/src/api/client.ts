const defaultApiUrl = "http://localhost:8080";
let accessToken: string | undefined;
let refreshSession: (() => Promise<void>) | undefined;
let refreshInFlight: Promise<void> | undefined;

function getApiUrl() {
  return (process.env.EXPO_PUBLIC_API_URL ?? defaultApiUrl).replace(/\/$/, "");
}

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly fieldErrors: string[] = [],
    public readonly code?: string,
    public readonly fieldErrorsByName: Readonly<Record<string, string>> = {},
  ) {
    super(message);
    this.name = "ApiError";
  }
}

export function setAccessToken(token: string | undefined) {
  accessToken = token;
}

export function isExpiredAccessResponse(status: number) {
  return status === 401;
}

export function setSessionRefresher(refresher: () => Promise<void>) {
  refreshSession = refresher;
}

type RequestOptions = Omit<RequestInit, "body"> & {
  body?: unknown;
};

type ProblemDetail = {
  codigo?: string;
  detail?: string;
  errores?: { campo?: string; mensaje?: string }[];
};

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const tokenUsed = accessToken;
  let response = await sendRequest(path, options);
  if (isExpiredAccessResponse(response.status) && tokenUsed && refreshSession && !path.startsWith("/api/v1/sesiones")) {
    if (accessToken === tokenUsed) {
      refreshInFlight ??= refreshSession().finally(() => { refreshInFlight = undefined; });
      await refreshInFlight;
    }
    response = await sendRequest(path, options);
  }

  if (!response.ok) {
    const payload = (await response.json().catch(() => ({}))) as ProblemDetail;
    const namedFieldErrors = Object.fromEntries(
      (payload.errores ?? [])
        .filter((error): error is { campo: string; mensaje: string } => Boolean(error.campo && error.mensaje))
        .map((error) => [error.campo, error.mensaje]),
    );
    const fieldErrors = Object.values(namedFieldErrors);
    throw new ApiError(
      payload.detail || "La petición no pudo completarse.",
      response.status,
      fieldErrors,
      payload.codigo,
      namedFieldErrors,
    );
  }
  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}

async function sendRequest(path: string, options: RequestOptions): Promise<Response> {
  const { body, ...requestOptions } = options;
  const headers = new Headers(requestOptions.headers);
  headers.set("Accept", "application/json");

  if (accessToken) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }

  if (body !== undefined) {
    headers.set("Content-Type", "application/json");
  }

  return fetch(`${getApiUrl()}${path.startsWith("/") ? path : `/${path}`}`, {
    ...requestOptions,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });

}

export function getConfiguredApiUrl() {
  return getApiUrl();
}
