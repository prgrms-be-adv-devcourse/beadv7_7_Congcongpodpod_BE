import { api, getAccessToken, getApiBaseUrl } from './api';

type Envelope<T> = { data?: T };
const unwrap = <T,>(value: T | Envelope<T>): T => value && typeof value === 'object' && 'data' in value && value.data !== undefined ? value.data : value as T;

export type ServerNotification = {
  id: number;
  type: string;
  title: string;
  body: string;
  data?: string | null;
  linkTarget?: string | null;
  linkId?: number | null;
  readYn: boolean;
  createdAt: string;
};

export type NotificationPage = { items: ServerNotification[]; total: number; page: number; size: number; hasNext: boolean };

export async function getNotifications(page = 0, size = 20) {
  return unwrap(await api<NotificationPage | Envelope<NotificationPage>>(`/notifications?page=${page}&size=${size}`));
}

export async function getUnreadNotificationCount() {
  return unwrap(await api<{ count: number } | Envelope<{ count: number }>>('/notifications/unread-count')).count;
}

export async function markNotificationRead(id: number) {
  await api(`/notifications/${id}/read`, { method: 'PATCH' });
}

export async function markAllNotificationsRead() {
  await api('/notifications/read-all', { method: 'PATCH' });
}

function parseSseFrames(chunk: string, emit: (notification: ServerNotification) => void) {
  for (const frame of chunk.split(/\r?\n\r?\n/)) {
    const event = frame.split(/\r?\n/).find(line => line.startsWith('event:'))?.slice(6).trim();
    const data = frame.split(/\r?\n/).filter(line => line.startsWith('data:')).map(line => line.slice(5).trim()).join('\n');
    if (event !== 'notification' || !data) continue;
    try { emit(JSON.parse(data) as ServerNotification); } catch { /* 잘못된 한 이벤트가 연결을 종료하지 않게 한다. */ }
  }
}

export function connectNotificationStream(
  onNotification: (notification: ServerNotification) => void,
  onDisconnected: () => void,
) {
  let closed = false;
  let request: XMLHttpRequest | undefined;
  let consumed = 0;
  let disconnectReported = false;
  const reportDisconnected = () => {
    if (closed || disconnectReported) return;
    disconnectReported = true;
    onDisconnected();
  };

  void getAccessToken().then(token => {
    if (closed || !token) return reportDisconnected();
    request = new XMLHttpRequest();
    request.open('GET', `${getApiBaseUrl()}/notifications/stream`, true);
    request.setRequestHeader('Accept', 'text/event-stream');
    request.setRequestHeader('Authorization', `Bearer ${token}`);
    request.onprogress = () => {
      const received = request?.responseText ?? '';
      const completeEnd = Math.max(received.lastIndexOf('\n\n'), received.lastIndexOf('\r\n\r\n'));
      if (completeEnd < consumed) return;
      const end = completeEnd + (received.startsWith('\r\n', completeEnd) ? 4 : 2);
      parseSseFrames(received.slice(consumed, end), onNotification);
      consumed = end;
    };
    request.onerror = reportDisconnected;
    request.onloadend = reportDisconnected;
    request.send();
  }).catch(reportDisconnected);

  return () => {
    closed = true;
    request?.abort();
  };
}

export function notificationRoute(notification: Pick<ServerNotification, 'linkTarget' | 'linkId'>) {
  const id = notification.linkId;
  switch (notification.linkTarget?.toUpperCase()) {
    case 'ORDER': return id ? `/orders/${id}` : '/orders';
    case 'STORE': return id ? `/stores/${id}` : '/stores';
    case 'DEPOSIT': return '/deposits';
    default: return '/notifications';
  }
}
