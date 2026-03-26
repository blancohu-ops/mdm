import type { AuthMeResponse, LoginResponse } from "@/services/contracts/backoffice";

const SESSION_STORAGE_KEY = "mdm.backoffice.session";
const POST_LOGIN_REDIRECT_KEY = "mdm.backoffice.post-login-redirect";

export type StoredSession = LoginResponse & {
  userId?: string;
  enterpriseId?: string | null;
  serviceProviderId?: string | null;
  permissions?: string[];
  dataScopes?: string[];
  capabilities?: string[];
};

export function getStoredSession(): StoredSession | null {
  if (typeof window === "undefined") {
    return null;
  }

  const rawValue = window.localStorage.getItem(SESSION_STORAGE_KEY);
  if (!rawValue) {
    return null;
  }

  try {
    return JSON.parse(rawValue) as StoredSession;
  } catch {
    window.localStorage.removeItem(SESSION_STORAGE_KEY);
    return null;
  }
}

export function saveStoredSession(session: StoredSession) {
  if (typeof window === "undefined") {
    return;
  }

  window.localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session));
}

export function clearStoredSession() {
  if (typeof window === "undefined") {
    return;
  }

  window.localStorage.removeItem(SESSION_STORAGE_KEY);
}

export function setPendingPostLoginRedirect(path?: string | null) {
  if (typeof window === "undefined") {
    return;
  }

  const normalized = normalizeRedirectPath(path);
  if (normalized) {
    window.sessionStorage.setItem(POST_LOGIN_REDIRECT_KEY, normalized);
    return;
  }

  window.sessionStorage.removeItem(POST_LOGIN_REDIRECT_KEY);
}

export function getPendingPostLoginRedirect() {
  if (typeof window === "undefined") {
    return null;
  }

  return normalizeRedirectPath(window.sessionStorage.getItem(POST_LOGIN_REDIRECT_KEY));
}

export function consumePendingPostLoginRedirect() {
  const redirect = getPendingPostLoginRedirect();
  if (typeof window !== "undefined") {
    window.sessionStorage.removeItem(POST_LOGIN_REDIRECT_KEY);
  }
  return redirect;
}

export function clearPendingPostLoginRedirect() {
  if (typeof window === "undefined") {
    return;
  }

  window.sessionStorage.removeItem(POST_LOGIN_REDIRECT_KEY);
}

export function readPostLoginRedirectFromSearch(search: string) {
  const params = new URLSearchParams(search);
  return normalizeRedirectPath(params.get("redirect"));
}

export function buildLoginRedirectPath(path?: string | null) {
  const redirect = normalizeRedirectPath(path);
  return redirect ? `/auth/login?redirect=${encodeURIComponent(redirect)}` : "/auth/login";
}

export function redirectToLogin(path?: string | null) {
  if (typeof window === "undefined") {
    return;
  }

  const redirect =
    normalizeRedirectPath(path) ??
    normalizeRedirectPath(`${window.location.pathname}${window.location.search}${window.location.hash}`);

  clearStoredSession();
  setPendingPostLoginRedirect(redirect);
  const target = buildLoginRedirectPath(redirect);

  if (`${window.location.pathname}${window.location.search}` !== target) {
    window.location.replace(target);
  }
}

export function mergeStoredSessionProfile(profile: AuthMeResponse) {
  const current = getStoredSession();
  if (!current) {
    return null;
  }

  const next: StoredSession = {
    ...current,
    userId: profile.userId,
    enterpriseId: profile.enterpriseId ?? null,
    serviceProviderId: profile.serviceProviderId ?? null,
    role: profile.role,
    displayName: profile.displayName,
    organization: profile.organization,
    permissions: profile.permissions ?? [],
    dataScopes: profile.dataScopes ?? [],
    capabilities: profile.capabilities ?? [],
  };
  saveStoredSession(next);
  return next;
}

function normalizeRedirectPath(path?: string | null) {
  if (!path) {
    return null;
  }

  const trimmed = path.trim();
  if (!trimmed.startsWith("/")) {
    return null;
  }

  if (trimmed.startsWith("/auth")) {
    return null;
  }

  return trimmed;
}
