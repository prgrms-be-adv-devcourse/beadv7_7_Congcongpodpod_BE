import { Image, type ImageStyle, type StyleProp } from 'react-native';

const logo = require('../../assets/images/brand/lastdish-logo.png');

export function BrandLogo({ size = 72, style }: { size?: number; style?: StyleProp<ImageStyle> }) {
  return (
    <Image
      accessibilityIgnoresInvertColors
      accessibilityLabel="라디, 라스트디시 로고"
      resizeMode="contain"
      source={logo}
      style={[{ width: size, height: size }, style]}
    />
  );
}
