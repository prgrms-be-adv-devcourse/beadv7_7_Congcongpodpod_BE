import { router, Tabs } from 'expo-router';

import { AnimatedTabIcon, triggerTabFeedback } from '@/components/animated-tab-icon';
import { FloatingTabBar } from '@/components/floating-tab-bar';
import { GlassTabBackground } from '@/components/glass-tab-background';
import { colors, fonts, radius } from '@/constants/theme';
import { showAppAlert } from '@/lib/app-overlay';
import { useAuth } from '@/providers/auth-provider';

const tabs = {
  index: ['홈', 'home-outline', 'home'] as const,
  stores: ['목록', 'list-outline', 'list'] as const,
  favorites: ['찜', 'heart-outline', 'heart'] as const,
  orders: ['주문내역', 'receipt-outline', 'receipt'] as const,
  my: ['마이', 'person-outline', 'person'] as const,
};

export default function TabLayout() {
  const { member, initializing } = useAuth();
  const requireLogin = (redirect: '/favorites' | '/orders') => ({
    tabPress: (event: { preventDefault: () => void }) => {
      if (initializing || member) return;
      event.preventDefault();
      router.push({ pathname: '/login', params: { redirect } });
      setTimeout(() => showAppAlert('로그인이 필요해요', '찜과 주문내역은 로그인 후 이용할 수 있어요.'), 120);
    },
  });

  return (
    <Tabs
      tabBar={(props) => <FloatingTabBar {...props}/>}
      screenListeners={({ route }) => ({ tabPress: () => triggerTabFeedback(route.name) })}
      screenOptions={({ route }) => {
        const [title, idle, active] = tabs[route.name as keyof typeof tabs];
        return {
          title,
          headerShown: false,
          tabBarActiveTintColor: colors.ink900,
          tabBarInactiveTintColor: colors.ink400,
          tabBarStyle: {
            height: 64,
            paddingTop: 6,
            paddingBottom: 6,
            borderTopWidth: 0,
            borderRadius: radius.navigation,
            backgroundColor: 'transparent',
          },
          tabBarBackground: () => <GlassTabBackground/>,
          tabBarLabelStyle: { fontSize: 10, fontWeight: '700', fontFamily: fonts.body },
          tabBarIcon: ({ color, focused }) => (
            <AnimatedTabIcon tabKey={route.name} active={active} color={color} focused={focused} idle={idle} size={focused ? 22 : 21} />
          ),
        };
      }}
    >
      <Tabs.Screen name="index" />
      <Tabs.Screen name="stores" />
      <Tabs.Screen name="favorites" listeners={requireLogin('/favorites')} />
      <Tabs.Screen name="orders" listeners={requireLogin('/orders')} />
      <Tabs.Screen name="my" />
    </Tabs>
  );
}
