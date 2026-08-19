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

type AlertListener = (request: AppAlertRequest) => void;
type LoadingListener = (count: number) => void;

let alertId = 0;
let alertListener: AlertListener | undefined;
let loadingCount = 0;
let loadingListener: LoadingListener | undefined;

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
