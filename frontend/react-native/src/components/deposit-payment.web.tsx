import { loadTossPayments, type TossPaymentsWidgets } from '@tosspayments/tosspayments-sdk';
import { router, useLocalSearchParams } from 'expo-router';
import { useEffect, useRef, useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { ConfirmModal } from '@/components/confirm-modal';
import { LoadingState } from '@/components/loading-state';
import { colors, fonts, radius } from '@/constants/theme';
import { approveDepositPayment } from '@/lib/account';
import { showAppAlert } from '@/lib/app-overlay';
import { useAuth } from '@/providers/auth-provider';

export default function DepositPaymentScreen() {
  const params = useLocalSearchParams<{ clientKey?: string; orderId?: string; amount?: string; result?: string; paymentKey?: string; message?: string }>();
  const { member } = useAuth();
  const widgets = useRef<TossPaymentsWidgets | undefined>(undefined);
  const [ready, setReady] = useState(false);
  const [processing, setProcessing] = useState(params.result === 'success');
  const [confirming, setConfirming] = useState(false);
  const amount = Number(params.amount ?? 0);

  useEffect(() => {
    if (params.result !== 'success' || !params.paymentKey || !params.orderId || !amount) return;
    void approveDepositPayment(params.paymentKey, params.orderId, amount)
      .then(() => router.replace('/deposits'))
      .catch(error => router.replace({ pathname: '/deposits/charge/fail', params: { message: error instanceof Error ? error.message : '결제를 승인하지 못했어요.', amount: String(amount) } }));
  }, [amount, params.orderId, params.paymentKey, params.result]);

  useEffect(() => {
    if (params.result === 'fail') {
      router.replace({ pathname: '/deposits/charge/fail', params: { message: params.message ?? '결제를 완료하지 못했어요.', amount: String(amount) } });
      return;
    }
    if (params.result || !params.clientKey || !amount) return;
    let active = true;
    void loadTossPayments(params.clientKey)
      .then(toss => toss.widgets({ customerKey: `lastdish-${crypto.randomUUID()}` }))
      .then(async instance => {
        await instance.setAmount({ currency: 'KRW', value: amount });
        await Promise.all([
          instance.renderPaymentMethods({ selector: '#lastdish-payment-methods', variantKey: 'DEFAULT' }),
          instance.renderAgreement({ selector: '#lastdish-payment-agreement', variantKey: 'DEFAULT' }),
        ]);
        if (!active) return;
        widgets.current = instance;
        setReady(true);
      })
      .catch(error => showAppAlert('결제창을 준비하지 못했어요', error instanceof Error ? error.message : '잠시 후 다시 시도해주세요.'));
    return () => { active = false; };
  }, [amount, params.clientKey, params.message, params.result]);

  const pay = async () => {
    if (!widgets.current || !params.orderId) return;
    setConfirming(false);
    setProcessing(true);
    try {
      const origin = window.location.origin;
      await widgets.current.requestPayment({
        orderId: params.orderId,
        orderName: 'LastDish 예치금 충전',
        customerEmail: member?.email,
        customerName: member?.name,
        customerMobilePhone: member?.phone?.replace(/\D/g, ''),
        successUrl: `${origin}/deposits/charge/payment?result=success`,
        failUrl: `${origin}/deposits/charge/payment?result=fail`,
      });
    } catch (error) {
      setProcessing(false);
      showAppAlert('결제를 시작하지 못했어요', error instanceof Error ? error.message : '잠시 후 다시 시도해주세요.');
    }
  };

  if (params.result === 'success') return <LoadingState label="결제를 승인하고 있어요" />;

  return <SafeAreaView style={styles.safe} edges={['top', 'bottom']}>
    <View style={styles.header}><Pressable accessibilityLabel="뒤로 가기" onPress={() => router.back()} style={styles.back}><Text style={styles.backText}>‹</Text></Pressable><Text style={styles.title}>결제수단 선택</Text><View style={styles.back}/></View>
    <ScrollView contentContainerStyle={styles.scroll}>
      <View style={styles.summary}><Text style={styles.summaryLabel}>충전 금액</Text><Text style={styles.summaryValue}>{amount.toLocaleString()}원</Text></View>
      <View nativeID="lastdish-payment-methods" style={styles.widget}/>
      <View nativeID="lastdish-payment-agreement" style={styles.widget}/>
    </ScrollView>
    <View style={styles.footer}><Pressable disabled={!ready || processing} onPress={() => setConfirming(true)} style={[styles.button, (!ready || processing) && styles.disabled]}><Text style={styles.buttonText}>{processing ? '결제 승인 중…' : ready ? `${amount.toLocaleString()}원 결제하기` : '결제창 준비 중…'}</Text></Pressable></View>
    <ConfirmModal visible={confirming} icon="card-outline" title="충전을 진행할까요?" description={`선택한 결제수단으로 ${amount.toLocaleString()}원을 결제합니다.`} confirmLabel={`${amount.toLocaleString()}원 결제`} busy={processing} busyLabel="결제창 여는 중…" onCancel={() => setConfirming(false)} onConfirm={() => void pay()}/>
  </SafeAreaView>;
}

const styles = StyleSheet.create({ safe: { flex: 1, width: '100%', maxWidth: 760, alignSelf: 'center', backgroundColor: colors.canvas }, header: { minHeight: 56, paddingHorizontal: 10, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }, back: { width: 44, height: 44, alignItems: 'center', justifyContent: 'center' }, backText: { color: colors.ink900, fontSize: 38, lineHeight: 40 }, title: { color: colors.ink900, fontFamily: fonts.body, fontSize: 18, fontWeight: '700' }, scroll: { paddingBottom: 24 }, summary: { marginHorizontal: 20, marginBottom: 8, padding: 16, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', borderRadius: radius.card, backgroundColor: colors.green100, borderWidth: 1, borderColor: colors.green300 }, summaryLabel: { color: colors.ink700, fontFamily: fonts.body, fontSize: 12 }, summaryValue: { color: colors.ink900, fontFamily: fonts.body, fontSize: 20, fontWeight: '700' }, widget: { minHeight: 80 }, footer: { padding: 12, backgroundColor: colors.white, borderTopWidth: 1, borderTopColor: colors.line }, button: { minHeight: 52, alignItems: 'center', justifyContent: 'center', borderRadius: radius.input, backgroundColor: colors.green300 }, disabled: { opacity: 0.5 }, buttonText: { color: colors.white, fontFamily: fonts.body, fontSize: 16, fontWeight: '800' } });
