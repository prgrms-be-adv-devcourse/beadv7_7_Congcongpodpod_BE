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
  payload?: string;
  onPress?: () => void;
};

export type AppDishReportRequest = {
  id: number;
  level?: number;
  grade?: string;
  purchaseCount?: number;
  savedAmount?: number;
  earnedPoints?: number;
  remainToNextLevel?: number;
};

type AlertListener = (request: AppAlertRequest) => void;
type LoadingListener = (count: number) => void;
type NotificationListener = (request: AppNotificationRequest) => void;
type DishReportListener = (request: AppDishReportRequest) => void;

let alertId = 0;
let alertListener: AlertListener | undefined;
let loadingCount = 0;
let loadingListener: LoadingListener | undefined;
let notificationId = 0;
let notificationListener: NotificationListener | undefined;
let dishReportId = 0;
let dishReportListener: DishReportListener | undefined;

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

export function resetGlobalLoading() {
  loadingCount = 0;
  loadingListener?.(loadingCount);
}

export function subscribeGlobalLoading(listener: LoadingListener) {
  loadingListener = listener;
  listener(loadingCount);
  return () => { if (loadingListener === listener) loadingListener = undefined; };
}

export function showInAppNotification(title: string, message: string, onPress?: () => void, type?: string, payload?: string) {
  notificationListener?.({ id: ++notificationId, title, message, onPress, type, payload });
}

export function subscribeInAppNotifications(listener: NotificationListener) {
  notificationListener = listener;
  return () => { if (notificationListener === listener) notificationListener = undefined; };
}

export function showDishReport(report: Omit<AppDishReportRequest, 'id'> = {}) {
  dishReportListener?.({ id: ++dishReportId, ...report });
}

export function subscribeDishReports(listener: DishReportListener) {
  dishReportListener = listener;
  return () => { if (dishReportListener === listener) dishReportListener = undefined; };
}
