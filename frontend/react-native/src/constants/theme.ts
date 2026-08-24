import { Platform } from 'react-native';

export const colors = {
  canvas: '#F7F8F6',
  canvasWarm: '#FAFAF8',
  white: '#FFFFFF',
  ink900: '#171A18',
  ink700: '#4D534F',
  ink500: '#747A75',
  ink400: '#9AA09B',
  line: '#E5E8E5',
  lineStrong: '#D1D6D2',
  green50: '#F0FFF6',
  green100: '#DDF9E9',
  green200: '#B4F0CC',
  green300: '#03C75A',
  green500: '#03C75A',
  green700: '#008F42',
  green900: '#005D2D',
  blue50: '#EEF5F8',
  blue300: '#C6DDE7',
  apricot50: '#FFF4E7',
  apricot300: '#F1C58E',
  danger50: '#FFF0ED',
  danger700: '#A44637',
  kakao: '#FEE500',
  warning: '#A66018',
} as const;

export const spacing = { xxs: 2, xs: 4, sm: 8, md: 12, lg: 16, xl: 24, xxl: 32, xxxl: 48 } as const;
export const radius = { control: 10, input: 12, card: 16, sheet: 22, navigation: 26, pill: 999 } as const;
export const fonts = {
  body: Platform.select({ ios: 'Apple SD Gothic Neo', android: 'sans-serif', default: 'system-ui' }),
  fallback: Platform.select({ ios: 'Apple SD Gothic Neo', android: 'sans-serif', default: 'system-ui' }),
} as const;

export const layout = { compact: 560, content: 760, wide: 960 } as const;
export const motion = { fast: 120, base: 200, screen: 280, emphasis: 400 } as const;
export const shadow = {
  card: { shadowColor: '#151A16', shadowOpacity: 0.075, shadowRadius: 10, shadowOffset: { width: 0, height: 4 }, elevation: 2 },
  float: { shadowColor: '#111512', shadowOpacity: 0.18, shadowRadius: 16, shadowOffset: { width: 0, height: 7 }, elevation: 8 },
} as const;
