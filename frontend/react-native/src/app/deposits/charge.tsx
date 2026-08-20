import { router } from 'expo-router';
import { useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { Page, Panel, PrimaryButton } from '@/components/page';
import { showAppAlert } from '@/lib/app-overlay';
import { colors, fonts, radius } from '@/constants/theme';
import { readyDepositPayment } from '@/lib/account';

export default function DepositCharge() {
  const [amount, setAmount] = useState(30000); const [loading, setLoading] = useState(false);
  const start = async () => { try { setLoading(true); const ready = await readyDepositPayment(amount); router.push({ pathname: '/deposits/charge/payment', params: { clientKey: ready.tossClientKey, orderId: ready.merchantOrderId, amount: String(ready.amount) } }); } catch (error) { showAppAlert('결제를 시작하지 못했어요', error instanceof Error ? error.message : '잠시 후 다시 시도해주세요.'); } finally { setLoading(false); } };
  return <Page title="예치금 충전" description="충전할 금액을 선택하세요."><Panel><View style={styles.options}>{[10000, 30000, 50000].map((x) => <Pressable key={x} onPress={() => setAmount(x)} style={[styles.option, x === amount && styles.selected]}><Text style={[styles.optionText, x === amount && styles.selectedText]}>{x.toLocaleString()}원</Text>{x === amount && <Text style={styles.check}>선택됨</Text>}</Pressable>)}</View></Panel><Text style={styles.notice}>토스페이먼츠 결제창에서 결제수단을 선택하고 승인하면 예치금에 즉시 반영됩니다.</Text><PrimaryButton disabled={loading} label={loading ? '결제 준비 중…' : `${amount.toLocaleString()}원 결제`} onPress={() => void start()} /></Page>;
}
const styles = StyleSheet.create({ options: { gap: 9 }, option: { minHeight: 58, paddingHorizontal: 16, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', borderWidth: 1, borderColor: colors.line, borderRadius: radius.input }, selected: { backgroundColor: colors.green100, borderColor: colors.green300 }, optionText: { color: colors.ink700, fontFamily: fonts.body, fontWeight: '700' }, selectedText: { color: colors.green700 }, check: { color: colors.green700, fontFamily: fonts.body, fontSize: 11, fontWeight: '700' }, notice: { color: colors.ink700, fontFamily: fonts.body, fontSize: 12, lineHeight: 18 } });
