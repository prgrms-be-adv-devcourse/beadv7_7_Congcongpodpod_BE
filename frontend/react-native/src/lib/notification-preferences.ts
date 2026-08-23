import { storage } from '@/lib/storage';

export type NotificationPreferences = {
  enabled: boolean;
  orders: boolean;
  pickup: boolean;
  benefits: boolean;
};

export const DEFAULT_NOTIFICATION_PREFERENCES: NotificationPreferences = {
  enabled: true,
  orders: true,
  pickup: true,
  benefits: true,
};

const STORAGE_KEY = 'notificationPreferences';
let current = DEFAULT_NOTIFICATION_PREFERENCES;
const listeners = new Set<(preferences: NotificationPreferences) => void>();

function publish(preferences: NotificationPreferences) {
  current = preferences;
  listeners.forEach(listener => listener(preferences));
}

export async function loadNotificationPreferences() {
  try {
    const saved = await storage.getItem(STORAGE_KEY);
    if (saved) publish({ ...DEFAULT_NOTIFICATION_PREFERENCES, ...JSON.parse(saved) });
  } catch {
    publish(DEFAULT_NOTIFICATION_PREFERENCES);
  }
  return current;
}

export async function saveNotificationPreferences(preferences: NotificationPreferences) {
  publish(preferences);
  await storage.setItem(STORAGE_KEY, JSON.stringify(preferences));
}

export function subscribeNotificationPreferences(listener: (preferences: NotificationPreferences) => void) {
  listeners.add(listener);
  listener(current);
  return () => { listeners.delete(listener); };
}

export function canShowNotification(preferences: NotificationPreferences, type?: string) {
  if (!preferences.enabled) return false;
  const normalized = type?.toUpperCase() ?? '';
  if (normalized.includes('PICKUP') || normalized === 'PICKED_UP') return preferences.pickup;
  if (normalized.includes('ORDER')) return preferences.orders;
  return preferences.benefits;
}
