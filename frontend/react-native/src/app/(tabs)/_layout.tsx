import { Tabs } from 'expo-router';

import { AnimatedTabIcon, triggerTabFeedback } from '@/components/animated-tab-icon';
import { colors, fonts } from '@/constants/theme';
import { showLoginRequired } from '@/lib/login-required';
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
      showLoginRequired(redirect);
    },
  });

  return (
    <Tabs
      screenListeners={({ route }) => ({ tabPress: () => triggerTabFeedback(route.name) })}
      screenOptions={({ route }) => {
        const [title, idle, active] = tabs[route.name as keyof typeof tabs];
        return {
          title,
          headerShown: false,
          tabBarActiveTintColor: colors.ink900,
          tabBarInactiveTintColor: colors.ink400,
          tabBarStyle: {
            height: 78,
            paddingTop: 8,
            paddingBottom: 12,
            borderTopColor: colors.line,
            backgroundColor: colors.white,
            shadowColor: '#17281C',
            shadowOpacity: 0.055,
            shadowRadius: 12,
            shadowOffset: { width: 0, height: -3 },
          },
          tabBarLabelStyle: { fontSize: 11, fontWeight: '700', fontFamily: fonts.body },
          tabBarIcon: ({ color, focused }) => (
            <AnimatedTabIcon tabKey={route.name} active={active} color={color} focused={focused} idle={idle} size={focused ? 23 : 22} />
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
