const defaultApiUrl = "http://localhost:8080";

function getApiUrl() {
  return (process.env.EXPO_PUBLIC_API_URL ?? defaultApiUrl).replace(/\/$/, "");
}

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

export async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers);
  headers.set("Accept", "application/json");

  const response = await fetch(`${getApiUrl()}${path.startsWith("/") ? path : `/${path}`}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    const detail = await response.text();
    throw new ApiError(detail || "La petición no pudo completarse.", response.status);
  }

  return (await response.json()) as T;
}

export function getConfiguredApiUrl() {
  return getApiUrl();
}
