import { router } from 'expo-router';
import { useEffect, useState } from 'react';
import { StyleSheet, Text, TextInput, View } from 'react-native';

import { Page, PrimaryButton } from '@/components/page';
import { ConfirmModal } from '@/components/confirm-modal';
import { LoadingState } from '@/components/loading-state';
import { showAppAlert } from '@/lib/app-overlay';
import { colors, fonts, radius } from '@/constants/theme';
import { getDepositBalance } from '@/lib/account';
import { createOrderFromCartItem, getMemberCart } from '@/lib/cart';
import { useAuth } from '@/providers/auth-provider';
import { useCart } from '@/providers/cart-provider';

export default function Checkout() {
  const { member } = useAuth(); const { item, loading, refresh, resetLocal } = useCart(); const [balance,setBalance]=useState<number|null>(null); const [paying,setPaying]=useState(false); const [confirming,setConfirming]=useState(false);
  const total = item ? item.discountPrice * item.cartQuantity : 0;
  useEffect(()=>{if(!member)return;void getDepositBalance().then(setBalance).catch(()=>setBalance(null))},[member]);
  if (!member) return <Page title="로그인이 필요해요" description="주문과 결제는 로그인 후 이용할 수 있어요."><PrimaryButton label="로그인하기" onPress={() => router.push({ pathname: '/login', params: { redirect: '/cart/checkout' } })} /></Page>;
  if (loading && !item) return <Page title="주문 확인" description="장바구니와 결제 정보를 준비하고 있어요."><LoadingState label="결제 정보를 확인하고 있어요" /></Page>;
  if (!item) return <Page title="주문 확인" description="장바구니에 담긴 상품이 없어요."><PrimaryButton label="주변 상품 둘러보기" onPress={() => router.replace('/stores')} /></Page>;
  const requestPayment=()=>{if(balance===null)return showAppAlert('잔액을 확인하지 못했어요','잠시 후 다시 시도해주세요.');if(balance<total)return showAppAlert('예치금이 부족해요',`${(total-balance).toLocaleString()}원을 더 충전해주세요.`,[{text:'취소',style:'cancel'},{text:'충전하기',onPress:()=>router.push('/deposits/charge')}]);setConfirming(true)};
  const pay=async()=>{if(!member){router.push('/login');return}try{setPaying(true);const cart=await getMemberCart();const serverItem=cart.items.find((candidate)=>candidate.cartItemId===item.cartItemId);if(!serverItem)throw new Error('서버 장바구니에 상품이 없어요. 장바구니를 다시 확인해주세요.');if(serverItem.dishId!==item.dishId||Number(serverItem.quantity)!==item.cartQuantity){await refresh();throw new Error('장바구니 내용이 변경됐어요. 수량을 다시 확인해주세요.');}if(Number(serverItem.subtotalPrice)!==total){await refresh();throw new Error('상품 가격이 변경됐어요. 결제 금액을 다시 확인해주세요.');}if(!serverItem.orderable)throw new Error('현재 주문할 수 없는 상품이에요.');await createOrderFromCartItem(serverItem.cartItemId);setConfirming(false);resetLocal();router.replace('/orders');void refresh().catch(()=>undefined)}catch(error){setConfirming(false);showAppAlert('주문하지 못했어요',error instanceof Error?error.message:'잠시 후 다시 시도해주세요.');void getDepositBalance().then(setBalance).catch(()=>undefined)}finally{setPaying(false)}};
  return <Page title="주문 확인" description="상품과 픽업 정보를 마지막으로 확인하세요.">
    <View style={styles.section}><Text style={styles.sectionTitle}>{item.storeName}</Text><Text style={styles.line}>{item.dishName} × {item.cartQuantity}</Text><Text style={styles.meta}>픽업 · 오늘 20:00–21:00</Text></View>
    <View style={styles.section}><Text style={styles.sectionTitle}>가게 요청사항</Text><TextInput placeholder="예: 도착하면 연락 부탁드려요." placeholderTextColor={colors.ink400} multiline style={styles.request} /></View>
    <View style={styles.section}><Text style={styles.sectionTitle}>결제 수단</Text><View style={styles.row}><Text style={styles.line}>예치금</Text><Text style={styles.value}>{balance===null ? '잔액 확인 중' : `${balance.toLocaleString()}원 보유`}</Text></View></View>
    <View style={styles.receipt}><Text style={styles.sectionTitle}>결제 내역</Text><ReceiptRow label="상품 금액" value={`${total.toLocaleString()}원`} /><ReceiptRow label="할인" value="0원" /><ReceiptRow label="예치금 사용" value={`−${total.toLocaleString()}원`} /><View style={styles.total}><Text style={styles.totalLabel}>총 결제 금액</Text><Text style={styles.totalValue}>{total.toLocaleString()}원</Text></View><ReceiptRow label="결제 후 예치금" value={balance===null?'—':`${Math.max(0,balance-total).toLocaleString()}원`} /></View>
    <PrimaryButton disabled={paying||balance===null} label={paying?'주문 처리 중…':`${total.toLocaleString()}원 결제`} onPress={requestPayment} />
    <ConfirmModal visible={confirming} icon="wallet-outline" title="결제를 진행할까요?" description={`${item.storeName} · ${item.dishName} ${item.cartQuantity}개\n예치금 ${total.toLocaleString()}원이 사용됩니다.`} confirmLabel={`${total.toLocaleString()}원 결제`} busy={paying} busyLabel="결제 처리 중…" onCancel={()=>setConfirming(false)} onConfirm={()=>void pay()}/>
  </Page>;
}
function ReceiptRow({ label, value }: { label: string; value: string }) { return <View style={styles.row}><Text style={styles.meta}>{label}</Text><Text style={styles.line}>{value}</Text></View>; }
const styles = StyleSheet.create({ section: { paddingVertical: 14, gap: 8, borderBottomWidth: 1, borderBottomColor: colors.line }, sectionTitle: { color: colors.ink900, fontFamily: fonts.body, fontSize: 16, fontWeight: '700' }, line: { color: colors.ink900, fontFamily: fonts.body, fontSize: 13 }, meta: { color: colors.ink700, fontFamily: fonts.body, fontSize: 12 }, value: { color: colors.ink900, fontFamily: fonts.body, fontSize: 13, fontWeight: '700' }, request: { minHeight: 72, padding: 12, color: colors.ink900, fontFamily: fonts.body, fontSize: 13, textAlignVertical: 'top', backgroundColor: colors.white, borderWidth: 1, borderColor: colors.line, borderRadius: radius.input }, receipt: { paddingVertical: 14, gap: 8, borderBottomWidth: 1, borderBottomColor: colors.line, borderStyle: 'dashed' }, row: { minHeight: 32, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 12 }, total: { minHeight: 50, marginTop: 4, paddingTop: 12, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', borderTopWidth: 1, borderTopColor: colors.line }, totalLabel: { color: colors.ink900, fontFamily: fonts.body, fontSize: 16, fontWeight: '700' }, totalValue: { color: colors.ink900, fontFamily: fonts.body, fontSize: 18, fontWeight: '700' } });
