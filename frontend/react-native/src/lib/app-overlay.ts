export type AppAlertButton = {
  text?: string;
  style?: 'default' | 'cancel' | 'destructive';
  onPress?: () => void;
};

export type AppAlertRequest = {
  id: number;
  title: string;
  message?: string;
  buttons: AppAlertButton[];
};

export type AppNotificationRequest = {
  id: number;
  title: string;
  message: string;
  type?: string;
  onPress?: () => void;
};

type AlertListener = (request: AppAlertRequest) => void;
type LoadingListener = (count: number) => void;
type NotificationListener = (request: AppNotificationRequest) => void;

let alertId = 0;
let alertListener: AlertListener | undefined;
let loadingCount = 0;
let loadingListener: LoadingListener | undefined;
let notificationId = 0;
let notificationListener: NotificationListener | undefined;

export function showAppAlert(title: string, message?: string, buttons: AppAlertButton[] = [{ text: '확인' }]) {
  alertListener?.({ id: ++alertId, title, message, buttons: buttons.length ? buttons : [{ text: '확인' }] });
}

export function subscribeAppAlerts(listener: AlertListener) {
  alertListener = listener;
  return () => { if (alertListener === listener) alertListener = undefined; };
}

export function beginGlobalLoading() {
  loadingCount += 1;
  loadingListener?.(loadingCount);
}

export function endGlobalLoading() {
  loadingCount = Math.max(0, loadingCount - 1);
  loadingListener?.(loadingCount);
}

export function subscribeGlobalLoading(listener: LoadingListener) {
  loadingListener = listener;
  listener(loadingCount);
  return () => { if (loadingListener === listener) loadingListener = undefined; };
}

export function showInAppNotification(title: string, message: string, onPress?: () => void, type?: string) {
  notificationListener?.({ id: ++notificationId, title, message, onPress, type });
}

export function subscribeInAppNotifications(listener: NotificationListener) {
  notificationListener = listener;
  return () => { if (notificationListener === listener) notificationListener = undefined; };
}
