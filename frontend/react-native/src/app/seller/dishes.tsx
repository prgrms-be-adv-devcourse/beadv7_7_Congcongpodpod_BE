import { Ionicons } from '@expo/vector-icons';
import { router, useFocusEffect } from 'expo-router';
import { useCallback, useMemo, useState } from 'react';
import { Image, Modal, Pressable, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { EmptyState } from '@/components/empty-state';
import { LoadingState } from '@/components/loading-state';
import { showAppAlert } from '@/lib/app-overlay';
import { SellerShell } from '@/components/seller-shell';
import { colors, fonts, radius, shadow } from '@/constants/theme';
import { getDishImageSource } from '@/lib/food-image';
import { usePullToRefresh } from '@/hooks/use-pull-to-refresh';
import { adjustDishStock, getMyStores, getSellerDishes } from '@/lib/seller';
import type { Dish, Store } from '@/types/store';

const filters = [['ALL', '전체'], ['ON_SALE', '판매 중'], ['SOLD_OUT', '품절']] as const;
type Filter = (typeof filters)[number][0];

export default function SellerDishes() {
  const [store, setStore] = useState<Store | null>(null);
  const [dishes, setDishes] = useState<Dish[]>([]);
  const [filter, setFilter] = useState<Filter>('ALL');
  const [loading, setLoading] = useState(true);
  const [failed, setFailed] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setFailed(false);
    try {
      const [mine] = await getMyStores();
      setStore(mine ?? null);
      setDishes(mine ? await getSellerDishes(mine.storeId) : []);
    } catch {
      setFailed(true);
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(useCallback(() => { void load(); }, [load]));
  const { refreshing, onRefresh } = usePullToRefresh(load);

  const visible = useMemo(() => dishes.filter((dish) => {
    if (filter === 'ALL') return true;
    return (dish.status ?? (dish.quantity > 0 ? 'ON_SALE' : 'SOLD_OUT')) === filter;
  }), [dishes, filter]);

  const updateDish = (updated: Dish) => setDishes((current) => current.map((dish) => dish.dishId === updated.dishId ? updated : dish));

  return (
    <SellerShell title="상품 관리" description="판매 상태와 재고를 한눈에 확인하고 필요한 만큼만 조정하세요." storeName={store?.storeName ?? '미등록'} refreshing={refreshing} onRefresh={onRefresh}>
      <View accessibilityRole="tablist" style={styles.tabs}>
        {filters.map(([key, label]) => <Pressable accessibilityRole="tab" accessibilityState={{ selected: filter === key }} key={key} onPress={() => setFilter(key)} style={styles.tab}><Text style={[styles.tabText, filter === key && styles.tabTextActive]}>{label}</Text>{filter === key ? <View style={styles.tabLine}/> : null}</Pressable>)}
      </View>

      <View style={styles.toolbar}>
        <View><Text style={styles.listTitle}>등록 상품</Text><Text style={styles.listMeta}>{visible.length}개 상품</Text></View>
        <Pressable accessibilityRole="button" disabled={!store} onPress={() => router.push('/seller/dishes/new')} style={({ pressed }) => [styles.addButtonHit, pressed && styles.pressed, !store && styles.disabled]}><View style={styles.addButton}><Ionicons name="add" size={15} color={colors.white}/><Text style={styles.addButtonText}>상품 등록</Text></View></Pressable>
      </View>

      {loading && !dishes.length ? <LoadingState label="등록 상품을 확인하고 있어요" compact/> : failed && !dishes.length ? <EmptyState title="상품을 불러오지 못했어요" description="잠시 후 다시 시도해주세요." actionLabel="다시 불러오기" onAction={() => void load()}/> : visible.length ? <View style={styles.list}>{visible.map((dish) => <ProductRow category={store?.category} dish={dish} key={dish.dishId} onChanged={updateDish}/>)}</View> : <EmptyState title={filter === 'ALL' ? '등록된 상품이 없어요' : '이 상태의 상품이 없어요'} description={filter === 'ALL' ? '첫 상품을 등록하고 마감 판매를 시작해보세요.' : '다른 상태의 상품을 확인해보세요.'}/>} 
    </SellerShell>
  );
}

function ProductRow({ dish, category, onChanged }: { dish: Dish; category?: string; onChanged: (dish: Dish) => void }) {
  const [open, setOpen] = useState(false);
  const [draftStock, setDraftStock] = useState(dish.quantity);
  const [saving, setSaving] = useState(false);
  const available = dish.quantity > 0 && (!dish.status || dish.status === 'ON_SALE');
  const statusLabel = available ? '판매 중' : dish.quantity === 0 || dish.status === 'SOLD_OUT' ? '품절' : '판매 종료';
  const changed = draftStock !== dish.quantity;

  const openEditor = () => {
    setDraftStock(dish.quantity);
    setOpen(true);
  };

  const closeEditor = () => {
    if (saving) return;
    setDraftStock(dish.quantity);
    setOpen(false);
  };

  const save = async () => {
    const quantityDelta = draftStock - dish.quantity;
    if (!quantityDelta || saving) return;
    try {
      setSaving(true);
      const updated = await adjustDishStock(dish.dishId, quantityDelta);
      onChanged(updated);
      setDraftStock(updated.quantity);
      setOpen(false);
    } catch (error) {
      showAppAlert('재고를 변경하지 못했어요', error instanceof Error ? error.message : '잠시 후 다시 시도해주세요.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <View style={styles.product}>
      <View style={styles.productSummary}>
        <Image accessible accessibilityLabel={`${dish.dishName} 상품 이미지`} source={getDishImageSource(dish, category)} style={styles.image}/>
        <View style={styles.productCopy}>
          <View style={styles.statusRow}><View style={[styles.dot, !available && styles.dotStop]}/><Text style={[styles.status, !available && styles.stop]}>{statusLabel}</Text></View>
          <Text numberOfLines={1} style={styles.name}>{dish.dishName}</Text>
          <Text style={styles.price}>{dish.discountPrice.toLocaleString()}원{dish.price > dish.discountPrice ? <Text style={styles.originalPrice}>  {dish.price.toLocaleString()}원</Text> : null}</Text>
          <Text style={styles.stock}>현재 재고 <Text style={styles.stockStrong}>{dish.quantity}개</Text></Text>
        </View>
      </View>

      <Pressable accessibilityRole="button" accessibilityState={{ expanded: open }} onPress={openEditor} style={({ pressed }) => [styles.stockButton, pressed && styles.pressed]}><Ionicons name="options-outline" size={17} color={colors.ink900}/><Text style={styles.stockButtonText}>재고 조정</Text><Ionicons name="chevron-forward" size={16} color={colors.ink500}/></Pressable>

      <Modal animationType="fade" onRequestClose={closeEditor} presentationStyle="overFullScreen" transparent visible={open}>
        <View style={styles.modalRoot}>
          <Pressable accessibilityLabel="재고 조정 닫기" accessibilityRole="button" onPress={closeEditor} style={styles.scrim}/>
          <SafeAreaView edges={['bottom']} style={[styles.sheetStage, { width: '100%', maxWidth: 560, alignSelf: 'center', overflow: 'hidden', borderTopLeftRadius: radius.sheet, borderTopRightRadius: radius.sheet, backgroundColor: colors.white }]}>
            <View accessibilityViewIsModal style={[styles.sheet, { paddingBottom: 24 }]}>
              <View style={styles.sheetHandle}/>
              <View style={styles.sheetHeader}>
                <View style={styles.sheetHeading}><Text style={styles.sheetTitle}>재고 조정</Text><Text numberOfLines={1} style={styles.sheetProduct}>{dish.dishName} · 현재 {dish.quantity}개</Text></View>
                <Pressable accessibilityLabel="재고 조정 닫기" accessibilityRole="button" disabled={saving} hitSlop={4} onPress={closeEditor} style={({ pressed }) => [styles.sheetClose, pressed && styles.stepPressed, saving && styles.disabled]}><Ionicons name="close" size={23} color={colors.ink900}/></Pressable>
              </View>

              <View style={styles.editorHead}><View><Text style={styles.editorTitle}>판매 수량 변경</Text><Text style={styles.hint}>저장할 때 서버에 한 번만 반영됩니다.</Text></View><View style={styles.changePreview}>{changed ? <><Text style={styles.previewBefore}>{dish.quantity}</Text><Ionicons name="arrow-forward" size={13} color={colors.ink400}/><Text style={styles.previewAfter}>{draftStock}개</Text></> : <Text style={styles.previewCurrent}>현재 {dish.quantity}개</Text>}</View></View>
              <View style={styles.stepper}>
                <Pressable accessibilityLabel="재고 한 개 제거" accessibilityRole="button" accessibilityState={{ disabled: draftStock === 0 }} disabled={draftStock === 0} hitSlop={4} onPress={() => setDraftStock((value) => Math.max(0, value - 1))} style={({ pressed }) => [styles.step, pressed && styles.stepPressed, draftStock === 0 && styles.stepDisabled]}><Ionicons name="remove" size={21} color={colors.ink900}/><Text style={styles.stepLabel}>제거</Text></Pressable>
                <View accessibilityLiveRegion="polite" style={styles.draft}><View style={styles.draftContent}><Text style={styles.draftValue}>{draftStock}</Text><Text style={styles.draftUnit}>개</Text></View></View>
                <Pressable accessibilityLabel="재고 한 개 추가" accessibilityRole="button" hitSlop={4} onPress={() => setDraftStock((value) => value + 1)} style={({ pressed }) => [styles.step, pressed && styles.stepPressed]}><Ionicons name="add" size={21} color={colors.ink900}/><Text style={styles.stepLabel}>추가</Text></Pressable>
              </View>
              <View style={styles.quickRow}>{[-10, -5, 5, 10].map((delta) => <Pressable accessibilityLabel={`재고 ${Math.abs(delta)}개 ${delta < 0 ? '제거' : '추가'}`} accessibilityRole="button" key={delta} onPress={() => setDraftStock((value) => Math.max(0, value + delta))} style={({ pressed }) => [styles.quickButton, pressed && styles.stepPressed]}><Text style={styles.quickText}>{delta > 0 ? `+${delta}` : delta}</Text></Pressable>)}</View>
              <View style={styles.editorActions}>
                <Pressable accessibilityRole="button" disabled={saving} onPress={closeEditor} style={({ pressed }) => [styles.cancelButton, pressed && styles.pressed, saving && styles.disabled]}><Text style={styles.cancelText}>취소</Text></Pressable>
                <Pressable accessibilityRole="button" accessibilityState={{ disabled: !changed || saving }} disabled={!changed || saving} onPress={() => void save()} style={({ pressed }) => [styles.saveButton, pressed && styles.pressed, (!changed || saving) && styles.saveButtonDisabled]}><Text style={[styles.saveText, (!changed || saving) && styles.saveTextDisabled]}>{saving ? '저장 중…' : changed ? `${draftStock}개로 저장` : '변경 없음'}</Text></Pressable>
              </View>
            </View>
          </SafeAreaView>
        </View>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  tabs: { flexDirection: 'row', borderBottomWidth: 1, borderBottomColor: colors.line },
  tab: { flex: 1, minHeight: 48, alignItems: 'center', justifyContent: 'center' },
  tabText: { color: colors.ink400, fontFamily: fonts.body, fontSize: 13, fontWeight: '700' },
  tabTextActive: { color: colors.ink900, fontWeight: '900' },
  tabLine: { position: 'absolute', left: '50%', bottom: 0, width: 48, height: 3, marginLeft: -24, borderRadius: 2, backgroundColor: colors.ink900 },
  toolbar: { minHeight: 58, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  listTitle: { color: colors.ink900, fontFamily: fonts.body, fontSize: 18, fontWeight: '900' },
  listMeta: { marginTop: 3, color: colors.ink500, fontFamily: fonts.body, fontSize: 10, fontWeight: '600' },
  addButtonHit: { minHeight: 44, justifyContent: 'center' },
  addButton: { height: 34, paddingHorizontal: 10, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 3, borderRadius: 9, backgroundColor: colors.green500 },
  addButtonText: { color: colors.white, fontFamily: fonts.body, fontSize: 11, fontWeight: '800' },
  list: { gap: 11 },
  product: { padding: 14, borderRadius: 12, borderWidth: 1, borderColor: colors.line, backgroundColor: colors.white, ...shadow.card },
  productSummary: { flexDirection: 'row', gap: 13 },
  image: { width: 96, height: 96, borderRadius: 10, backgroundColor: colors.canvas },
  productCopy: { flex: 1, minWidth: 0, justifyContent: 'center' },
  statusRow: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  dot: { width: 6, height: 6, borderRadius: 3, backgroundColor: colors.green500 },
  dotStop: { backgroundColor: colors.ink400 },
  status: { color: colors.green700, fontFamily: fonts.body, fontSize: 10, fontWeight: '800' },
  stop: { color: colors.ink500 },
  name: { marginTop: 6, color: colors.ink900, fontFamily: fonts.body, fontSize: 17, fontWeight: '900', letterSpacing: -0.35 },
  price: { marginTop: 5, color: colors.ink900, fontFamily: fonts.body, fontSize: 14, fontWeight: '800' },
  originalPrice: { color: colors.ink400, fontSize: 10, fontWeight: '500', textDecorationLine: 'line-through' },
  stock: { marginTop: 5, color: colors.ink700, fontFamily: fonts.body, fontSize: 11 },
  stockStrong: { color: colors.ink900, fontWeight: '900' },
  stockButton: { minHeight: 44, marginTop: 13, paddingHorizontal: 12, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 7, borderRadius: radius.control, backgroundColor: colors.canvas, borderWidth: 1, borderColor: colors.line },
  stockButtonText: { color: colors.ink900, fontFamily: fonts.body, fontSize: 12, fontWeight: '800' },
  modalRoot: { flex: 1, justifyContent: 'flex-end' },
  scrim: { ...StyleSheet.absoluteFillObject, backgroundColor: 'rgba(15, 20, 17, 0.46)' },
  sheetStage: { width: '100%', alignItems: 'center' },
  sheet: { width: '100%', maxWidth: 560, paddingHorizontal: 20, paddingTop: 8, paddingBottom: 12, borderTopLeftRadius: 22, borderTopRightRadius: 22, backgroundColor: colors.white, ...shadow.float },
  sheetHandle: { alignSelf: 'center', width: 36, height: 4, borderRadius: 2, backgroundColor: colors.lineStrong },
  sheetHeader: { minHeight: 74, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 12 },
  sheetHeading: { flex: 1, minWidth: 0 },
  sheetTitle: { color: colors.ink900, fontFamily: fonts.body, fontSize: 20, fontWeight: '900', letterSpacing: -0.45 },
  sheetProduct: { marginTop: 4, color: colors.ink500, fontFamily: fonts.body, fontSize: 11, fontWeight: '700' },
  sheetClose: { width: 44, height: 44, alignItems: 'center', justifyContent: 'center', borderRadius: 22, backgroundColor: colors.canvas },
  editorHead: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 12 },
  editorTitle: { color: colors.ink900, fontFamily: fonts.body, fontSize: 14, fontWeight: '900' },
  hint: { marginTop: 4, color: colors.ink500, fontFamily: fonts.body, fontSize: 10 },
  changePreview: { minHeight: 30, paddingHorizontal: 9, flexDirection: 'row', alignItems: 'center', gap: 5, borderRadius: radius.pill, backgroundColor: colors.canvas },
  previewBefore: { color: colors.ink500, fontFamily: fonts.body, fontSize: 11, textDecorationLine: 'line-through' },
  previewAfter: { color: colors.ink900, fontFamily: fonts.body, fontSize: 12, fontWeight: '900' },
  previewCurrent: { color: colors.ink700, fontFamily: fonts.body, fontSize: 11, fontWeight: '800' },
  stepper: { height: 60, marginTop: 15, flexDirection: 'row', overflow: 'hidden', borderRadius: radius.input, borderWidth: 1, borderColor: colors.lineStrong, backgroundColor: colors.white },
  step: { width: 84, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.canvas },
  stepPressed: { backgroundColor: colors.line },
  stepDisabled: { opacity: 0.35 },
  stepLabel: { marginTop: 1, color: colors.ink700, fontFamily: fonts.body, fontSize: 9, fontWeight: '700' },
  draft: { flex: 1, alignItems: 'center', justifyContent: 'center', borderLeftWidth: 1, borderRightWidth: 1, borderColor: colors.line },
  draftContent: { flexDirection: 'row', alignItems: 'baseline', justifyContent: 'center' },
  draftValue: { color: colors.ink900, fontFamily: fonts.body, fontSize: 23, lineHeight: 28, fontWeight: '900' },
  draftUnit: { marginLeft: 3, color: colors.ink700, fontFamily: fonts.body, fontSize: 11, lineHeight: 16, fontWeight: '700' },
  quickRow: { marginTop: 8, flexDirection: 'row', gap: 6 },
  quickButton: { flex: 1, minHeight: 44, alignItems: 'center', justifyContent: 'center', borderRadius: 8, backgroundColor: colors.canvas, borderWidth: 1, borderColor: colors.line },
  quickText: { color: colors.ink700, fontFamily: fonts.body, fontSize: 11, fontWeight: '800' },
  editorActions: { marginTop: 10, flexDirection: 'row', gap: 8 },
  cancelButton: { flex: 1, minHeight: 44, alignItems: 'center', justifyContent: 'center', borderRadius: radius.control, borderWidth: 1, borderColor: colors.lineStrong, backgroundColor: colors.white },
  cancelText: { color: colors.ink700, fontFamily: fonts.body, fontSize: 11, fontWeight: '800' },
  saveButton: { flex: 1.45, minHeight: 44, alignItems: 'center', justifyContent: 'center', borderRadius: radius.control, backgroundColor: colors.green500 },
  saveText: { color: colors.white, fontFamily: fonts.body, fontSize: 11, fontWeight: '900' },
  saveButtonDisabled: { backgroundColor: colors.canvas, borderWidth: 1, borderColor: colors.line },
  saveTextDisabled: { color: colors.ink400 },
  disabled: { opacity: 0.42 },
  pressed: { opacity: 0.72 },
});
