import type { ApiResult, LoginResponse } from "@/services/contracts/backoffice";
import {
  clearStoredSession,
  getStoredSession,
  saveStoredSession,
  type StoredSession,
} from "@/services/utils/authSession";

type BackendEnvelope<T> = {
  code: string;
  message: string;
  data: T;
  requestId?: string;
};

type BackendErrorEnvelope = {
  code?: string;
  message?: string;
  details?: string[];
  requestId?: string;
};

type RequestOptions = {
  method?: "GET" | "POST" | "PUT" | "DELETE";
  body?: BodyInit | object;
  headers?: HeadersInit;
  auth?: boolean;
  retryOnAuth?: boolean;
};

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8083").replace(
  /\/$/,
  "",
);

let refreshPromise: Promise<StoredSession | null> | null = null;

export async function apiRequest<T>(
  path: string,
  {
    method = "GET",
    body,
    headers,
    auth = true,
    retryOnAuth = true,
  }: RequestOptions = {},
): Promise<ApiResult<T>> {
  const session = getStoredSession();
  const requestHeaders = new Headers(headers);
  const isFormData = typeof FormData !== "undefined" && body instanceof FormData;

  if (!isFormData && body !== undefined && !requestHeaders.has("Content-Type")) {
    requestHeaders.set("Content-Type", "application/json");
  }

  if (auth && session?.accessToken) {
    requestHeaders.set("Authorization", `Bearer ${session.accessToken}`);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers: requestHeaders,
    body: isFormData ? (body as FormData) : serializeBody(body),
  });

  if (response.status === 401 && auth && retryOnAuth && session?.refreshToken) {
    await refreshSession();
    return apiRequest<T>(path, { method, body, headers, auth, retryOnAuth: false });
  }

  if (!response.ok) {
    throw await buildRequestError(response);
  }

  const payload = (await response.json()) as BackendEnvelope<T>;
  return {
    data: payload.data,
    message: payload.message,
  };
}

export function getApiBaseUrl() {
  return API_BASE_URL;
}

export function buildApiUrl(path: string) {
  if (/^https?:\/\//i.test(path)) {
    return path;
  }

  return `${API_BASE_URL}${path.startsWith("/") ? path : `/${path}`}`;
}

export async function downloadAuthenticatedFile(path: string, suggestedFileName?: string) {
  const file = await fetchAuthenticatedFile(path);
  const url = URL.createObjectURL(file.blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = suggestedFileName ?? file.fileName ?? "download";
  link.click();
  URL.revokeObjectURL(url);
}

export async function openAuthenticatedFile(path: string) {
  const file = await fetchAuthenticatedFile(path);
  const url = URL.createObjectURL(file.blob);
  window.open(url, "_blank", "noopener,noreferrer");
  window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
}

export async function fetchAuthenticatedFile(path: string) {
  const response = await authorizedFetch(path, { method: "GET" });
  if (!response.ok) {
    throw await buildRequestError(response);
  }

  return {
    blob: await response.blob(),
    fileName: readFileNameFromResponse(response),
    mimeType: response.headers.get("Content-Type") ?? undefined,
  };
}

async function authorizedFetch(path: string, init: RequestInit, retryOnAuth = true) {
  const session = getStoredSession();
  const headers = new Headers(init.headers);
  if (session?.accessToken) {
    headers.set("Authorization", `Bearer ${session.accessToken}`);
  }

  const response = await fetch(buildApiUrl(path), {
    ...init,
    headers,
  });

  if (response.status === 401 && retryOnAuth && session?.refreshToken) {
    await refreshSession();
    return authorizedFetch(path, init, false);
  }

  return response;
}

function serializeBody(body: BodyInit | object | undefined) {
  if (body === undefined || body instanceof Blob || typeof body === "string") {
    return body;
  }

  return JSON.stringify(body);
}

function readFileNameFromResponse(response: Response) {
  const disposition = response.headers.get("Content-Disposition");
  if (!disposition) {
    return undefined;
  }

  const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i);
  if (utf8Match?.[1]) {
    return decodeURIComponent(utf8Match[1]);
  }

  const quotedMatch = disposition.match(/filename="([^"]+)"/i);
  if (quotedMatch?.[1]) {
    return quotedMatch[1];
  }

  return undefined;
}

async function buildRequestError(response: Response) {
  const contentType = response.headers.get("Content-Type") ?? "";

  if (contentType.includes("application/json")) {
    const payload = (await response.json()) as BackendErrorEnvelope;
    const message = payload.details?.find(Boolean) ?? payload.message ?? `请求失败 (${response.status})`;
    return new Error(message);
  }

  const text = await response.text();
  return new Error(text || `请求失败 (${response.status})`);
}

async function refreshSession() {
  if (!refreshPromise) {
    refreshPromise = performRefresh().finally(() => {
      refreshPromise = null;
    });
  }

  return refreshPromise;
}

async function performRefresh() {
  const current = getStoredSession();
  if (!current?.refreshToken) {
    clearStoredSession();
    throw new Error("登录已过期，请重新登录");
  }

  const response = await fetch(`${API_BASE_URL}/api/v1/auth/refresh-token`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      refreshToken: current.refreshToken,
    }),
  });

  if (!response.ok) {
    clearStoredSession();
    throw await buildRequestError(response);
  }

  const payload = (await response.json()) as BackendEnvelope<LoginResponse>;
  const nextSession: StoredSession = {
    ...current,
    ...payload.data,
  };
  saveStoredSession(nextSession);
  return nextSession;
}
