import { Stack, type ErrorBoundaryProps } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';
import { StatusBar } from 'expo-status-bar';
import { useEffect, useState } from 'react';
import { Platform, Pressable, StyleSheet, Text, View } from 'react-native';

import { StartupLoadingScreen } from '@/components/startup-loading-screen';
import { AppOverlayProvider } from '@/components/app-overlay-provider';
import { LoginRequiredModal } from '@/components/login-required-modal';
import { NetworkStatusProvider } from '@/components/network-status-provider';
import { colors, layout } from '@/constants/theme';
import { AuthProvider, useAuth } from '@/providers/auth-provider';
import { CartProvider } from '@/providers/cart-provider';
import { NotificationProvider } from '@/providers/notification-provider';
import { StoreAvailabilityProvider } from '@/providers/store-availability-provider';

const STARTUP_MIN_DURATION_MS = 1_500;

export function ErrorBoundary({ error, retry }: ErrorBoundaryProps) {
  return <View accessibilityLiveRegion="assertive" style={styles.errorStage}>
    <Text style={styles.errorTitle}>화면을 열지 못했어요</Text>
    <Text style={styles.errorMessage}>{error.message || '잠시 후 다시 시도해주세요.'}</Text>
    <Pressable accessibilityRole="button" onPress={retry} style={styles.retryButton}><Text style={styles.retryText}>다시 시도</Text></Pressable>
  </View>;
}

void SplashScreen.preventAutoHideAsync().catch(() => undefined);

function AppBootstrap() {
  const { initializing } = useAuth();
  const [minimumDurationElapsed, setMinimumDurationElapsed] = useState(false);

  useEffect(() => {
    const timer = setTimeout(() => setMinimumDurationElapsed(true), STARTUP_MIN_DURATION_MS);
    void SplashScreen.hideAsync().catch(() => undefined);
    return () => clearTimeout(timer);
  }, []);

  if (initializing || !minimumDurationElapsed) return <StartupLoadingScreen />;

  return (
    <AppOverlayProvider>
      <NotificationProvider><CartProvider><StoreAvailabilityProvider>
        <View style={[styles.stage, Platform.OS === 'web' && styles.webStage]}>
          <Stack
            screenOptions={{
              headerShown: false,
              contentStyle: { backgroundColor: colors.canvas },
            }}
          />
        </View>
        <LoginRequiredModal />
      </StoreAvailabilityProvider></CartProvider></NotificationProvider>
    </AppOverlayProvider>
  );
}

export default function RootLayout() {
  return (
    <>
      <StatusBar style="dark" />
      <NetworkStatusProvider>
        <AuthProvider>
          <AppBootstrap />
        </AuthProvider>
      </NetworkStatusProvider>
    </>
  );
}

const styles = StyleSheet.create({
  stage: {
    flex: 1,
    width: '100%',
    maxWidth: layout.wide,
    alignSelf: 'center',
    backgroundColor: colors.canvas,
  },
  webStage: { maxWidth: layout.shell },
  errorStage: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: 12, padding: 24, backgroundColor: colors.canvas },
  errorTitle: { color: colors.ink900, fontSize: 22, fontWeight: '800' },
  errorMessage: { maxWidth: 420, color: colors.ink700, fontSize: 14, lineHeight: 21, textAlign: 'center' },
  retryButton: { minWidth: 120, minHeight: 48, marginTop: 6, alignItems: 'center', justifyContent: 'center', borderRadius: 16, backgroundColor: colors.green500 },
  retryText: { color: colors.white, fontSize: 15, fontWeight: '800' },
});
