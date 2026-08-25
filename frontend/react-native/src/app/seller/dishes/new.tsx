import { RoundedIcon as Ionicons } from '@/components/rounded-icon';
import * as ImagePicker from 'expo-image-picker';
import { router } from 'expo-router';
import { useEffect, useRef, useState } from 'react';
import { Animated, Easing, Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import { OptimizedImage as Image } from '@/components/optimized-image';

import { PrimaryButton } from '@/components/page';
import { SellerShell } from '@/components/seller-shell';
import { colors, fonts, radius } from '@/constants/theme';
import { classifyFoodImage, FoodImageTooLargeError, type FoodAnalysisPhase, type FoodClassification } from '@/lib/ai';
import { showAppAlert } from '@/lib/app-overlay';
import { getDishImageSource } from '@/lib/food-image';
import { getMyStores, registerDish } from '@/lib/seller';

const defaultCategories = ['한식', '치킨', '중식', '분식', '카페·디저트', '패스트푸드'];
type PriceInputMode = 'rate' | 'price';
const minimumDiscountRate = 30;

function calculateDiscountPrice(regularPrice: number, discountRate: number) {
  if (regularPrice <= 0 || discountRate < minimumDiscountRate || discountRate >= 100) return 0;
  const rawPrice = regularPrice * (1 - discountRate / 100);
  return Math.round(rawPrice / 100) * 100;
}
const analysisMessages: Record<FoodAnalysisPhase, { title: string; description: string }> = {
  preparing: { title: '이미지 분석 준비 중...', description: '사진 크기와 형식을 확인하고 있어요.' },
  compressing: { title: '이미지가 커서 압축 중입니다...', description: '화질을 유지하면서 전송 용량을 줄이고 있어요.' },
  compressingAgain: { title: '이미지 용량을 한 번 더 줄이는 중...', description: '업로드 가능한 크기로 안전하게 조정하고 있어요.' },
  uploading: { title: '이미지를 전송 중입니다...', description: 'AI 분석 서버로 사진을 안전하게 보내고 있어요.' },
  analyzing: { title: '사진에 맞는 추천 카테고리 분류 중...', description: '음식의 특징을 분석해 가장 가까운 카테고리를 찾고 있어요.' },
};

function nextFrame() {
  return new Promise<void>((resolve) => requestAnimationFrame(() => resolve()));
}

function formatTimeInput(value: string) {
  const digits = value.replace(/\D/g, '').slice(0, 4);
  return digits.length > 2 ? `${digits.slice(0, 2)}:${digits.slice(2)}` : digits;
}

function addMinutes(time: string, minutes: number) {
  const match = /^(\d{2}):(\d{2})$/.exec(time);
  const base = match ? Number(match[1]) * 60 + Number(match[2]) : 18 * 60;
  const result = (base + minutes) % (24 * 60);
  return `${String(Math.floor(result / 60)).padStart(2, '0')}:${String(result % 60).padStart(2, '0')}`;
}

function localDateTime() {
  const date = new Date();
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
  return local.toISOString().slice(0, 19);
}

export default function NewDish() {
  const [image, setImage] = useState<ImagePicker.ImagePickerAsset | null>(null);
  const [classification, setClassification] = useState<FoodClassification | null>(null);
  const [analysisUnavailable, setAnalysisUnavailable] = useState(false);
  const [category, setCategory] = useState('');
  const [classifying, setClassifying] = useState(false);
  const [picking, setPicking] = useState(false);
  const [dishName, setDishName] = useState('');
  const [description, setDescription] = useState('');
  const [regularPrice, setRegularPrice] = useState('');
  const [discountInput, setDiscountInput] = useState('30');
  const [priceInputMode, setPriceInputMode] = useState<PriceInputMode>('rate');
  const [directSalePrice, setDirectSalePrice] = useState('');
  const [quantity, setQuantity] = useState(10);
  const [pickupStart, setPickupStart] = useState('18:00');
  const [pickupEnd, setPickupEnd] = useState('20:00');
  const [analysisPhase, setAnalysisPhase] = useState<FoodAnalysisPhase>('preparing');
  const [storeId, setStoreId] = useState<number | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const analysisId = useRef(0);
  const analysisProgress = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    void getMyStores().then(([store]) => setStoreId(store?.storeId ?? null)).catch(() => setStoreId(null));
  }, []);

  useEffect(() => {
    if (!classifying) {
      analysisProgress.stopAnimation();
      return;
    }

    analysisProgress.setValue(0);
    const progressAnimation = Animated.loop(Animated.timing(analysisProgress, {
      toValue: 1,
      duration: 1800,
      easing: Easing.inOut(Easing.cubic),
      useNativeDriver: false,
    }));
    progressAnimation.start();

    return () => {
      progressAnimation.stop();
    };
  }, [analysisProgress, classifying]);

  const pickImage = async () => {
    if (picking) return;
    setPicking(true);
    try {
      const permission = await ImagePicker.requestMediaLibraryPermissionsAsync();
      if (!permission.granted) {
        showAppAlert('사진 접근이 필요해요', '상품 이미지를 선택하려면 설정에서 사진 접근을 허용해주세요.');
        return;
      }

      const result = await ImagePicker.launchImageLibraryAsync({ mediaTypes: ['images'], allowsEditing: true, aspect: [4, 3], quality: 0.85 });
      if (result.canceled || !result.assets[0]) return;

      const selected = result.assets[0];
      const currentAnalysisId = ++analysisId.current;
      setImage(selected);
      setClassification(null);
      setAnalysisUnavailable(false);
      setCategory('');
      setAnalysisPhase('preparing');
      setClassifying(true);
      try {
        // 선택한 사진과 분석 상태가 먼저 그려진 뒤 압축을 시작해 화면 멈춤처럼 보이지 않게 합니다.
        await nextFrame();
        await nextFrame();
        const recommendation = await classifyFoodImage(selected, (phase) => {
          if (currentAnalysisId === analysisId.current) setAnalysisPhase(phase);
        });
        if (currentAnalysisId !== analysisId.current) return;
        setClassification(recommendation);
        setCategory(recommendation.predictedCategory);
        showAppAlert('카테고리를 추천했어요', `${recommendation.predictedCategory} · 신뢰도 ${Math.round(recommendation.confidence * 100)}%`);
      } catch (error) {
        if (currentAnalysisId !== analysisId.current) return;
        if (error instanceof FoodImageTooLargeError) {
          setImage(null);
          setClassification(null);
          setAnalysisUnavailable(false);
          setCategory('');
          showAppAlert('사진 용량이 너무 커요', error.message);
        } else {
          setAnalysisUnavailable(true);
          const timedOut = error instanceof Error && error.message.includes('시간이 초과');
          showAppAlert(
            'AI 분석을 건너뛰었어요',
            timedOut
              ? '5초 안에 분석 결과를 받지 못했어요. 아래에서 카테고리를 직접 선택해주세요.'
              : '이미지 분석 서버에서 응답을 받지 못했어요. 아래에서 카테고리를 직접 선택해주세요.',
          );
        }
      } finally {
        if (currentAnalysisId === analysisId.current) setClassifying(false);
      }
    } catch (error) {
      showAppAlert('사진을 열지 못했어요', error instanceof Error ? error.message : '잠시 후 다시 시도해주세요.');
    } finally {
      setPicking(false);
    }
  };

  const removeImage = () => {
    showAppAlert('상품 사진을 삭제할까요?', '추천된 카테고리도 함께 초기화됩니다.', [
      { text: '취소', style: 'cancel' },
      {
        text: '삭제',
        style: 'destructive',
        onPress: () => {
          analysisId.current += 1;
          setImage(null);
          setClassification(null);
          setAnalysisUnavailable(false);
          setCategory('');
          setClassifying(false);
        },
      },
    ]);
  };

  const categories = classification?.predictedCategory && !defaultCategories.includes(classification.predictedCategory)
    ? [classification.predictedCategory, ...defaultCategories]
    : defaultCategories;

  const selectCategory = (item: string) => {
    setCategory(item);
  };
  const originalPrice = Number(regularPrice || 0);
  const requestedDiscountRate = Number(discountInput || 0);
  const enteredSalePrice = Number(directSalePrice || 0);
  const roundedEnteredSalePrice = Math.round(enteredSalePrice / 100) * 100;
  const discountedPrice = priceInputMode === 'rate'
    ? calculateDiscountPrice(originalPrice, requestedDiscountRate)
    : roundedEnteredSalePrice;
  const savings = Math.max(0, originalPrice - discountedPrice);
  const actualDiscountRate = originalPrice > 0 ? savings / originalPrice * 100 : 0;
  const discountInputInvalid = priceInputMode === 'rate' && discountInput.length > 0 && (requestedDiscountRate < minimumDiscountRate || requestedDiscountRate >= 100);
  const directPriceInvalid = priceInputMode === 'price' && directSalePrice.length > 0 && (roundedEnteredSalePrice <= 0 || roundedEnteredSalePrice >= originalPrice || actualDiscountRate + Number.EPSILON < minimumDiscountRate);
  const roundedPriceInvalid = priceInputMode === 'rate' && originalPrice > 0 && requestedDiscountRate >= minimumDiscountRate && actualDiscountRate + Number.EPSILON < minimumDiscountRate;
  const priceMissing = originalPrice <= 0 || (priceInputMode === 'rate' ? discountInput.length === 0 : directSalePrice.length === 0);
  const requiredMissing = !image || !category || !dishName.trim() || !description.trim() || !storeId || !/^\d{2}:\d{2}$/.test(pickupStart) || !/^\d{2}:\d{2}$/.test(pickupEnd);

  const submit = async () => {
    if (submitting) return;
    if (!storeId) return showAppAlert('매장을 확인하지 못했어요', '내 매장을 다시 확인한 뒤 시도해주세요.');
    if (!image) return showAppAlert('상품 사진을 등록해주세요', '상품 생성에는 대표 사진이 필요해요.');
    if (!category) return showAppAlert('카테고리를 확인해주세요', '사진 분석 결과 또는 직접 선택한 카테고리가 필요해요.');
    if (!dishName.trim() || !description.trim()) return showAppAlert('상품 정보를 입력해주세요', '상품명과 상품 설명을 모두 작성해주세요.');
    try {
      setSubmitting(true);
      await registerDish({
        storeId,
        dishName: dishName.trim(),
        registeredAt: localDateTime(),
        description: description.trim(),
        category,
        stockQuantity: quantity,
        dishPrice: originalPrice,
        discountPrice: discountedPrice,
        pickupStartTime: pickupStart,
        pickupEndTime: pickupEnd,
      }, image);
      showAppAlert('상품을 등록했어요', '상품 관리에서 판매 상태와 재고를 확인할 수 있어요.', [
        { text: '확인', onPress: () => router.replace('/seller/dishes') },
      ]);
    } catch (error) {
      showAppAlert('상품을 등록하지 못했어요', error instanceof Error ? error.message : '잠시 후 다시 시도해주세요.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <SellerShell back title="새 상품 등록" description="사진을 등록하면 AI가 어울리는 카테고리를 추천해드려요.">
      <View>
        <Text style={styles.label}>상품 이미지</Text>
        <View style={styles.upload}>
          <Image source={image ? { uri: image.uri } : getDishImageSource({})} style={styles.uploadImage} />
          <View style={styles.uploadShade} />
          {image ? <Pressable accessibilityLabel="상품 사진 삭제" onPress={removeImage} style={styles.removeImage}><Ionicons name="trash-outline" size={17} color={colors.ink900}/></Pressable> : null}
          <Pressable accessibilityRole="button" accessibilityState={{ disabled: picking }} disabled={picking} onPress={pickImage} style={styles.uploadButton}>
            <Ionicons name="camera-outline" size={17} color={colors.ink900} />
            <Text style={styles.uploadButtonText}>{picking ? '사진 여는 중' : image ? '사진 다시 선택' : '상품 사진 선택'}</Text>
          </Pressable>
        </View>
        <Text style={styles.helper}>선택 즉시 AI 분석을 시작하며 대표 사진은 상품 상세와 장바구니에 표시돼요.</Text>
      </View>

      <View style={[styles.categoryPanel, classifying && styles.categoryPanelActive]}>
        {classifying && (
          <View style={styles.recommendation}>
          <View style={styles.recommendationRow}>
            <View style={styles.recommendationCopy}>
              <Text style={styles.recommendationEyebrow}>AI 카테고리 추천</Text>
              <Text style={styles.recommendationTitle}>{analysisMessages[analysisPhase].title}</Text>
              <Text style={styles.recommendationDescription}>{analysisMessages[analysisPhase].description}</Text>
            </View>
          </View>
          <View style={styles.progressTrack}><Animated.View style={[styles.progressFill, { width: analysisProgress.interpolate({ inputRange: [0, 1], outputRange: ['14%', '100%'] }) }]} /></View>
          <View style={styles.panelDivider} />
        </View>
        )}
        {!classifying && classification && (
          <View style={styles.categoryHeading}>
            <View style={styles.categoryHeadingCopy}>
              <Text style={styles.label}>AI 분석 완료 · {classification.predictedCategory}</Text>
              <Text style={styles.categoryDescription}>신뢰도 {Math.round(classification.confidence * 100)}% · 분석 결과로 카테고리가 자동 지정됐어요.</Text>
            </View>
          </View>
        )}
        {!classifying && analysisUnavailable && (
          <>
            <View style={styles.categoryHeading}>
              <View style={styles.categoryHeadingCopy}>
                <Text style={styles.label}>{category ? `${category} 직접 선택됨` : '직접 카테고리를 선택해주세요'}</Text>
                <Text style={styles.categoryDescription}>AI 분석을 완료하지 못해 이번 상품만 직접 지정할 수 있어요.</Text>
              </View>
            </View>
            <View style={styles.categoryList}>
              {categories.map((item) => {
                const selected = category === item;
                return (
                  <Pressable key={item} accessibilityRole="button" accessibilityState={{ selected }} onPress={() => selectCategory(item)} style={[styles.categoryChip, selected && styles.categoryChipSelected]}>
                    <Text style={[styles.categoryText, selected && styles.categoryTextSelected]}>{item}</Text>
                  </Pressable>
                );
              })}
            </View>
          </>
        )}
        {!classifying && !classification && !analysisUnavailable && (
          <View style={styles.categoryHeading}>
            <View style={styles.categoryHeadingCopy}>
              <Text style={styles.label}>사진 분석 후 카테고리가 지정돼요</Text>
              <Text style={styles.categoryDescription}>음식 사진을 등록하면 AI가 적합한 카테고리를 확인해요.</Text>
            </View>
          </View>
        )}
      </View>

      <View style={styles.formSection}>
        <SectionHeader title="상품 정보" description="고객이 한눈에 이해할 수 있게 간결하게 작성해주세요." />
        <LabeledField label="상품명" value={dishName} onChangeText={setDishName} placeholder="예: 오늘의 닭갈비 도시락" maxLength={40} />
        <LabeledField label="상품 설명" value={description} onChangeText={setDescription} placeholder="구성, 알레르기 정보, 맛의 특징을 알려주세요." maxLength={160} multiline />
        <Text style={styles.characterCount}>{description.length}/160</Text>
      </View>

      <View style={styles.formSection}>
        <SectionHeader
          title="가격 설정"
          description="할인율 또는 판매가로 입력할 수 있으며 최소 30% 할인이 필요해요."
          onInfo={() => showAppAlert(
            '가격 자동 보정 정책',
            '할인율과 직접 할인가 모두 최종 판매가를 가장 가까운 100원 단위로 반올림합니다. 반올림된 가격은 정가 대비 30% 이상 할인되어야 하며, 기준에 미달하면 할인율 또는 할인가를 수정해야 합니다.',
          )}
        />
        <View accessibilityRole="radiogroup" style={styles.priceMode}>
          {([['rate', '할인율로 입력'], ['price', '할인가로 입력']] as const).map(([value, label]) => {
            const selected = priceInputMode === value;
            return <Pressable key={value} accessibilityRole="radio" accessibilityState={{ selected }} onPress={() => setPriceInputMode(value)} style={[styles.priceModeButton, selected && styles.priceModeButtonSelected]}><Text style={[styles.priceModeText, selected && styles.priceModeTextSelected]}>{label}</Text></Pressable>;
          })}
        </View>
        <View style={styles.fieldRow}>
          <View style={styles.flexField}><MoneyField label="정가" value={regularPrice} onChangeText={setRegularPrice} placeholder="12,000" /></View>
          {priceInputMode === 'rate'
            ? <View style={styles.flexField}><PercentField label="할인율" value={discountInput} onChangeText={setDiscountInput} /></View>
            : <View style={styles.flexField}><MoneyField emphasized label="할인 판매가" value={directSalePrice} onChangeText={setDirectSalePrice} onBlur={() => { if (roundedEnteredSalePrice > 0) setDirectSalePrice(String(roundedEnteredSalePrice)); }} placeholder="8,400" /></View>}
        </View>
        <Text style={styles.priceModeHelp}>{priceInputMode === 'rate' ? '할인 판매가는 100원 단위로 자동 반올림돼요.' : '입력한 판매가도 100원 단위로 자동 반올림하고 실제 할인율을 확인해요.'}</Text>
        {discountInputInvalid ? (
          <View style={styles.priceWarning}><Ionicons name="alert-circle-outline" size={16} color={colors.danger700}/><Text style={styles.priceWarningText}>할인율은 30% 이상 100% 미만으로 입력해주세요.</Text></View>
        ) : directPriceInvalid ? (
          <View style={styles.priceWarning}><Ionicons name="alert-circle-outline" size={16} color={colors.danger700}/><Text style={styles.priceWarningText}>할인 판매가는 정가보다 낮고 실제 할인율이 30% 이상이어야 해요.</Text></View>
        ) : roundedPriceInvalid ? (
          <View style={styles.priceWarning}><Ionicons name="alert-circle-outline" size={16} color={colors.danger700}/><Text style={styles.priceWarningText}>자동 반올림 후 실제 할인율이 30% 미만이에요. 할인율을 조금 높여주세요.</Text></View>
        ) : originalPrice > 0 && discountedPrice > 0 ? (
          <View style={styles.discountPreview}><View style={styles.discountCopy}><Text style={styles.discountLabel}>마감 할인 판매가</Text><Text style={styles.discountPrice}>{discountedPrice.toLocaleString()}원</Text><Text style={styles.discountSaving}>{savings.toLocaleString()}원 절약 · 실제 {actualDiscountRate.toFixed(1)}% 할인</Text></View></View>
        ) : null}
      </View>

      <View style={styles.formSection}>
        <SectionHeader title="판매 수량" description="오늘 판매 가능한 수량만 등록해주세요." />
        <View style={styles.quantityControl}>
          <Pressable accessibilityLabel="판매 수량 줄이기" onPress={() => setQuantity((value) => Math.max(1, value - 1))} style={styles.quantityButton}><Ionicons name="remove" size={22} color={colors.ink900}/></Pressable>
          <View style={styles.quantityValue}><Text style={styles.quantityNumber}>{quantity}</Text><Text style={styles.quantityUnit}>개</Text></View>
          <Pressable accessibilityLabel="판매 수량 늘리기" onPress={() => setQuantity((value) => Math.min(999, value + 1))} style={styles.quantityButton}><Ionicons name="add" size={22} color={colors.ink900}/></Pressable>
        </View>
        <View style={styles.quickRow}>{[5, 10, 20, 50].map((value) => <Pressable key={value} onPress={() => setQuantity(value)} style={[styles.quickButton, quantity === value && styles.quickButtonActive]}><Text style={[styles.quickText, quantity === value && styles.quickTextActive]}>{value}개</Text></Pressable>)}</View>
      </View>

      <View style={styles.formSection}>
        <SectionHeader title="픽업 가능 시간" description="고객이 방문할 수 있는 시작과 마감 시간을 설정해주세요." />
        <View style={styles.fieldRow}>
          <View style={styles.flexField}><LabeledField label="픽업 시작" value={pickupStart} onChangeText={(value) => setPickupStart(formatTimeInput(value))} placeholder="18:00" keyboardType="number-pad" maxLength={5} /></View>
          <View style={styles.timeArrow}><Ionicons name="arrow-forward" size={16} color={colors.ink400}/></View>
          <View style={styles.flexField}><LabeledField label="픽업 마감" value={pickupEnd} onChangeText={(value) => setPickupEnd(formatTimeInput(value))} placeholder="20:00" keyboardType="number-pad" maxLength={5} /></View>
        </View>
        <View style={styles.quickRow}>{[[30, '30분'], [60, '1시간'], [120, '2시간']].map(([minutes, label]) => <Pressable key={minutes} onPress={() => setPickupEnd(addMinutes(pickupStart, Number(minutes)))} style={styles.quickButton}><Text style={styles.quickText}>시작 후 {label}</Text></Pressable>)}</View>
      </View>

      <PrimaryButton label={submitting ? '이미지 업로드 및 등록 중…' : '상품 등록하기'} disabled={submitting || requiredMissing || priceMissing || discountInputInvalid || directPriceInvalid || roundedPriceInvalid} onPress={() => void submit()} />
    </SellerShell>
  );
}

const styles = StyleSheet.create({
  upload: { position: 'relative', height: 170, overflow: 'hidden', backgroundColor: colors.canvas, borderWidth: 1, borderColor: colors.lineStrong, borderRadius: radius.card },
  uploadImage: { width: '100%', height: '100%' },
  uploadShade: { ...StyleSheet.absoluteFillObject, backgroundColor: 'rgba(0,0,0,.08)' },
  removeImage: { position: 'absolute', right: 12, top: 12, width: 40, height: 40, alignItems: 'center', justifyContent: 'center', borderRadius: radius.control, backgroundColor: 'rgba(255,255,255,.94)' },
  uploadButton: { position: 'absolute', right: 12, bottom: 12, minHeight: 40, paddingHorizontal: 12, flexDirection: 'row', alignItems: 'center', gap: 6, borderRadius: radius.control, backgroundColor: 'rgba(255,255,255,.94)' },
  uploadButtonText: { color: colors.ink900, fontFamily: fonts.body, fontSize: 12, fontWeight: '900' },
  helper: { marginTop: 7, color: colors.ink500, fontFamily: fonts.body, fontSize: 10, lineHeight: 15 },
  label: { color: colors.ink900, fontFamily: fonts.body, fontWeight: '900' },
  categoryPanel: { padding: 12, gap: 10, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.lineStrong, borderRadius: radius.card, overflow: 'hidden' },
  categoryPanelActive: { borderColor: colors.green500 },
  recommendation: { gap: 9, padding: 10, backgroundColor: colors.green50, borderRadius: radius.input },
  recommendationRow: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  recommendationCopy: { flex: 1, gap: 2 },
  recommendationEyebrow: { color: colors.green700, fontFamily: fonts.body, fontSize: 10, fontWeight: '800' },
  recommendationTitle: { color: colors.ink900, fontFamily: fonts.body, fontSize: 14, fontWeight: '900' },
  recommendationDescription: { color: colors.ink500, fontFamily: fonts.body, fontSize: 10, lineHeight: 15 },
  progressTrack: { height: 3, overflow: 'hidden', backgroundColor: colors.green200, borderRadius: 2 },
  progressFill: { height: '100%', backgroundColor: colors.green500, borderRadius: 2 },
  panelDivider: { height: 1, backgroundColor: colors.line },
  categoryHeading: { flexDirection: 'row', alignItems: 'center', gap: 9 },
  categoryHeadingCopy: { flex: 1, gap: 2 },
  categoryDescription: { color: colors.ink500, fontFamily: fonts.body, fontSize: 10, lineHeight: 15 },
  categoryList: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  categoryChip: { minHeight: 38, paddingHorizontal: 13, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.white, borderWidth: 1, borderColor: colors.line, borderRadius: radius.control },
  categoryChipSelected: { backgroundColor: colors.ink900, borderColor: colors.ink900 },
  categoryText: { color: colors.ink700, fontFamily: fonts.body, fontSize: 12, fontWeight: '800' },
  categoryTextSelected: { color: colors.white },
  formSection: { padding: 14, gap: 12, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.lineStrong, borderRadius: radius.card },
  sectionHeader: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  sectionCopy: { flex: 1, gap: 2 },
  sectionInfo: { width: 38, height: 38, marginRight: -8, alignItems: 'center', justifyContent: 'center', borderRadius: 19 },
  sectionInfoPressed: { backgroundColor: colors.canvas },
  sectionTitle: { color: colors.ink900, fontFamily: fonts.body, fontSize: 15, fontWeight: '900' },
  sectionDescription: { color: colors.ink500, fontFamily: fonts.body, fontSize: 10, lineHeight: 15 },
  field: { gap: 6 },
  fieldLabel: { color: colors.ink700, fontFamily: fonts.body, fontSize: 11, fontWeight: '800' },
  input: { minHeight: 50, paddingHorizontal: 13, color: colors.ink900, fontFamily: fonts.body, fontSize: 15, backgroundColor: colors.canvasWarm, borderWidth: 1, borderColor: colors.line, borderRadius: radius.input },
  multilineInput: { minHeight: 88, paddingTop: 13, paddingBottom: 13, textAlignVertical: 'top' },
  characterCount: { marginTop: -8, textAlign: 'right', color: colors.ink400, fontFamily: fonts.body, fontSize: 9 },
  fieldRow: { flexDirection: 'row', alignItems: 'flex-end', gap: 9 },
  flexField: { flex: 1, minWidth: 0 },
  moneyWrap: { position: 'relative' },
  moneyInput: { paddingRight: 34, fontWeight: '800' },
  moneyInputEmphasized: { borderColor: colors.green300, backgroundColor: colors.green50 },
  moneyUnit: { position: 'absolute', right: 12, bottom: 16, color: colors.ink500, fontFamily: fonts.body, fontSize: 12, fontWeight: '800' },
  priceMode: { padding: 4, flexDirection: 'row', gap: 4, borderRadius: radius.control, backgroundColor: colors.canvasWarm },
  priceModeButton: { minHeight: 44, flex: 1, alignItems: 'center', justifyContent: 'center', borderRadius: radius.input },
  priceModeButtonSelected: { backgroundColor: colors.ink900 },
  priceModeText: { color: colors.ink500, fontFamily: fonts.body, fontSize: 12, fontWeight: '800' },
  priceModeTextSelected: { color: colors.white },
  priceModeHelp: { marginTop: -4, color: colors.ink500, fontFamily: fonts.body, fontSize: 10, lineHeight: 15 },
  discountPreview: { minHeight: 78, paddingHorizontal: 13, paddingVertical: 11, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 12, borderRadius: radius.input, backgroundColor: colors.green50 },
  discountCopy: { flex: 1 },
  discountLabel: { color: colors.ink700, fontFamily: fonts.body, fontSize: 10, fontWeight: '700' },
  discountPrice: { marginTop: 2, color: colors.ink900, fontFamily: fonts.body, fontSize: 22, fontWeight: '900', fontVariant: ['tabular-nums'] },
  discountSaving: { marginTop: 2, color: colors.green700, fontFamily: fonts.body, fontSize: 10, fontWeight: '800' },
  priceWarning: { minHeight: 42, paddingHorizontal: 12, flexDirection: 'row', alignItems: 'center', gap: 7, borderRadius: radius.input, backgroundColor: colors.danger50 },
  priceWarningText: { color: colors.danger700, fontFamily: fonts.body, fontSize: 11, fontWeight: '700' },
  quantityControl: { minHeight: 72, flexDirection: 'row', alignItems: 'stretch', overflow: 'hidden', borderWidth: 1, borderColor: colors.lineStrong, borderRadius: radius.input },
  quantityButton: { width: 72, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.canvasWarm },
  quantityValue: { flex: 1, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 5, borderLeftWidth: 1, borderRightWidth: 1, borderColor: colors.line },
  quantityNumber: { color: colors.ink900, fontFamily: fonts.body, fontSize: 27, fontWeight: '900', fontVariant: ['tabular-nums'] },
  quantityUnit: { color: colors.ink500, fontFamily: fonts.body, fontSize: 12, fontWeight: '800' },
  quickRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 7 },
  quickButton: { minHeight: 36, paddingHorizontal: 12, alignItems: 'center', justifyContent: 'center', borderWidth: 1, borderColor: colors.line, borderRadius: radius.pill, backgroundColor: colors.canvasWarm },
  quickButtonActive: { borderColor: colors.green700, backgroundColor: colors.green50 },
  quickText: { color: colors.ink700, fontFamily: fonts.body, fontSize: 10, fontWeight: '800' },
  quickTextActive: { color: colors.green700 },
  timeArrow: { width: 22, height: 50, alignItems: 'center', justifyContent: 'center' },
});

function SectionHeader({ title, description, onInfo }: { title: string; description: string; onInfo?: () => void }) {
  return <View style={styles.sectionHeader}><View style={styles.sectionCopy}><Text style={styles.sectionTitle}>{title}</Text><Text style={styles.sectionDescription}>{description}</Text></View>{onInfo ? <Pressable accessibilityHint="가격 반올림과 최소 할인 정책을 확인합니다" accessibilityLabel="가격 자동 보정 정책 안내" accessibilityRole="button" hitSlop={3} onPress={onInfo} style={({ pressed }) => [styles.sectionInfo, pressed && styles.sectionInfoPressed]}><Ionicons name="alert-circle-outline" size={22} color={colors.ink500}/></Pressable> : null}</View>;
}

function LabeledField({ label, multiline, ...props }: { label: string; value: string; onChangeText: (value: string) => void; placeholder: string; maxLength?: number; multiline?: boolean; keyboardType?: 'default' | 'number-pad' }) {
  return <View style={styles.field}><Text style={styles.fieldLabel}>{label}</Text><TextInput {...props} multiline={multiline} placeholderTextColor={colors.ink400} style={[styles.input, multiline && styles.multilineInput]} /></View>;
}

function MoneyField({ label, value, onChangeText, onBlur, placeholder, emphasized }: { label: string; value: string; onChangeText: (value: string) => void; onBlur?: () => void; placeholder: string; emphasized?: boolean }) {
  return <View style={styles.field}><Text style={styles.fieldLabel}>{label}</Text><View style={styles.moneyWrap}><TextInput value={value ? Number(value).toLocaleString() : ''} onBlur={onBlur} onChangeText={(next) => onChangeText(next.replace(/\D/g, '').slice(0, 9))} placeholder={placeholder} placeholderTextColor={colors.ink400} keyboardType="number-pad" style={[styles.input, styles.moneyInput, emphasized && styles.moneyInputEmphasized]}/><Text style={styles.moneyUnit}>원</Text></View></View>;
}

function PercentField({ label, value, onChangeText }: { label: string; value: string; onChangeText: (value: string) => void }) {
  return <View style={styles.field}><Text style={styles.fieldLabel}>{label}</Text><View style={styles.moneyWrap}><TextInput value={value} onChangeText={(next) => onChangeText(next.replace(/\D/g, '').slice(0, 2))} placeholder="30" placeholderTextColor={colors.ink400} keyboardType="number-pad" style={[styles.input, styles.moneyInput, styles.moneyInputEmphasized]}/><Text style={styles.moneyUnit}>%</Text></View></View>;
}
