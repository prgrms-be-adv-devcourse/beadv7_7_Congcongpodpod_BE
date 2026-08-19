import { api } from './api';

type Envelope<T> = { data?: T };
type Page<T> = { content?: T[]; number?: number; totalPages?: number; totalElements?: number };
export type DepositHistory = { id: number; orderId?: number; paymentId?: number; type: 'CHARGE' | 'USE' | 'REFUND'; amount: number; balanceAfter: number; createdAt: string };

const unwrap = <T,>(value: T | Envelope<T>): T => value && typeof value === 'object' && 'data' in value && value.data !== undefined ? value.data : value as T;

export async function getDepositBalance() {
  const value = unwrap(await api<{ balance: number } | Envelope<{ balance: number }>>('/deposits/balance'));
  return Number(value.balance ?? 0);
}

export async function getDepositHistory(page = 0, size = 7) {
  const value = unwrap(await api<Page<DepositHistory> | Envelope<Page<DepositHistory>>>(`/deposits/history?page=${page}&size=${size}`));
  return { content: value.content ?? [], page: Number(value.number ?? page), totalPages: Number(value.totalPages ?? 0), totalElements: Number(value.totalElements ?? value.content?.length ?? 0) };
}

export async function updateMyProfile(input: { userName: string; name: string; phone: string; email: string; password?: string }) {
  await api('/members/me', { method: 'PUT', body: JSON.stringify(input) });
}

export type PaymentReady = { paymentId: number; merchantOrderId: string; amount: number; approvedStatus: string; tossClientKey: string };
export type PaymentApprove = { paymentId: number; merchantOrderId: string; amount: number; approvedStatus: string; approvedAt: string; depositBalance: number };

export async function readyDepositPayment(amount: number) {
  return api<PaymentReady>('/payments', { method: 'POST', body: JSON.stringify({ amount, pgProvider: 'TOSS' }) });
}

export async function approveDepositPayment(paymentKey: string, orderId: string, amount: number) {
  return api<PaymentApprove>('/payments/approve', { method: 'POST', body: JSON.stringify({ paymentKey, orderId, amount }) });
}
