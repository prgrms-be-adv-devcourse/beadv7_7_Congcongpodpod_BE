import { RoundedIcon as Ionicons } from '@/components/rounded-icon';
import { BlurView } from 'expo-blur';
import { router } from 'expo-router';
import type { PropsWithChildren } from 'react';
import { useCallback, useEffect, useRef, useState } from 'react';
import { AccessibilityInfo, Animated, Easing, Modal, Pressable, StyleSheet, Text, useWindowDimensions, View } from 'react-native';
import { Gesture, GestureDetector } from 'react-native-gesture-handler';
import Reanimated, { Easing as ReanimatedEasing, runOnJS, useAnimatedStyle, useSharedValue, withTiming } from 'react-native-reanimated';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import Svg, { Circle, Ellipse, Path } from 'react-native-svg';

import { LoadingState } from '@/components/loading-state';
import { colors, fonts, radius, shadow, typography } from '@/constants/theme';
import { type AppAlertRequest, type AppDishReportRequest, type AppNotificationRequest, subscribeAppAlerts, subscribeDishReports, subscribeGlobalLoading, subscribeInAppNotifications } from '@/lib/app-overlay';

const MIN_LOADING_VISIBLE_MS = 160;
const REPORT_PARTICLES = Array.from({ length: 36 }, (_, index) => ({
  left: `${4 + ((index * 23) % 92)}%` as `${number}%`,
  top: 6 + ((index * 31) % 94),
  size: 2 + (index % 3),
  drift: (index % 2 === 0 ? -1 : 1) * (5 + (index % 5) * 3),
  fall: 24 + (index % 6) * 7,
}));

function notificationVisual(type?: string) {
  if (type === 'ORDER_CREATED') return { icon: 'storefront-outline' as const, label: '새 주문' };
  if (type === 'ORDER_ACCEPTED') return { icon: 'receipt-outline' as const, label: '주문 접수' };
  if (type === 'PICKUP_READY') return { icon: 'bag-check-outline' as const, label: '픽업 준비 완료' };
  if (type === 'PICKUP_STARTED') return { icon: 'time-outline' as const, label: '픽업 시작' };
  if (type === 'PICKUP_DEADLINE_SOON') return { icon: 'alarm-outline' as const, label: '마감 임박' };
  if (type === 'PICKED_UP') return { icon: 'checkmark-circle-outline' as const, label: '픽업 완료' };
  if (type === 'ORDER_NO_SHOW') return { icon: 'time-outline' as const, label: '미수령 처리' };
  if (type === 'ORDER_CANCELLED') return { icon: 'close-circle-outline' as const, label: '주문 취소' };
  if (type === 'ORDER_REJECTED') return { icon: 'alert-circle-outline' as const, label: '주문 거절' };
  if (type === 'POINT_EARNED') return { icon: 'sparkles-outline' as const, label: '포인트 적립' };
  return { icon: 'notifications-outline' as const, label: '새 알림' };
}

function NotificationToast({ notification, onDismiss }: { notification: AppNotificationRequest; onDismiss: (id: number, onDismissed?: () => void) => void }) {
  const motion = useRef(new Animated.Value(0)).current;
  const swipeX = useSharedValue(0);
  const { width } = useWindowDimensions();
  const visual = notificationVisual(notification.type);

  useEffect(() => {
    Animated.timing(motion, { toValue: 1, duration: 260, easing: Easing.bezier(0.22, 1, 0.36, 1), useNativeDriver: true }).start();
  }, [motion]);

  const dismiss = (after?: () => void, direction = 0) => {
    if (direction) swipeX.value = withTiming(direction * Math.max(width + 48, 520), { duration: 140, easing: ReanimatedEasing.out(ReanimatedEasing.cubic) });
    Animated.timing(motion, { toValue: 0, duration: 140, easing: Easing.out(Easing.cubic), useNativeDriver: true }).start(() => onDismiss(notification.id, after));
  };

  const removeAfterSwipe = () => onDismiss(notification.id);
  const swipeStyle = useAnimatedStyle(() => ({ transform: [{ translateX: swipeX.value }] }));
  const swipeGesture = Gesture.Pan()
    .activeOffsetX([-8, 8])
    .failOffsetY([-12, 12])
    .onUpdate((event) => { swipeX.value = event.translationX; })
    .onEnd((event) => {
      if (Math.abs(event.translationX) >= 72 || Math.abs(event.velocityX) >= 550) {
        const direction = event.translationX < 0 ? -1 : 1;
        swipeX.value = withTiming(direction * Math.max(width + 48, 520), { duration: 140, easing: ReanimatedEasing.out(ReanimatedEasing.cubic) }, (finished) => {
          if (finished) runOnJS(removeAfterSwipe)();
        });
      } else {
        swipeX.value = withTiming(0, { duration: 200, easing: ReanimatedEasing.bezier(0.22, 1, 0.36, 1) });
      }
    });

  return <GestureDetector gesture={swipeGesture}><Reanimated.View style={[styles.notificationAnimated, swipeStyle]}><Animated.View style={{ opacity: motion, transform: [{ translateY: motion.interpolate({ inputRange: [0, 1], outputRange: [-16, 0] }) }, { scale: motion.interpolate({ inputRange: [0, 1], outputRange: [0.98, 1] }) }] }}>
      <Pressable accessibilityRole="button" accessibilityLabel={`${notification.title}. ${notification.message}. 자세히 보기`} onPress={() => dismiss(notification.onPress)} style={({ pressed }) => [styles.notificationCard, pressed && styles.notificationPressed]}>
        <View style={styles.notificationIcon}><Ionicons name={visual.icon} size={21} color={colors.white}/><View style={styles.notificationUnread}/></View>
        <View style={styles.notificationCopy}>
          <View style={styles.notificationMeta}><Text style={styles.notificationLabel}>{visual.label}</Text><Text style={styles.notificationTime}>방금</Text></View>
          <Text numberOfLines={1} style={styles.notificationTitle}>{notification.title}</Text>
          <Text numberOfLines={3} style={styles.notificationMessage}>{notification.message}</Text>
        </View>
        <Pressable accessibilityLabel="알림 닫기" hitSlop={8} onPress={(event) => { event.stopPropagation(); dismiss(); }} style={styles.notificationClose}><Ionicons name="close" size={15} color={colors.ink500}/></Pressable>
      </Pressable>
    </Animated.View></Reanimated.View></GestureDetector>;
}

function MetricBlurCurtain({ motion, accent = false }: { motion: Animated.Value; accent?: boolean }) {
  return <Animated.View pointerEvents="none" style={[styles.reportMetricBlurLayer, {
    opacity: motion.interpolate({ inputRange: [0, 0.2, 0.82, 1], outputRange: [1, 1, 0.48, 0] }),
    transform: [{ translateY: motion.interpolate({ inputRange: [0, 0.2, 1], outputRange: [0, 0, 18] }) }, { scale: motion.interpolate({ inputRange: [0, 1], outputRange: [1, 1.025] }) }],
  }]}>
    <BlurView intensity={accent ? 30 : 22} style={[styles.reportMetricBlur, accent && styles.reportMetricAccentBlur]} tint={accent ? 'dark' : 'light'}>
      <View style={styles.reportMetricBlurPrompt}><Ionicons name="eye-outline" size={17} color={accent ? colors.white : colors.ink700}/><Text style={[styles.reportMetricBlurPromptText, accent && styles.reportMetricBlurPromptTextAccent]}>눌러서 확인</Text></View>
    </BlurView>
    {REPORT_PARTICLES.map((particle, index) => {
      const start = 0.08 + (index % 8) * 0.035;
      return <Animated.View key={`${particle.left}-${particle.top}`} style={[styles.reportParticle, accent && styles.reportParticleAccent, {
        left: particle.left,
        top: particle.top,
        width: particle.size,
        height: particle.size,
        borderRadius: particle.size / 2,
        opacity: motion.interpolate({ inputRange: [0, start, Math.min(0.72, start + 0.16), 0.78, 1], outputRange: [0, 0, 0.95, 0.7, 0] }),
        transform: [{ translateX: motion.interpolate({ inputRange: [0, 1], outputRange: [0, particle.drift] }) }, { translateY: motion.interpolate({ inputRange: [0, 1], outputRange: [0, particle.fall] }) }, { scale: motion.interpolate({ inputRange: [0, 0.55, 1], outputRange: [0.55, 1, 0.35] }) }],
      }]}/>;
    })}
  </Animated.View>;
}

function ReportTreeBackdrop({ motion }: { motion: Animated.Value }) {
  const trunkGrowth = motion.interpolate({ inputRange: [0, 0.08, 0.38, 1], outputRange: [0, 0, 1, 1] });
  const crownGrowth = motion.interpolate({ inputRange: [0, 0.24, 0.62, 1], outputRange: [0, 0, 1.04, 1] });
  const detailGrowth = motion.interpolate({ inputRange: [0, 0.42, 0.72, 1], outputRange: [0, 0, 1, 1] });

  return <View accessibilityElementsHidden importantForAccessibility="no-hide-descendants" pointerEvents="none" style={styles.reportTreeScene}>
    <Animated.View style={[styles.reportTreeTrunkArt, { opacity: trunkGrowth, transform: [{ translateY: trunkGrowth.interpolate({ inputRange: [0, 1], outputRange: [72, 0] }) }, { scaleY: trunkGrowth }] }]}>
      <Svg height="190" viewBox="0 0 180 190" width="180">
        <Path d="M76 188c7-33 8-57 7-79-1-29-9-49-22-67 12 8 23 21 31 37 4-28 13-50 27-67-8 22-12 46-11 72 12-16 27-28 44-34-19 16-32 35-40 57-8 24-9 51-2 81H76Z" fill="#6D4C31"/>
        <Path d="M91 187c5-38 5-71 0-99" fill="none" stroke="#9B6B43" strokeLinecap="round" strokeWidth="7"/>
      </Svg>
    </Animated.View>
    <Animated.View style={[styles.reportTreeCrownArt, { opacity: crownGrowth, transform: [{ translateY: crownGrowth.interpolate({ inputRange: [0, 1], outputRange: [54, 0] }) }, { scale: crownGrowth }] }]}>
      <Svg height="220" viewBox="0 0 330 220" width="330">
        <Ellipse cx="165" cy="190" fill="rgba(0,93,45,0.16)" rx="132" ry="19"/>
        <Circle cx="86" cy="122" fill="#008F42" r="54"/>
        <Circle cx="132" cy="75" fill="#03A94F" r="66"/>
        <Circle cx="199" cy="66" fill="#03C75A" r="72"/>
        <Circle cx="251" cy="112" fill="#008F42" r="58"/>
        <Circle cx="177" cy="133" fill="#00A94D" r="75"/>
        <Path d="M71 128c35 20 65 17 91-11M178 69c14 28 38 44 72 49M119 76c22 11 39 29 48 53" fill="none" opacity=".2" stroke="#F0FFF6" strokeLinecap="round" strokeWidth="7"/>
      </Svg>
    </Animated.View>
    <Animated.View style={[styles.reportTreeLightArt, { opacity: detailGrowth, transform: [{ scale: detailGrowth }] }]}>
      <Svg height="174" viewBox="0 0 300 174" width="300">
        <Circle cx="72" cy="101" fill="#62DD91" r="13"/>
        <Circle cx="119" cy="47" fill="#B4F0CC" r="10"/>
        <Circle cx="181" cy="34" fill="#DDF9E9" r="12"/>
        <Circle cx="235" cy="83" fill="#62DD91" r="11"/>
        <Circle cx="156" cy="113" fill="#B4F0CC" r="9"/>
      </Svg>
    </Animated.View>
  </View>;
}

function DishReportModal({ report, insets, onClose }: { report?: AppDishReportRequest; insets: { top: number; bottom: number }; onClose: () => void }) {
  const entrance = useRef(new Animated.Value(0)).current;
  const entranceStarted = useRef(false);
  const entranceFallback = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  const savedAmountReveal = useRef(new Animated.Value(0)).current;
  const earnedPointsReveal = useRef(new Animated.Value(0)).current;
  const [savedAmountVisible, setSavedAmountVisible] = useState(false);
  const [earnedPointsVisible, setEarnedPointsVisible] = useState(false);
  const [reduceMotion, setReduceMotion] = useState(false);

  useEffect(() => {
    AccessibilityInfo.isReduceMotionEnabled().then(setReduceMotion);
    const subscription = AccessibilityInfo.addEventListener('reduceMotionChanged', setReduceMotion);
    return () => subscription.remove();
  }, []);

  const startEntrance = useCallback(() => {
    if (!report || entranceStarted.current) return;
    entranceStarted.current = true;
    if (entranceFallback.current) clearTimeout(entranceFallback.current);
    if (reduceMotion) return entrance.setValue(1);
    entrance.stopAnimation();
    entrance.setValue(0);
    Animated.timing(entrance, { toValue: 1, duration: 900, easing: Easing.bezier(0.2, 0, 0, 1), isInteraction: false, useNativeDriver: true }).start();
  }, [entrance, reduceMotion, report]);

  useEffect(() => {
    if (!report) return;
    setSavedAmountVisible(false);
    setEarnedPointsVisible(false);
    savedAmountReveal.setValue(0);
    earnedPointsReveal.setValue(0);
    entranceStarted.current = false;
    entrance.stopAnimation();
    entrance.setValue(reduceMotion ? 1 : 0);
    entranceFallback.current = setTimeout(startEntrance, 120);
    return () => {
      if (entranceFallback.current) clearTimeout(entranceFallback.current);
    };
  }, [earnedPointsReveal, entrance, reduceMotion, report, savedAmountReveal, startEntrance]);

  const revealMetric = (motion: Animated.Value, setVisible: (visible: boolean) => void) => {
    if (reduceMotion) {
      motion.setValue(1);
      setVisible(true);
      return;
    }
    motion.stopAnimation();
    Animated.timing(motion, { toValue: 1, duration: 800, easing: Easing.bezier(0.22, 1, 0.36, 1), isInteraction: false, useNativeDriver: true }).start(({ finished }) => {
      if (finished) setVisible(true);
    });
  };

  const navigate = (path: '/grades' | '/points') => {
    onClose();
    requestAnimationFrame(() => router.push(path));
  };

  return <Modal animationType="none" onRequestClose={onClose} onShow={startEntrance} presentationStyle="overFullScreen" transparent visible={Boolean(report)}>
    <View style={[styles.reportRoot, { paddingTop: Math.max(24, insets.top), paddingBottom: Math.max(24, insets.bottom) }]}>
      <View style={styles.reportScene}>
      <ReportTreeBackdrop motion={entrance}/>
      <Animated.View renderToHardwareTextureAndroid style={[styles.reportCardStage, { opacity: entrance.interpolate({ inputRange: [0, 0.56, 0.78, 1], outputRange: [0, 0, 1, 1] }), transform: [{ translateY: entrance.interpolate({ inputRange: [0, 0.56, 0.82, 1], outputRange: [34, 34, -2, 0] }) }, { scale: entrance.interpolate({ inputRange: [0, 0.56, 0.82, 1], outputRange: [0.96, 0.96, 1.008, 1] }) }] }]}>
        <Animated.View pointerEvents="none" style={[styles.reportDepthBack, { opacity: entrance.interpolate({ inputRange: [0, 0.38, 1], outputRange: [0, 0.72, 0.42] }), transform: [{ translateY: entrance.interpolate({ inputRange: [0, 1], outputRange: [18, 11] }) }, { scale: entrance.interpolate({ inputRange: [0, 1], outputRange: [0.92, 0.96] }) }] }]}/>
        <Animated.View pointerEvents="none" style={[styles.reportDepthMiddle, { opacity: entrance.interpolate({ inputRange: [0, 0.28, 1], outputRange: [0, 0.9, 0.62] }), transform: [{ translateY: entrance.interpolate({ inputRange: [0, 1], outputRange: [11, 6] }) }, { scale: entrance.interpolate({ inputRange: [0, 1], outputRange: [0.95, 0.98] }) }] }]}/>
        <View accessibilityRole="alert" accessibilityViewIsModal style={styles.reportCard}>
        <View style={styles.reportHeader}>
          <View style={styles.reportMark}><Ionicons name="leaf-outline" size={22} color={colors.white}/></View>
          <View style={styles.reportHeaderCopy}><Text style={styles.reportEyebrow}>픽업 완료 리포트</Text><Text style={styles.reportTitle}>오늘도 한 끼를 구조했어요</Text></View>
          <Pressable accessibilityLabel="리포트 닫기" hitSlop={8} onPress={onClose} style={styles.reportClose}><Ionicons name="close" size={17} color={colors.ink500}/></Pressable>
        </View>
        <View style={styles.reportPurchase}><View><Text style={styles.reportLabel}>구매 정보</Text><Text style={styles.reportPurchaseTitle}>픽업 완료 · 구매 반영 완료</Text></View><Ionicons name="receipt-outline" size={21} color={colors.green700}/></View>
        <Pressable accessibilityRole="button" onPress={() => navigate('/grades')} style={({ pressed }) => [styles.reportLevel, pressed && styles.reportPressed]}><View><Text style={styles.reportLabel}>현재 등급</Text><Text style={styles.reportLevelValue}>{report?.level ? `Lv.${report.level} · ${report.grade}` : '등급 확인 중'}</Text>{report?.remainToNextLevel !== undefined ? <Text style={styles.reportLevelHint}>{report.remainToNextLevel > 0 ? `다음 등급까지 픽업 ${report.remainToNextLevel}회` : '현재 최고 등급이에요'}</Text> : null}</View><Ionicons name="chevron-forward" size={19} color={colors.green700}/></Pressable>
        <View style={styles.reportMetrics}>
          <Pressable accessibilityRole="button" accessibilityLabel={savedAmountVisible ? `누적 절약 금액 ${report?.savedAmount?.toLocaleString() ?? '확인 불가'}원` : '누적 절약 금액, 눌러서 확인'} onPress={() => !savedAmountVisible && revealMetric(savedAmountReveal, setSavedAmountVisible)} style={({ pressed }) => [styles.reportMetric, pressed && styles.reportMetricPressed]}><Text style={styles.reportMetricLabel}>누적 절약 금액</Text><Text style={styles.reportMetricValue}>{report?.savedAmount === undefined ? '—' : `${report.savedAmount.toLocaleString()}원`}</Text><Text style={styles.reportMetricReveal}>{savedAmountVisible ? '공개됨' : '금액 확인 완료'}</Text>{!savedAmountVisible ? <MetricBlurCurtain motion={savedAmountReveal}/> : null}</Pressable>
          <Pressable accessibilityRole="button" accessibilityLabel={earnedPointsVisible ? `지금 적립된 포인트 ${report?.earnedPoints?.toLocaleString() ?? '확인 불가'}포인트` : '지금 적립된 포인트, 눌러서 확인'} onPress={() => !earnedPointsVisible && revealMetric(earnedPointsReveal, setEarnedPointsVisible)} style={({ pressed }) => [styles.reportMetric, styles.reportMetricAccent, pressed && styles.reportMetricPressed]}><Text style={styles.reportMetricAccentLabel}>지금 적립된 포인트</Text><Text style={styles.reportMetricAccentValue}>{report?.earnedPoints === undefined ? '—' : `+${report.earnedPoints.toLocaleString()}P`}</Text><Text style={styles.reportMetricLink}>{earnedPointsVisible ? '공개됨' : '포인트 확인 완료'}</Text>{!earnedPointsVisible ? <MetricBlurCurtain accent motion={earnedPointsReveal}/> : null}</Pressable>
        </View>
        <View style={styles.reportTotal}><Text style={styles.reportLabel}>총 구매 횟수</Text><Text style={styles.reportTotalValue}>{report?.purchaseCount === undefined ? '—' : `${report.purchaseCount.toLocaleString()}회`}</Text></View>
        <View style={styles.reportActions}><Pressable onPress={onClose} style={({ pressed }) => [styles.reportLater, pressed && styles.pressed]}><Text style={styles.reportLaterText}>닫기</Text></Pressable><Pressable onPress={() => navigate('/grades')} style={({ pressed }) => [styles.reportPrimary, pressed && styles.pressed]}><Text style={styles.reportPrimaryText}>내 등급 확인하기</Text></Pressable></View>
        </View>
      </Animated.View>
      </View>
    </View>
  </Modal>;
}

export function AppOverlayProvider({ children }: PropsWithChildren) {
  const insets = useSafeAreaInsets();
  const { height } = useWindowDimensions();
  const [alert, setAlert] = useState<AppAlertRequest>();
  const [loadingCount, setLoadingCount] = useState(0);
  const [showLoading, setShowLoading] = useState(false);
  const [notifications, setNotifications] = useState<AppNotificationRequest[]>([]);
  const [dishReport, setDishReport] = useState<AppDishReportRequest>();
  const loadingVisible = useRef(false);
  const loadingShownAt = useRef(0);
  const hideLoadingTimer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  const maxVisibleNotifications = Math.max(1, Math.min(4, Math.floor((height - insets.top - insets.bottom - 24) / 116)));
  const maxVisibleRef = useRef(maxVisibleNotifications);

  useEffect(() => subscribeAppAlerts(setAlert), []);
  useEffect(() => subscribeDishReports(setDishReport), []);
  useEffect(() => subscribeGlobalLoading(setLoadingCount), []);
  useEffect(() => {
    maxVisibleRef.current = maxVisibleNotifications;
    setNotifications(current => current.slice(-maxVisibleNotifications));
  }, [maxVisibleNotifications]);
  useEffect(() => subscribeInAppNotifications(request => {
    setNotifications(current => [...current, request].slice(-maxVisibleRef.current));
  }), []);
  useEffect(() => {
    if (hideLoadingTimer.current) clearTimeout(hideLoadingTimer.current);
    if (loadingCount > 0) {
      if (!loadingVisible.current) {
        loadingVisible.current = true;
        loadingShownAt.current = Date.now();
        setShowLoading(true);
      }
      return;
    }
    if (!loadingVisible.current) return;
    const remaining = Math.max(0, MIN_LOADING_VISIBLE_MS - (Date.now() - loadingShownAt.current));
    hideLoadingTimer.current = setTimeout(() => {
      loadingVisible.current = false;
      setShowLoading(false);
    }, remaining);
  }, [loadingCount]);
  useEffect(() => () => { if (hideLoadingTimer.current) clearTimeout(hideLoadingTimer.current); }, []);

  const dismiss = () => {
    const cancel = alert?.buttons.find(button => button.style === 'cancel');
    setAlert(undefined);
    cancel?.onPress?.();
  };

  const dismissNotification = (id: number, onDismissed?: () => void) => {
    setNotifications(current => current.filter(item => item.id !== id));
    onDismissed?.();
  };

  return <>
    {children}
    {notifications.length ? <View pointerEvents="box-none" style={[styles.notificationLayer, { top: Math.max(12, insets.top + 8) }]}>{notifications.map(notification => <NotificationToast key={notification.id} notification={notification} onDismiss={dismissNotification}/>)}</View> : null}
    <Modal animationType="fade" onRequestClose={dismiss} presentationStyle="overFullScreen" transparent visible={Boolean(alert)}>
      <View style={[styles.root, { paddingBottom: Math.max(20, insets.bottom) }]}>
        <Pressable accessibilityLabel="알림 닫기" onPress={dismiss} style={styles.scrim}/>
        {alert ? <View accessibilityRole="alert" accessibilityViewIsModal style={styles.card}>
          <View style={[styles.icon, alert.buttons.some(button => button.style === 'destructive') && styles.iconDanger]}><Ionicons name={alert.buttons.some(button => button.style === 'destructive') ? 'alert-circle-outline' : 'notifications-outline'} size={21} color={alert.buttons.some(button => button.style === 'destructive') ? colors.danger700 : colors.green700}/></View>
          <Text style={styles.title}>{alert.title}</Text>
          {alert.message ? <Text style={styles.message}>{alert.message}</Text> : null}
          <View style={styles.actions}>{alert.buttons.map((button, index) => <Pressable accessibilityRole="button" key={`${alert.id}-${index}`} onPress={() => { setAlert(undefined); button.onPress?.(); }} style={({ pressed }) => [styles.action, button.style === 'cancel' && styles.cancel, button.style === 'destructive' && styles.danger, pressed && styles.pressed]}><Text style={[styles.actionText, button.style === 'cancel' && styles.cancelText]}>{button.text ?? '확인'}</Text></Pressable>)}</View>
        </View> : null}
      </View>
    </Modal>
    <DishReportModal report={dishReport} insets={insets} onClose={() => setDishReport(undefined)}/>
    {showLoading ? <View accessibilityLabel="처리 중" accessibilityRole="progressbar" pointerEvents="none" style={styles.loadingRoot}><View style={styles.loadingCard}><LoadingState compact inline label="잠시만 기다려주세요"/></View></View> : null}
  </>;
}

const styles = StyleSheet.create({
  root: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingHorizontal: 24 },
  scrim: { ...StyleSheet.absoluteFillObject, backgroundColor: 'rgba(15,20,17,0.48)' },
  card: { width: '100%', maxWidth: 360, paddingHorizontal: 20, paddingTop: 24, paddingBottom: 18, alignItems: 'center', borderRadius: radius.card, borderWidth: StyleSheet.hairlineWidth, borderColor: colors.lineStrong, backgroundColor: colors.white, ...shadow.sheet },
  icon: { width: 44, height: 44, alignItems: 'center', justifyContent: 'center', borderRadius: 22, backgroundColor: colors.green50 },
  iconDanger: { backgroundColor: colors.danger50 },
  title: { ...typography.sectionTitle, marginTop: 15, color: colors.ink900, fontFamily: fonts.body, textAlign: 'center' },
  message: { marginTop: 7, color: colors.ink700, fontFamily: fonts.body, fontSize: 13, lineHeight: 20, textAlign: 'center' },
  actions: { width: '100%', marginTop: 22, flexDirection: 'row', gap: 8 },
  action: { minHeight: 50, flex: 1, alignItems: 'center', justifyContent: 'center', borderRadius: radius.input, backgroundColor: colors.green500 },
  cancel: { borderWidth: 1, borderColor: colors.lineStrong, backgroundColor: colors.white },
  danger: { backgroundColor: colors.danger700 },
  actionText: { color: colors.white, fontFamily: fonts.body, fontSize: 14, fontWeight: '900' },
  cancelText: { color: colors.ink900 },
  pressed: { opacity: 0.72, transform: [{ scale: 0.985 }] },
  loadingRoot: { ...StyleSheet.absoluteFillObject, zIndex: 2000, alignItems: 'center', justifyContent: 'center', paddingHorizontal: 24, backgroundColor: 'rgba(247,248,246,0.82)' },
  loadingCard: { width: 148, minHeight: 104, alignItems: 'center', justifyContent: 'center' },
  notificationLayer: { position: 'absolute', left: 12, right: 12, zIndex: 1000, alignItems: 'center', gap: 8 },
  notificationAnimated: { width: '100%', maxWidth: 440, borderRadius: radius.input, ...shadow.notification },
  notificationCard: { width: '100%', minHeight: 96, paddingLeft: 12, paddingRight: 46, paddingVertical: 12, flexDirection: 'row', alignItems: 'center', gap: 12, overflow: 'hidden', borderRadius: radius.input, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.lineStrong },
  notificationIcon: { width: 42, height: 42, alignItems: 'center', justifyContent: 'center', borderRadius: radius.control, backgroundColor: colors.green900 },
  notificationUnread: { position: 'absolute', right: 4, top: 4, width: 7, height: 7, borderRadius: 4, backgroundColor: colors.white, borderWidth: 1.5, borderColor: colors.green900 },
  notificationCopy: { flex: 1, minWidth: 0 },
  notificationMeta: { marginBottom: 3, flexDirection: 'row', alignItems: 'center', gap: 6 },
  notificationLabel: { color: colors.green900, fontFamily: fonts.body, fontSize: 11, lineHeight: 15, fontWeight: '800' },
  notificationTime: { color: colors.ink400, fontFamily: fonts.body, fontSize: 10, lineHeight: 14 },
  notificationTitle: { color: colors.ink900, fontFamily: fonts.body, fontSize: 15, lineHeight: 20, fontWeight: '800', letterSpacing: -0.25 },
  notificationMessage: { marginTop: 3, color: colors.ink700, fontFamily: fonts.body, fontSize: 12, lineHeight: 17 },
  notificationClose: { position: 'absolute', top: 9, right: 9, width: 30, height: 30, alignItems: 'center', justifyContent: 'center', borderRadius: 15, backgroundColor: colors.white },
  notificationPressed: { opacity: 0.94, transform: [{ scale: 0.99 }] },
  reportRoot: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingHorizontal: 18, backgroundColor: 'rgba(15,20,17,0.58)' },
  reportScene: { width: '100%', maxWidth: 410, position: 'relative', alignItems: 'center' },
  reportTreeScene: { position: 'absolute', top: -172, width: 350, height: 300, alignItems: 'center', justifyContent: 'flex-end' },
  reportTreeTrunkArt: { position: 'absolute', bottom: 8, zIndex: 1, transformOrigin: 'bottom' },
  reportTreeCrownArt: { position: 'absolute', top: 0, zIndex: 2 },
  reportTreeLightArt: { position: 'absolute', top: 20, zIndex: 3 },
  reportCardStage: { width: '100%', position: 'relative', zIndex: 4 },
  reportDepthBack: { ...StyleSheet.absoluteFillObject, borderRadius: radius.sheet, backgroundColor: colors.green700 },
  reportDepthMiddle: { ...StyleSheet.absoluteFillObject, borderRadius: radius.sheet, backgroundColor: colors.ink700 },
  reportCard: { width: '100%', padding: 18, borderRadius: radius.sheet, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.lineStrong, ...shadow.float },
  reportHeader: { flexDirection: 'row', alignItems: 'center', gap: 11 },
  reportMark: { width: 46, height: 46, alignItems: 'center', justifyContent: 'center', borderRadius: 15, backgroundColor: colors.green700 },
  reportHeaderCopy: { flex: 1, minWidth: 0 },
  reportEyebrow: { color: colors.green700, fontFamily: fonts.body, fontSize: 11, fontWeight: '900' },
  reportTitle: { marginTop: 3, color: colors.ink900, fontFamily: fonts.body, fontSize: 19, lineHeight: 25, fontWeight: '900', letterSpacing: -0.5 },
  reportClose: { width: 34, height: 34, alignItems: 'center', justifyContent: 'center', borderRadius: 17, backgroundColor: colors.canvas },
  reportPurchase: { minHeight: 70, marginTop: 18, paddingHorizontal: 14, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', borderRadius: radius.input, backgroundColor: colors.canvas },
  reportLabel: { color: colors.ink500, fontFamily: fonts.body, fontSize: 10, fontWeight: '800' },
  reportPurchaseTitle: { marginTop: 5, color: colors.ink900, fontFamily: fonts.body, fontSize: 14, fontWeight: '900' },
  reportLevel: { minHeight: 76, marginTop: 8, paddingHorizontal: 14, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', borderRadius: radius.input, borderWidth: 1, borderColor: colors.line, backgroundColor: colors.white },
  reportLevelValue: { marginTop: 5, color: colors.ink900, fontFamily: fonts.body, fontSize: 18, fontWeight: '900', letterSpacing: -0.4 },
  reportLevelHint: { marginTop: 4, color: colors.ink500, fontFamily: fonts.body, fontSize: 10, fontWeight: '700' },
  reportPressed: { backgroundColor: colors.green50, borderColor: colors.green300 },
  reportMetrics: { marginTop: 8, flexDirection: 'row', gap: 8 },
  reportMetric: { minHeight: 112, flex: 1, padding: 14, justifyContent: 'space-between', overflow: 'hidden', borderRadius: radius.input, backgroundColor: colors.canvas },
  reportMetricAccent: { backgroundColor: colors.green700 },
  reportMetricLabel: { color: colors.ink500, fontFamily: fonts.body, fontSize: 10, fontWeight: '800' },
  reportMetricValue: { color: colors.ink900, fontFamily: fonts.body, fontSize: 19, fontWeight: '900', letterSpacing: -0.5 },
  reportMetricAccentLabel: { color: colors.green100, fontFamily: fonts.body, fontSize: 10, fontWeight: '800' },
  reportMetricAccentValue: { color: colors.white, fontFamily: fonts.body, fontSize: 20, fontWeight: '900', letterSpacing: -0.5 },
  reportMetricLink: { color: colors.green100, fontFamily: fonts.body, fontSize: 9, fontWeight: '800' },
  reportMetricReveal: { color: colors.ink500, fontFamily: fonts.body, fontSize: 9, fontWeight: '800' },
  reportMetricBlurLayer: { ...StyleSheet.absoluteFillObject, overflow: 'hidden', borderRadius: radius.input },
  reportMetricBlur: { ...StyleSheet.absoluteFillObject, alignItems: 'center', justifyContent: 'center', backgroundColor: 'rgba(247,248,246,0.48)' },
  reportMetricAccentBlur: { backgroundColor: 'rgba(0,93,45,0.44)' },
  reportMetricBlurPrompt: { alignItems: 'center', gap: 5 },
  reportMetricBlurPromptText: { color: colors.ink700, fontFamily: fonts.body, fontSize: 10, fontWeight: '900' },
  reportMetricBlurPromptTextAccent: { color: colors.white },
  reportParticle: { position: 'absolute', backgroundColor: 'rgba(77,83,79,0.7)' },
  reportParticleAccent: { backgroundColor: 'rgba(221,249,233,0.9)' },
  reportMetricPressed: { opacity: 0.88 },
  reportTotal: { minHeight: 62, marginTop: 8, paddingHorizontal: 14, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', borderRadius: radius.input, borderWidth: 1, borderColor: colors.line, backgroundColor: colors.white },
  reportTotalValue: { color: colors.ink900, fontFamily: fonts.body, fontSize: 17, fontWeight: '900' },
  reportActions: { marginTop: 14, flexDirection: 'row', gap: 8 },
  reportLater: { minHeight: 50, flex: 0.7, alignItems: 'center', justifyContent: 'center', borderRadius: radius.input, borderWidth: 1, borderColor: colors.lineStrong, backgroundColor: colors.white },
  reportLaterText: { color: colors.ink700, fontFamily: fonts.body, fontSize: 13, fontWeight: '800' },
  reportPrimary: { minHeight: 50, flex: 1.3, alignItems: 'center', justifyContent: 'center', borderRadius: radius.input, backgroundColor: colors.green700 },
  reportPrimaryText: { color: colors.white, fontFamily: fonts.body, fontSize: 13, fontWeight: '900' },
});
