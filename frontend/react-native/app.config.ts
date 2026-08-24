import type { ExpoConfig } from 'expo/config';

const naverMapClientId = process.env.NAVER_MAP_CLIENT_ID;
if (naverMapClientId && !process.env.EXPO_PUBLIC_NAVER_MAP_CLIENT_ID) process.env.EXPO_PUBLIC_NAVER_MAP_CLIENT_ID = naverMapClientId;

const config: ExpoConfig = {
  name: 'Lastdish',
  slug: 'lastdish',
  scheme: 'lastdish',
  version: '1.0.0',
  orientation: 'portrait',
  userInterfaceStyle: 'light',
  icon: './assets/images/icon.png',
  ios: {
    bundleIdentifier: 'com.congcongpodpod.lastdishApp',
  },
  android: {
    package: 'com.congcongpodpod.lastdish_app',
  },
  web: { output: 'static', favicon: './assets/images/web-app-icon.png' },
  extra: { naverMapClientId },
  plugins: [
    'expo-router',
    [
      'expo-splash-screen',
      {
        backgroundColor: '#FAFAF8',
        image: './assets/images/brand/lastdish-logo.png',
        imageWidth: 132,
        resizeMode: 'contain',
      },
    ],
    'expo-secure-store',
    [
      'expo-image-picker',
      {
        photosPermission: '상품 사진을 선택하려면 사진 보관함 접근이 필요합니다.',
        cameraPermission: false,
        microphonePermission: false,
      },
    ],
    [
      '@mj-studio/react-native-naver-map',
      { client_id: naverMapClientId ?? 'NAVER_MAP_CLIENT_ID_REQUIRED' },
    ],
    [
      'expo-build-properties',
      {
        android: {
          extraMavenRepos: ['https://repository.map.naver.com/archive/maven'],
        },
      },
    ],
  ],
  experiments: { typedRoutes: true, reactCompiler: true },
};

export default config;
