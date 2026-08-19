import { api, cacheAccessToken, NetworkUnavailableError } from './api';
import { storage } from './storage';

export type Member = {
  id: number;
  userName: string;
  name: string;
  phone: string;
  email: string;
  role: string;
};

type Envelope<T> = { success: boolean; data: T };
type Tokens = { accessToken: string; refreshToken: string };
const MEMBER_CACHE_KEY = 'lastDishMember';

async function cacheMember(member: Member) {
  await storage.setItem(MEMBER_CACHE_KEY, JSON.stringify(member));
  return member;
}

async function getCachedMember() {
  const value = await storage.getItem(MEMBER_CACHE_KEY);
  if (!value) return null;
  try { return JSON.parse(value) as Member; } catch { return null; }
}

async function saveTokens(tokens: Tokens) {
  cacheAccessToken(tokens.accessToken);
  try {
    await Promise.all([
      storage.setItem('accessToken', tokens.accessToken),
      storage.setItem('refreshToken', tokens.refreshToken),
    ]);
  } catch (error) {
    cacheAccessToken(null);
    throw error;
  }
}

async function finishLogin(tokens: Tokens) {
  const [profile] = await Promise.all([getMyProfile(), saveTokens(tokens)]);
  return profile;
}

export async function login(email: string, password: string) {
  const result = await api<Envelope<Tokens>>('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  });
  cacheAccessToken(result.data.accessToken);
  return finishLogin(result.data);
}

export async function loginWithKakao(code: string) {
  const result = await api<Envelope<Tokens>>(`/auth/kakao?code=${encodeURIComponent(code)}`, {
    method: 'POST',
  });
  cacheAccessToken(result.data.accessToken);
  return finishLogin(result.data);
}

export async function getMyProfile() {
  const result = await api<Envelope<Member>>('/members/me');
  return cacheMember(result.data);
}

export async function withdrawAccount() {
  await api<Envelope<null>>('/auth/withdraw', { method: 'PATCH' });
  await clearSession();
}

export async function refreshSessionTokens() {
  const refreshToken = await storage.getItem('refreshToken');
  if (!refreshToken) throw new Error('로그인이 필요해요.');
  const result = await api<Envelope<Tokens>>('/auth/refresh', {
    method: 'POST',
    body: JSON.stringify({ refreshToken }),
  });
  await saveTokens(result.data);
  return getMyProfile();
}

export async function hasSession() {
  const [accessToken, refreshToken] = await Promise.all([
    storage.getItem('accessToken'),
    storage.getItem('refreshToken'),
  ]);
  return Boolean(accessToken || refreshToken);
}

export async function restoreSession() {
  if (!(await hasSession())) return null;
  try {
    return await getMyProfile();
  } catch (error) {
    if (error instanceof NetworkUnavailableError) return getCachedMember();
    try {
      return await refreshSessionTokens();
    } catch (refreshError) {
      if (refreshError instanceof NetworkUnavailableError) return getCachedMember();
      throw refreshError;
    }
  }
}

export async function clearSession() {
  cacheAccessToken(null);
  await Promise.all([
    storage.deleteItem('accessToken'),
    storage.deleteItem('refreshToken'),
    storage.deleteItem(MEMBER_CACHE_KEY),
  ]);
}
