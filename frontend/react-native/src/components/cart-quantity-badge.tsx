import { StyleSheet, Text, View } from 'react-native';

import { colors, fonts } from '@/constants/theme';

export function CartQuantityBadge({ quantity }: { quantity: number }) {
  return <View style={styles.badge}><Text style={styles.text}>{quantity}</Text></View>;
}

const styles = StyleSheet.create({
  badge: {
    minWidth: 20,
    height: 20,
    paddingHorizontal: 5,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 10,
    backgroundColor: colors.ink900,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: 'rgba(255,255,255,0.28)',
  },
  text: { color: colors.white, fontFamily: fonts.body, fontSize: 10, fontWeight: '900' },
});
