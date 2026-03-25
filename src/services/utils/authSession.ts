import type { AuthMeResponse, LoginResponse } from "@/services/contracts/backoffice";

const SESSION_STORAGE_KEY = "mdm.backoffice.session";

export type StoredSession = LoginResponse & {
  userId?: string;
  enterpriseId?: string | null;
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

export function mergeStoredSessionProfile(profile: AuthMeResponse) {
  const current = getStoredSession();
  if (!current) {
    return null;
  }

  const next: StoredSession = {
    ...current,
    userId: profile.userId,
    enterpriseId: profile.enterpriseId ?? null,
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
