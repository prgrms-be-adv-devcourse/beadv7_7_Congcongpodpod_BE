import { Stack } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';
import { StatusBar } from 'expo-status-bar';
import { useEffect, useState } from 'react';
import { StyleSheet, View } from 'react-native';

import { StartupLoadingScreen } from '@/components/startup-loading-screen';
import { AppOverlayProvider } from '@/components/app-overlay-provider';
import { LoginRequiredModal } from '@/components/login-required-modal';
import { NetworkStatusProvider } from '@/components/network-status-provider';
import { colors, layout } from '@/constants/theme';
import { AuthProvider, useAuth } from '@/providers/auth-provider';
import { CartProvider } from '@/providers/cart-provider';
import { NotificationProvider } from '@/providers/notification-provider';

const STARTUP_MIN_DURATION_MS = 1_500;

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
      <NotificationProvider><CartProvider>
        <View style={styles.stage}>
          <Stack
            screenOptions={{
              headerShown: false,
              contentStyle: { backgroundColor: colors.canvas },
            }}
          />
        </View>
        <LoginRequiredModal />
      </CartProvider></NotificationProvider>
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
});
