import { Redirect, router, Slot, Tabs, useSegments } from 'expo-router';

import { AnimatedTabIcon, triggerTabFeedback } from '@/components/animated-tab-icon';
import { LoadingState } from '@/components/loading-state';
import { colors, fonts } from '@/constants/theme';
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
  if (isStoreRegistration) return <Slot/>;
  if (member.role !== 'SELLER') return <Redirect href="/my"/>;

  return <Tabs screenListeners={({ route }) => ({ tabPress: () => triggerTabFeedback(route.name) })} screenOptions={({ route }) => {
    const item = tabs[route.name as keyof typeof tabs];
    return {
      headerShown: false,
      title: item?.[0],
      tabBarActiveTintColor: colors.ink900,
      tabBarInactiveTintColor: colors.ink400,
      tabBarStyle: { height: 76, paddingTop: 8, paddingBottom: 10, borderTopColor: colors.line, backgroundColor: colors.white, shadowColor: '#17281C', shadowOpacity: .055, shadowRadius: 12, shadowOffset: { width: 0, height: -3 } },
      tabBarLabelStyle: { fontSize: 11, fontWeight: '700', fontFamily: fonts.body },
      tabBarIcon: ({ color, focused }) => item ? <AnimatedTabIcon tabKey={route.name} active={item[2]} color={color} focused={focused} idle={item[1]} size={focused ? 21 : 20} /> : null,
    };
  }}>
    <Tabs.Screen name="home" />
    <Tabs.Screen name="dishes" />
    <Tabs.Screen name="orders" />
    <Tabs.Screen name="settlements" />
    <Tabs.Screen name="exit" listeners={{ tabPress: (event) => { event.preventDefault(); router.replace('/my'); } }} />
    <Tabs.Screen name="store" options={{ href: null }} />
    <Tabs.Screen name="dishes/new" options={{ href: null }} />
  </Tabs>;
}
