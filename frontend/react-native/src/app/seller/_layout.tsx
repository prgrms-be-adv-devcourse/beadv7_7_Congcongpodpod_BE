import { Redirect, router, Tabs, useSegments } from 'expo-router';
import { Text } from 'react-native';

import { AnimatedTabIcon, triggerTabFeedback } from '@/components/animated-tab-icon';
import { FloatingTabBar } from '@/components/floating-tab-bar';
import { GlassTabBackground } from '@/components/glass-tab-background';
import { LoadingState } from '@/components/loading-state';
import { colors, fonts, radius } from '@/constants/theme';
import { useAuth } from '@/providers/auth-provider';

const tabs = {
  home: ['대시보드', 'grid-outline', 'grid'] as const,
  dishes: ['상품', 'fast-food-outline', 'fast-food'] as const,
  orders: ['주문', 'receipt-outline', 'receipt'] as const,
  settlements: ['정산', 'wallet-outline', 'wallet'] as const,
  exit: ['나가기', 'exit-outline', 'exit-outline'] as const,
};

export default function SellerLayout() {
  const { member, initializing } = useAuth();
  const segments = useSegments();
  const isStoreRegistration = segments[segments.length - 1] === 'store';

  if (initializing) return <LoadingState label="판매자 권한을 확인하고 있어요"/>;
  if (!member) return <Redirect href="/login"/>;
  if (member.role !== 'SELLER' && !isStoreRegistration) return <Redirect href="/my"/>;

  return <Tabs tabBar={(props) => <FloatingTabBar {...props}/>} screenListeners={({ route }) => ({ tabPress: () => triggerTabFeedback(route.name) })} screenOptions={({ route }) => {
    const item = tabs[route.name as keyof typeof tabs];
    return {
      headerShown: false,
      title: item?.[0],
      tabBarActiveTintColor: route.name === 'exit' ? colors.danger700 : colors.ink900,
      tabBarInactiveTintColor: route.name === 'exit' ? colors.danger700 : colors.ink400,
      tabBarStyle: { height: 64, paddingTop: 6, paddingBottom: 6, borderTopWidth: 0, borderRadius: radius.navigation, backgroundColor: 'transparent' },
      tabBarBackground: () => <GlassTabBackground/>,
      tabBarLabelStyle: { fontSize: 10, fontWeight: '700', fontFamily: fonts.body },
      tabBarLabel: route.name === 'exit'
        ? () => <Text style={{ color: colors.danger700, fontFamily: fonts.body, fontSize: 10, fontWeight: '700' }}>나가기</Text>
        : undefined,
      tabBarIcon: ({ color, focused }) => item ? <AnimatedTabIcon tabKey={route.name} active={item[2]} color={route.name === 'exit' ? colors.danger700 : color} focused={focused} idle={item[1]} size={focused ? 20 : 19} /> : null,
    };
  }}>
    <Tabs.Screen name="home" />
    <Tabs.Screen name="dishes" />
    <Tabs.Screen name="orders" />
    <Tabs.Screen name="settlements" />
    <Tabs.Screen name="exit" listeners={{ tabPress: (event) => { event.preventDefault(); router.replace('/my'); } }} />
    <Tabs.Screen name="store" options={{ href: null, tabBarStyle: { display: 'none' } }} />
    <Tabs.Screen name="dishes/new" options={{ href: null }} />
  </Tabs>;
}
