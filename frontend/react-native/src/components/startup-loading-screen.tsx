import { useEffect, useRef } from 'react';
import { Animated, Easing, ImageBackground, StyleSheet, Text, View } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { StatusBar } from 'expo-status-bar';

import { BrandLogo } from '@/components/brand-logo';
import { colors, fonts, motion, spacing } from '@/constants/theme';
import { useReducedMotion } from '@/hooks/use-reduced-motion';

const DOT_DIMMED = 0.24;
const campaignBackground = require('../../assets/images/brand/lastdish-startup-campaign.png');

export function StartupLoadingScreen() {
  const reducedMotion = useReducedMotion();
  const logoPulse = useRef(new Animated.Value(0)).current;
  const dotValues = useRef([
    new Animated.Value(1),
    new Animated.Value(DOT_DIMMED),
    new Animated.Value(DOT_DIMMED),
  ]).current;

  useEffect(() => {
    const logoLoop = Animated.loop(
      Animated.sequence([
        Animated.timing(logoPulse, {
          toValue: 1,
          duration: motion.emphasis,
          easing: Easing.inOut(Easing.ease),
          useNativeDriver: true,
        }),
        Animated.timing(logoPulse, {
          toValue: 0,
          duration: motion.emphasis,
          easing: Easing.inOut(Easing.ease),
          useNativeDriver: true,
        }),
      ]),
    );
    const dotsLoop = Animated.loop(
      Animated.sequence(
        dotValues.map((_, activeIndex) =>
          Animated.parallel(
            dotValues.map((value, index) =>
              Animated.timing(value, {
                toValue: index === activeIndex ? 1 : DOT_DIMMED,
                duration: motion.base,
                easing: Easing.out(Easing.ease),
                useNativeDriver: true,
              }),
            ),
          ),
        ),
      ),
    );

    if (!reducedMotion) logoLoop.start();
    dotsLoop.start();

    return () => {
      logoLoop.stop();
      dotsLoop.stop();
    };
  }, [dotValues, logoPulse, reducedMotion]);

  const logoScale = logoPulse.interpolate({ inputRange: [0, 1], outputRange: [0.985, 1.015] });
  const logoOpacity = logoPulse.interpolate({ inputRange: [0, 1], outputRange: [0.86, 1] });

  return (
    <ImageBackground
      accessibilityLabel="라스트디시 앱을 준비하고 있어요"
      accessibilityLiveRegion="polite"
      accessibilityRole="progressbar"
      resizeMode="cover"
      source={campaignBackground}
      style={styles.screen}
    >
      <StatusBar style="light" />
      <LinearGradient colors={['rgba(12,12,11,0.72)', 'rgba(12,12,11,0.12)', 'rgba(12,12,11,0.18)', 'rgba(12,12,11,0.78)']} locations={[0, 0.34, 0.62, 1]} style={StyleSheet.absoluteFill}/>
      <View style={styles.brandLockup}>
        <View style={styles.logoSurface}>
          <BrandLogo size={54} />
        </View>
        <View>
          <Text style={styles.brand}>라스트디시</Text>
          <Text style={styles.shortBrand}>LAST DISH · 라디</Text>
        </View>
      </View>
      <View style={styles.content}>
        <Text style={styles.eyebrow}>오늘의 마지막 할인</Text>
        <Text style={styles.headline}>남은 맛을,{`\n`}좋은 가격에</Text>
        <Text style={styles.message}>가까운 매장의 따뜻한 한 끼를 준비하고 있어요</Text>
        <Animated.View
          style={[styles.loadingMark, {
            opacity: reducedMotion ? 1 : logoOpacity,
            transform: [{ scale: reducedMotion ? 1 : logoScale }],
          }]}
        >
          <BrandLogo size={42} />
        </Animated.View>
        <View accessibilityElementsHidden importantForAccessibility="no-hide-descendants" style={styles.dots}>
          {dotValues.map((value, index) => (
            <Animated.View
              key={index}
              style={[
                styles.dot,
                {
                  opacity: value,
                  transform: [{ scale: reducedMotion ? 1 : value.interpolate({ inputRange: [DOT_DIMMED, 1], outputRange: [0.82, 1] }) }],
                },
              ]}
            />
          ))}
        </View>
      </View>
    </ImageBackground>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    justifyContent: 'space-between',
    backgroundColor: colors.ink900,
    paddingHorizontal: spacing.xxl,
    paddingTop: 68,
    paddingBottom: 54,
  },
  brandLockup: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  logoSurface: {
    width: 58,
    height: 58,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 18,
    backgroundColor: 'rgba(255,255,255,0.94)',
  },
  content: {
    alignItems: 'flex-start',
  },
  brand: {
    color: colors.white,
    fontFamily: fonts.body,
    fontSize: 19,
    fontWeight: '900',
    letterSpacing: -0.6,
  },
  shortBrand: {
    marginTop: 2,
    color: 'rgba(255,255,255,0.64)',
    fontFamily: fonts.body,
    fontSize: 9,
    fontWeight: '800',
    letterSpacing: 1.1,
  },
  eyebrow: {
    color: '#8FE3AD',
    fontFamily: fonts.body,
    fontSize: 13,
    fontWeight: '900',
    letterSpacing: -0.2,
  },
  headline: {
    marginTop: 10,
    color: colors.white,
    fontFamily: fonts.body,
    fontSize: 38,
    lineHeight: 46,
    fontWeight: '900',
    letterSpacing: -1.8,
  },
  message: {
    marginTop: 12,
    color: 'rgba(255,255,255,0.72)',
    fontFamily: fonts.body,
    fontSize: 13,
    lineHeight: 20,
    fontWeight: '700',
    letterSpacing: -0.25,
  },
  loadingMark: {
    marginTop: 26,
    width: 48,
    height: 48,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 16,
    backgroundColor: 'rgba(255,255,255,0.92)',
  },
  dots: {
    marginTop: 12,
    flexDirection: 'row',
    gap: 7,
  },
  dot: {
    width: 6,
    height: 6,
    borderRadius: 3,
    backgroundColor: colors.white,
  },
});
