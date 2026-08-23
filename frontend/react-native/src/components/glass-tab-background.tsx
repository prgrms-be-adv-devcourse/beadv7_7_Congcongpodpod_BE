import { BlurView } from 'expo-blur';
import { StyleSheet, View } from 'react-native';

import { radius } from '@/constants/theme';

export function GlassTabBackground() {
  return <BlurView intensity={54} tint="light" style={styles.blur}><View style={styles.tint}/></BlurView>;
}

const styles = StyleSheet.create({
  blur: { ...StyleSheet.absoluteFillObject, overflow: 'hidden', borderRadius: radius.navigation },
  tint: { ...StyleSheet.absoluteFillObject, backgroundColor: 'rgba(255,255,255,0.9)', borderWidth: 1, borderColor: 'rgba(255,255,255,0.98)', borderRadius: radius.navigation },
});
