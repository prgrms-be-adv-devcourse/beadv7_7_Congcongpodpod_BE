import { useFocusEffect } from 'expo-router';
import { useCallback, useEffect, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { ConfirmModal } from '@/components/confirm-modal';
import { showAppAlert } from '@/lib/app-overlay';
import { EmptyState } from '@/components/empty-state';
import { LoadingState } from '@/components/loading-state';
import { SellerShell } from '@/components/seller-shell';
import { colors, fonts, radius } from '@/constants/theme';
import { acceptStoreOrder, getMyStores, getStoreOrders, rejectStoreOrder, updateStorePickup, type SellerOrder } from '@/lib/seller';
import { usePullToRefresh } from '@/hooks/use-pull-to-refresh';

const tabs = [['RESERVED','접수 대기'],['PICKUP_READY','픽업 대기'],['PICKED_UP','완료'],['CLOSED','종료']] as const;
type Tab=(typeof tabs)[number][0];
type PendingAction={order:SellerOrder;status:'ACCEPT'|'PICKED_UP'|'NO_SHOW'};
const reasons=[['OUT_OF_STOCK','재고 소진'],['QUALITY_ISSUE','상품 품질 문제'],['NOT_READY','준비 어려움'],['STORE_CLOSED','영업 종료']] as const;

export default function SellerOrders(){
  const [tab,setTab]=useState<Tab>('RESERVED'); const [storeId,setStoreId]=useState<number>(); const [storeName,setStoreName]=useState('미등록'); const [orders,setOrders]=useState<SellerOrder[]>([]); const [codes,setCodes]=useState<Record<number,string>>({}); const [loading,setLoading]=useState(true); const [processing,setProcessing]=useState<number>(); const [rejecting,setRejecting]=useState<number>(); const [pendingAction,setPendingAction]=useState<PendingAction>();
  useEffect(()=>{void getMyStores().then(([store])=>{if(!store)return;setStoreId(store.storeId);setStoreName(store.storeName)}).finally(()=>setLoading(false))},[]);
  const load=useCallback(async()=>{if(!storeId)return;setLoading(true);try{const rows=await getStoreOrders(storeId,tab==='CLOSED'?undefined:tab);setOrders(tab==='CLOSED'?rows.filter(x=>['CANCELLED','REJECTED','NO_SHOW'].includes(x.status)):rows)}catch(error){setOrders([]);showAppAlert('주문을 불러오지 못했어요',error instanceof Error?error.message:'잠시 후 다시 시도해주세요.')}finally{setLoading(false)}},[storeId,tab]);
  useFocusEffect(useCallback(()=>{void load()},[load]));
  const {refreshing,onRefresh}=usePullToRefresh(load);
  const accept=async(order:SellerOrder)=>{try{setProcessing(order.orderId);const result=await acceptStoreOrder(order.orderId);setCodes(current=>({...current,[order.orderId]:result.pickUpCode}));setOrders(current=>current.filter(x=>x.orderId!==order.orderId));setPendingAction(undefined);setTab('PICKUP_READY')}catch(error){showAppAlert('접수하지 못했어요',error instanceof Error?error.message:'잠시 후 다시 시도해주세요.')}finally{setProcessing(undefined)}};
  const reject=async(order:SellerOrder,reason:(typeof reasons)[number][0])=>{try{setProcessing(order.orderId);await rejectStoreOrder(order.orderId,reason);setRejecting(undefined);setOrders(current=>current.filter(x=>x.orderId!==order.orderId))}catch(error){showAppAlert('거절하지 못했어요',error instanceof Error?error.message:'잠시 후 다시 시도해주세요.')}finally{setProcessing(undefined)}};
  const pickup=async(order:SellerOrder,status:'PICKED_UP'|'NO_SHOW')=>{try{setProcessing(order.orderId);await updateStorePickup(order.orderId,status);setOrders(current=>current.filter(x=>x.orderId!==order.orderId));setPendingAction(undefined)}catch(error){showAppAlert('처리하지 못했어요',error instanceof Error?error.message:'잠시 후 다시 시도해주세요.')}finally{setProcessing(undefined)}};
  const confirmAction=()=>{if(!pendingAction)return;if(pendingAction.status==='ACCEPT')void accept(pendingAction.order);else void pickup(pendingAction.order,pendingAction.status)};
  const confirmCopy=pendingAction?.status==='ACCEPT'?{icon:'receipt-outline' as const,title:'주문을 접수할까요?',description:`${pendingAction.order.dishName} ${pendingAction.order.quantity}개를 준비합니다.\n접수하면 고객에게 픽업 코드가 발급돼요.`,label:'주문 접수',busy:'접수 중…'}:pendingAction?.status==='PICKED_UP'?{icon:'checkmark-circle-outline' as const,title:'픽업을 완료할까요?',description:`주문 #${pendingAction.order.orderId}의 고객 코드를 확인하셨나요?\n완료 후에는 상태를 되돌릴 수 없어요.`,label:'픽업 완료',busy:'완료 처리 중…'}:{icon:'alert-circle-outline' as const,title:'미수령 처리할까요?',description:`주문 #${pendingAction?.order.orderId??''}을 노쇼로 처리합니다.\n고객이 방문하지 않은 경우에만 진행해주세요.`,label:'미수령 처리',busy:'미수령 처리 중…'};
  return <><SellerShell title="주문 관리" description="접수 후 발급된 코드를 확인하고 픽업을 완료하세요." storeName={storeName} refreshing={refreshing} onRefresh={onRefresh}><View style={styles.tabs}>{tabs.map(([key,label])=><Pressable accessibilityRole="tab" accessibilityState={{ selected: tab === key }} key={key} onPress={()=>setTab(key)} style={styles.tab}><Text style={[styles.tabText,tab===key&&styles.active]}>{label}</Text>{tab===key?<View style={styles.tabLine}/>:null}</Pressable>)}</View>{loading&&!orders.length?<LoadingState label="매장 주문을 확인하고 있어요" compact/>:orders.length?orders.map(order=><OrderRow key={order.orderId} order={order} code={codes[order.orderId]} busy={processing===order.orderId} rejecting={rejecting===order.orderId} onAccept={()=>setPendingAction({order,status:'ACCEPT'})} onRejectOpen={()=>setRejecting(current=>current===order.orderId?undefined:order.orderId)} onReject={reason=>void reject(order,reason)} onPickup={status=>setPendingAction({order,status})}/>):<EmptyState title="이 상태의 주문이 없어요" description="새 주문이 들어오면 바로 여기에 표시돼요."/>}</SellerShell><ConfirmModal visible={Boolean(pendingAction)} icon={confirmCopy.icon} title={confirmCopy.title} description={confirmCopy.description} confirmLabel={confirmCopy.label} busy={Boolean(processing)} busyLabel={confirmCopy.busy} onCancel={()=>{if(!processing)setPendingAction(undefined)}} onConfirm={confirmAction}/></>;
}

function OrderRow({order,code,busy,rejecting,onAccept,onRejectOpen,onReject,onPickup}:{order:SellerOrder;code?:string;busy:boolean;rejecting:boolean;onAccept:()=>void;onRejectOpen:()=>void;onReject:(reason:(typeof reasons)[number][0])=>void;onPickup:(status:'PICKED_UP'|'NO_SHOW')=>void}){
  return <View style={styles.order}><View style={styles.head}><View><Text style={styles.id}>주문 #{order.orderId}</Text><Text style={styles.state}>{order.status==='RESERVED'?'접수 대기':order.status==='PICKUP_READY'?'픽업 대기':order.status==='PICKED_UP'?'픽업 완료':order.status==='NO_SHOW'?'미수령':order.status==='REJECTED'?'거절':'취소'}</Text></View><Text style={styles.pickup}>{order.pickupStartAt?.slice(0,5)??'--:--'}–{order.pickupEndAt?.slice(0,5)??'--:--'}</Text></View><Text style={styles.menu}>{order.dishName} × {order.quantity}</Text><Text style={styles.customer}>{order.memberName??'고객'} · {order.phone??'연락처 없음'}</Text>{order.status==='PICKUP_READY'?<View style={styles.codeBox}><View><Text style={styles.codeLabel}>고객에게 확인할 픽업 코드</Text><Text style={styles.code}>{code??order.pickupCode??'고객 화면에서 확인'}</Text></View><Text style={styles.price}>{Number(order.totalPrice).toLocaleString()}원</Text></View>:<Text style={styles.priceLine}>{Number(order.totalPrice).toLocaleString()}원</Text>}{order.status==='RESERVED'?<><View style={styles.actions}><Pressable disabled={busy} onPress={onRejectOpen} style={styles.secondary}><Text style={styles.secondaryText}>주문 거절</Text></Pressable><Pressable disabled={busy} onPress={onAccept} style={styles.action}><Text style={styles.actionText}>{busy?'처리 중…':'주문 접수'}</Text></Pressable></View>{rejecting?<View style={styles.reasons}><Text style={styles.reasonTitle}>거절 사유를 선택하세요</Text><View style={styles.reasonGrid}>{reasons.map(([key,label])=><Pressable key={key} disabled={busy} onPress={()=>onReject(key)} style={styles.reason}><Text style={styles.reasonText}>{label}</Text></Pressable>)}</View></View>:null}</>:order.status==='PICKUP_READY'?<View style={styles.actions}><Pressable disabled={busy} onPress={()=>onPickup('NO_SHOW')} style={styles.secondary}><Text style={styles.secondaryText}>미수령 처리</Text></Pressable><Pressable disabled={busy} onPress={()=>onPickup('PICKED_UP')} style={styles.action}><Text style={styles.actionText}>{busy?'처리 중…':'코드 확인 · 픽업 완료'}</Text></Pressable></View>:null}</View>;
}
const styles = StyleSheet.create({
  tabs: { flexDirection: 'row', borderBottomWidth: 1, borderBottomColor: colors.line },
  tab: { flex: 1, minHeight: 48, alignItems: 'center', justifyContent: 'center' },
  tabText: { color: colors.ink400, fontFamily: fonts.body, fontSize: 12, fontWeight: '700' },
  active: { color: colors.ink900, fontWeight: '900' },
  tabLine: { position: 'absolute', left: '50%', bottom: 0, width: 48, height: 3, marginLeft: -24, borderRadius: 2, backgroundColor: colors.ink900 },
  order: { marginTop: 10, padding: 15, borderRadius: radius.card, borderWidth: 1, borderColor: colors.line, backgroundColor: colors.white },
  head: { flexDirection: 'row', alignItems: 'flex-start', justifyContent: 'space-between' },
  id: { color: colors.ink900, fontFamily: fonts.body, fontSize: 15, fontWeight: '800' },
  state: { marginTop: 4, color: colors.green700, fontFamily: fonts.body, fontSize: 11, fontWeight: '700' },
  pickup: { color: colors.ink700, fontFamily: fonts.body, fontSize: 12 },
  menu: { marginTop: 13, color: colors.ink900, fontFamily: fonts.body, fontSize: 15, fontWeight: '700' },
  customer: { marginTop: 5, color: colors.ink700, fontFamily: fonts.body, fontSize: 12 },
  priceLine: { marginTop: 13, color: colors.ink900, fontFamily: fonts.body, fontWeight: '700' },
  codeBox: { marginTop: 13, padding: 12, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', borderRadius: radius.control, backgroundColor: colors.blue50, borderWidth: 1, borderColor: colors.blue300 },
  codeLabel: { color: colors.ink700, fontFamily: fonts.body, fontSize: 10 },
  code: { marginTop: 4, color: colors.green700, fontFamily: fonts.body, fontSize: 17, fontWeight: '700', letterSpacing: .8 },
  price: { color: colors.ink900, fontFamily: fonts.body, fontWeight: '700' },
  actions: { marginTop: 12, flexDirection: 'row', gap: 8 },
  secondary: { flex: 1, minHeight: 44, alignItems: 'center', justifyContent: 'center', borderRadius: radius.control, borderWidth: 1, borderColor: colors.line, backgroundColor: colors.white },
  secondaryText: { color: colors.ink700, fontFamily: fonts.body, fontSize: 11, fontWeight: '700' },
  action: { flex: 1.4, minHeight: 44, alignItems: 'center', justifyContent: 'center', borderRadius: radius.control, backgroundColor: colors.green300 },
  actionText: { color: colors.white, fontFamily: fonts.body, fontSize: 11, fontWeight: '800' },
  reasons: { marginTop: 10, padding: 11, borderRadius: radius.control, backgroundColor: colors.canvas },
  reasonTitle: { marginBottom: 8, color: colors.ink700, fontFamily: fonts.body, fontSize: 10 },
  reasonGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 6 },
  reason: { minHeight: 36, paddingHorizontal: 10, alignItems: 'center', justifyContent: 'center', borderRadius: 7, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.line },
  reasonText: { color: colors.ink900, fontFamily: fonts.body, fontSize: 10, fontWeight: '600' },
  empty: { paddingVertical: 44, textAlign: 'center', color: colors.ink400, fontFamily: fonts.body },
});
