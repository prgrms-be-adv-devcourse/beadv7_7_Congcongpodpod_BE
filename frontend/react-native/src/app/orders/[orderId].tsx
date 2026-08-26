import { RoundedIcon as Ionicons } from '@/components/rounded-icon';
import { router, useLocalSearchParams } from 'expo-router';
import { useCallback, useEffect, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { OptimizedImage as Image } from '@/components/optimized-image';
import { LoadingState } from '@/components/loading-state';
import { showAppAlert } from '@/lib/app-overlay';
import { Page } from '@/components/page';
import { colors, fonts, radius } from '@/constants/theme';
import { cancelOrder, getOrder, getPickupCode, type CustomerOrder } from '@/lib/orders';
import { subscribeOrderStateChanged } from '@/lib/order-events';
import { usePullToRefresh } from '@/hooks/use-pull-to-refresh';

const steps=['주문 접수','픽업 대기','픽업 완료'];
const stepOf=(status:string)=>status==='PICKED_UP'?2:status==='PICKUP_READY'?1:0;
const statusTitle=(status:string)=>status==='PICKED_UP'?'픽업이 완료됐어요':status==='PICKUP_READY'?'상품이 준비됐어요':status==='CANCELLED'?'취소된 주문이에요':status==='REJECTED'?'매장에서 주문을 거절했어요':'매장에서 주문을 확인하고 있어요';

export default function OrderDetail(){
  const {orderId}=useLocalSearchParams<{orderId:string}>(); const id=Number(orderId); const [order,setOrder]=useState<CustomerOrder|null>(null); const [code,setCode]=useState(''); const [failed,setFailed]=useState(false);
  const closeToOrders=()=>router.replace('/orders');
  const load=useCallback(async()=>{setFailed(false);try{const row=await getOrder(id);setOrder(row);if(row.status==='PICKUP_READY'){try{setCode((await getPickupCode(id)).pickupCode);}catch{setCode('');}}else setCode('');}catch{setFailed(true)}},[id]);
  useEffect(()=>{void load()},[load]);
  useEffect(()=>subscribeOrderStateChanged((event)=>{if(!event.orderId||event.orderId===id)void load()}),[id,load]);
  const {refreshing,onRefresh}=usePullToRefresh(load);
  if(failed&&!order)return <Page title="주문 상세" description="주문 정보를 불러오지 못했어요." refreshing={refreshing} onRefresh={onRefresh} onClose={closeToOrders} closeLabel="주문내역으로 닫기"/>;
  if(!order)return <Page title="주문 상세" onClose={closeToOrders} closeLabel="주문내역으로 닫기"><LoadingState label="주문 상태를 확인하고 있어요" compact/></Page>;
  const current=stepOf(order.status); const cancellable=order.status==='RESERVED';
  const confirmCancel=()=>showAppAlert('주문 취소','이 주문을 취소하시겠어요?',[{text:'계속 주문',style:'cancel'},{text:'취소하기',style:'destructive',onPress:()=>void cancelOrder(id).then(setOrder).catch(error=>showAppAlert('취소하지 못했어요',error instanceof Error?error.message:'잠시 후 다시 시도해주세요.'))}]);
  return <Page title="주문 상세" description={`주문 #${order.orderId}`} refreshing={refreshing} onRefresh={onRefresh} onClose={closeToOrders} closeLabel="주문내역으로 닫기">
    <View style={styles.status}><Text style={styles.statusTitle}>{statusTitle(order.status)}</Text><Text style={styles.statusText}>{order.pickupStartAt?.slice(0,5)??'--:--'}–{order.pickupEndAt?.slice(0,5)??'--:--'} · {order.storeName??`매장 #${order.storeId}`}</Text></View>
    {!['CANCELLED','REJECTED','NO_SHOW'].includes(order.status)?<View style={styles.sequence}>{steps.map((label,index)=><View key={label} style={styles.step}>{index?<View style={[styles.connector,index<=current&&styles.doneBg]}/>:null}<View style={[styles.dot,index<=current&&styles.doneBg]}/><Text style={[styles.stepText,index<=current&&styles.doneText]}>{label}</Text></View>)}</View>:null}
    {order.status==='PICKUP_READY'&&code?<View style={styles.pickupCode}><Text style={styles.codeLabel}>픽업 코드</Text><Text style={styles.code}>{code}</Text></View>:null}
    <View style={styles.section}><Pressable accessibilityRole="button" onPress={()=>router.push({pathname:'/stores/[storeId]',params:{storeId:String(order.storeId),origin:'/orders'}})} style={styles.storeHeading}><Text style={styles.sectionTitle}>{order.storeName??`매장 #${order.storeId}`}</Text><Ionicons name="chevron-forward" size={17} color={colors.ink700}/></Pressable><View style={styles.product}>{order.storeImageUrl?<Image source={{uri:order.storeImageUrl}} style={styles.photo}/>:<View style={styles.photo}><Ionicons name="restaurant-outline" size={22} color={colors.green700}/></View>}<View><Text style={styles.productName}>{order.dishName} × {order.quantity}</Text><Text style={styles.meta}>{Number(order.unitPrice).toLocaleString()}원</Text></View></View></View>
    <View style={styles.receipt}><Text style={styles.sectionTitle}>결제 정보</Text><Info label="상품 금액" value={`${Number(order.totalPrice).toLocaleString()}원`}/><Info label="결제 상태" value={order.paymentStatus==='PAID'?'결제 완료':order.paymentStatus}/><View style={styles.total}><Text style={styles.productName}>총 결제 금액</Text><Text style={styles.productName}>{Number(order.totalPrice).toLocaleString()}원</Text></View></View>
    {order.rejectReason?<View style={styles.section}><Text style={styles.sectionTitle}>처리 사유</Text><Text style={styles.request}>{order.rejectReason}</Text></View>:null}
    {cancellable?<Pressable style={styles.cancel} onPress={confirmCancel}><Text style={styles.cancelText}>주문 취소</Text></Pressable>:null}
  </Page>;
}
function Info({label,value}:{label:string;value:string}){return <View style={styles.info}><Text style={styles.request}>{label}</Text><Text style={styles.request}>{value}</Text></View>}
const styles=StyleSheet.create({status:{padding:15,backgroundColor:colors.green100,borderWidth:1,borderColor:colors.green300,borderRadius:radius.card},statusTitle:{fontSize:18,fontWeight:'700',color:colors.ink900,fontFamily:fonts.body},statusText:{marginTop:5,color:colors.ink700,fontFamily:fonts.body},sequence:{flexDirection:'row',marginVertical:4},step:{flex:1,alignItems:'center',paddingTop:17},dot:{position:'absolute',top:0,zIndex:2,width:9,height:9,borderRadius:5,backgroundColor:colors.line},connector:{position:'absolute',right:'50%',top:4,width:'100%',height:2,backgroundColor:colors.line},stepText:{fontSize:9,color:colors.ink400,fontFamily:fonts.body},doneBg:{backgroundColor:colors.green300},doneText:{color:colors.green700},pickupCode:{minHeight:46,paddingHorizontal:13,flexDirection:'row',alignItems:'center',justifyContent:'space-between',backgroundColor:'#EEF4F8',borderColor:'#CBDCE6',borderWidth:1,borderRadius:9},codeLabel:{fontSize:11,color:colors.ink700,fontFamily:fonts.body},code:{fontSize:19,letterSpacing:1.4,fontWeight:'700',color:colors.green700},section:{gap:9,paddingVertical:14,borderBottomWidth:1,borderBottomColor:colors.line},storeHeading:{minHeight:44,flexDirection:'row',alignItems:'center',justifyContent:'space-between'},sectionTitle:{fontWeight:'700',color:colors.ink900,fontFamily:fonts.body},product:{flexDirection:'row',alignItems:'center',gap:10},photo:{width:55,height:55,alignItems:'center',justifyContent:'center',borderRadius:9,backgroundColor:colors.green100},productName:{fontWeight:'700',color:colors.ink900,fontFamily:fonts.body},meta:{marginTop:5,color:colors.ink400,fontFamily:fonts.body},receipt:{gap:9,paddingVertical:14,borderBottomWidth:1,borderBottomColor:colors.line},info:{flexDirection:'row',justifyContent:'space-between'},total:{paddingTop:10,flexDirection:'row',justifyContent:'space-between',borderTopWidth:1,borderTopColor:colors.line},request:{color:colors.ink700,fontFamily:fonts.body},cancel:{minHeight:46,alignItems:'center',justifyContent:'center',borderWidth:1,borderColor:'#D83A3A',borderRadius:9},cancelText:{color:'#D83A3A',fontWeight:'600',fontFamily:fonts.body}});
