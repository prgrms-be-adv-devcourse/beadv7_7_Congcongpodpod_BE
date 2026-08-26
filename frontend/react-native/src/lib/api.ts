import { beginGlobalLoading, endGlobalLoading } from './app-overlay';
import { storage } from './storage';
import { invalidateQueries } from './query-cache';

const baseUrl = (process.env.EXPO_PUBLIC_API_BASE_URL ?? 'http://127.0.0.1:8080/api/v1').replace(/\/$/, '');
type Tokens = { accessToken: string; refreshToken: string };
type TokenEnvelope = { data: Tokens };
type ApiOptions = { globalLoading?: boolean; timeoutMs?: number };
const DEFAULT_TIMEOUT_MS = 15_000;
let refreshPromise: Promise<string> | null = null;
let cachedAccessToken: string | null | undefined;

export function cacheAccessToken(token: string | null) {
  if (cachedAccessToken !== token) invalidateQueries();
  cachedAccessToken = token;
}

export function getApiBaseUrl() {
  return baseUrl;
}

export async function getAccessToken() {
  const token = cachedAccessToken === undefined ? await storage.getItem('accessToken') : cachedAccessToken;
  cachedAccessToken = token;
  return token;
}

export class NetworkUnavailableError extends Error {
  constructor() {
    super('네트워크 연결이 필요합니다. 연결 상태를 확인해주세요.');
    this.name = 'NetworkUnavailableError';
  }
}

export class RequestCancelledError extends Error {
  constructor() {
    super('요청이 취소됐어요.');
    this.name = 'RequestCancelledError';
  }
}

export class ApiError extends Error {
  constructor(message: string, public readonly code?: string, public readonly status?: number) {
    super(message);
    this.name = 'ApiError';
  }
}

async function renewAccessToken() {
  if (refreshPromise) return refreshPromise;
  refreshPromise = (async () => {
    const refreshToken = await storage.getItem('refreshToken');
    if (!refreshToken) throw new Error('로그인이 필요해요.');
    const response = await request('/auth/refresh', { method: 'POST', body: JSON.stringify({ refreshToken }) }, null);
    const body = await response.json().catch(() => null) as TokenEnvelope | null;
    if (!response.ok || !body?.data?.accessToken) throw new Error('로그인이 만료됐어요. 다시 로그인해주세요.');
    cacheAccessToken(body.data.accessToken);
    await Promise.all([storage.setItem('accessToken', body.data.accessToken), storage.setItem('refreshToken', body.data.refreshToken)]);
    return body.data.accessToken;
  })().catch(async (error) => {
    if (!(error instanceof NetworkUnavailableError)) {
      cacheAccessToken(null);
      await Promise.all([storage.deleteItem('accessToken'), storage.deleteItem('refreshToken')]);
    }
    throw error;
  }).finally(() => { refreshPromise = null; });
  return refreshPromise;
}

export function refreshAccessToken() {
  return renewAccessToken();
}

async function request(path: string, init: RequestInit | undefined, token: string | null, timeoutMs = DEFAULT_TIMEOUT_MS) {
  const isFormData = typeof FormData !== 'undefined' && init?.body instanceof FormData;
  const controller = new AbortController();
  const abortFromCaller = () => controller.abort();
  if (init?.signal?.aborted) controller.abort();
  else init?.signal?.addEventListener('abort', abortFromCaller, { once: true });
  let timedOut = false;
  const timeout = setTimeout(() => { timedOut = true; controller.abort(); }, timeoutMs);
  try {
    return await fetch(`${baseUrl}${path}`, {
      ...init,
      signal: controller.signal,
      headers: {
        Accept: 'application/json',
        ...(!isFormData ? { 'Content-Type': 'application/json' } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...init?.headers,
      },
    });
  } catch {
    if (init?.signal?.aborted && !timedOut) throw new RequestCancelledError();
    if (timedOut) throw new Error('요청 시간이 초과됐어요. 잠시 후 다시 시도해주세요.');
    throw new NetworkUnavailableError();
  } finally {
    clearTimeout(timeout);
    init?.signal?.removeEventListener('abort', abortFromCaller);
  }
}

export async function api<T>(path: string, init?: RequestInit, options: ApiOptions = {}): Promise<T> {
  // 화면은 자체 loading/refreshing 상태로 피드백을 제어한다. 모든 네트워크
  // 요청을 전역 Modal로 막으면 이전 화면의 요청이 다음 화면의 터치까지 차단한다.
  const globalLoading = options.globalLoading === true;
  if (globalLoading) beginGlobalLoading();
  try {
    const token = cachedAccessToken === undefined
      ? await storage.getItem('accessToken')
      : cachedAccessToken;
    cachedAccessToken = token;
    let response = await request(path, init, token, options.timeoutMs);
    if (response.status === 401 && token && path !== '/auth/refresh' && path !== '/auth/login') response = await request(path, init, await renewAccessToken(), options.timeoutMs);

    const body = await response.json().catch(() => null) as {
      message?: string;
      errorCode?: string;
      code?: string;
      error?: { message?: string; code?: string; errorCode?: string };
    } | null;
    if (!response.ok) {
      throw new ApiError(
        body?.error?.message ?? body?.message ?? `API ${response.status}`,
        body?.error?.code ?? body?.error?.errorCode ?? body?.errorCode ?? body?.code,
        response.status,
      );
    }
    return body as T;
  } finally {
    if (globalLoading) endGlobalLoading();
  }
}
