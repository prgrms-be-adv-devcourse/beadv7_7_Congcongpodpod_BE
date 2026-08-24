import { createContext, type PropsWithChildren, useCallback, useContext, useEffect, useMemo, useState } from 'react';

import { clearSession, getMyProfile, login, loginWithKakao, refreshSessionTokens, restoreSession, withdrawAccount, type Member } from '@/lib/auth';
import { resetGlobalLoading } from '@/lib/app-overlay';

type AuthContextValue = {
  member: Member | null;
  initializing: boolean;
  signIn: (email: string, password: string) => Promise<void>;
  signInWithKakao: (code: string) => Promise<void>;
  signOut: () => Promise<void>;
  withdraw: () => Promise<void>;
  refreshProfile: () => Promise<void>;
  refreshSession: () => Promise<Member>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: PropsWithChildren) {
  const [member, setMember] = useState<Member | null>(null);
  const [initializing, setInitializing] = useState(true);

  useEffect(() => {
    void restoreSession()
      .then(setMember)
      .catch(() => { setMember(null); })
      .finally(() => setInitializing(false));
  }, []);

  const signIn = useCallback(async (email: string, password: string) => {
    setMember(await login(email, password));
  }, []);

  const signInWithKakao = useCallback(async (code: string) => {
    setMember(await loginWithKakao(code));
  }, []);

  const signOut = useCallback(async () => {
    try {
      await clearSession();
      setMember(null);
    } finally {
      resetGlobalLoading();
    }
  }, []);

  const withdraw = useCallback(async () => {
    try {
      await withdrawAccount();
      setMember(null);
    } finally {
      resetGlobalLoading();
    }
  }, []);

  const refreshProfile = useCallback(async () => setMember(await getMyProfile()), []);
  const refreshSession = useCallback(async () => {
    const profile = await refreshSessionTokens();
    setMember(profile);
    return profile;
  }, []);

  const value = useMemo(() => ({ member, initializing, signIn, signInWithKakao, signOut, withdraw, refreshProfile, refreshSession }), [member, initializing, signIn, signInWithKakao, signOut, withdraw, refreshProfile, refreshSession]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) throw new Error('useAuth must be used inside AuthProvider');
  return value;
}
