import { Ionicons } from '@expo/vector-icons';
import * as Location from 'expo-location';
import { router } from 'expo-router';
import { useEffect, useState } from 'react';
import { Image, Pressable, StyleSheet, Text, TextInput, View } from 'react-native';

import { LoadingState } from '@/components/loading-state';
import { showAppAlert } from '@/lib/app-overlay';
import { PrimaryButton } from '@/components/page';
import { SellerShell } from '@/components/seller-shell';
import { colors, fonts, radius } from '@/constants/theme';
import { getMyStores, registerStore, updateStore } from '@/lib/seller';
import { getStoreCoverImageSource, getStoreProfileImageSource } from '@/lib/food-image';
import { useAuth } from '@/providers/auth-provider';

const categories = [
  ['KOREAN', '한식'], ['WESTERN', '양식'], ['CHINESE', '중식'], ['CAFE_DESSERT', '카페·디저트'],
  ['BUNSIK', '분식'], ['CHICKEN', '치킨'], ['PIZZA', '피자'], ['LUNCH_BOX', '도시락'],
] as const;

export default function SellerStore() {
  const { member, refreshSession } = useAuth();
  const editing = member?.role === 'SELLER';
  const [storeId, setStoreId] = useState<number>();
  const [storeName, setStoreName] = useState('');
  const [businessNumber, setBusinessNumber] = useState('');
  const [storeAddress, setStoreAddress] = useState('');
  const [storePhone, setStorePhone] = useState('');
  const [openTime, setOpenTime] = useState('09:00');
  const [closeTime, setCloseTime] = useState('21:00');
  const [category, setCategory] = useState<string>('KOREAN');
  const [coverImageUrl, setCoverImageUrl] = useState<string>();
  const [profileImageUrl, setProfileImageUrl] = useState<string>();
  const [coordinates, setCoordinates] = useState<{ latitude: number; longitude: number }>();
  const [loading, setLoading] = useState(editing);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!editing) return;
    void getMyStores().then(([store]) => {
      if (!store) return;
      setStoreId(store.storeId);
      setStoreName(store.storeName);
      setBusinessNumber(store.businessNumber ?? '');
      setStoreAddress(store.address);
      setStorePhone(store.phone ?? '');
      setOpenTime(store.openTime?.slice(0, 5) ?? '09:00');
      setCloseTime(store.closeTime?.slice(0, 5) ?? '21:00');
      setCategory(store.category);
      setCoverImageUrl(store.coverImageUrl);
      setProfileImageUrl(store.profileImageUrl);
      setCoordinates({ latitude: store.latitude, longitude: store.longitude });
    }).catch(() => showAppAlert('매장 정보를 불러오지 못했어요')).finally(() => setLoading(false));
  }, [editing]);

  const submit = async () => {
    if (!storeName.trim() || !storeAddress.trim() || !storePhone.trim() || (!editing && !businessNumber.trim())) {
      showAppAlert('필수 정보를 입력해주세요', '상점명, 주소, 전화번호와 사업자등록번호를 확인해주세요.');
      return;
    }
    if (!/^([01]\d|2[0-3]):[0-5]\d$/.test(openTime) || !/^([01]\d|2[0-3]):[0-5]\d$/.test(closeTime)) {
      showAppAlert('영업시간을 확인해주세요', '09:00처럼 24시간 형식으로 입력해주세요.');
      return;
    }

    try {
      setSubmitting(true);
      let point = coordinates;
      if (!point) {
        const permission = await Location.requestForegroundPermissionsAsync();
        if (permission.status !== 'granted') throw new Error('매장 위치 등록을 위해 위치 권한이 필요해요.');
        const current = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.Balanced });
        point = { latitude: current.coords.latitude, longitude: current.coords.longitude };
      }
      const common = {
        storeName: storeName.trim(), storeAddress: storeAddress.trim(), storePhone: storePhone.trim(),
        openTime, closeTime, category, latitude: point.latitude, longitude: point.longitude, holidays: [],
      };
      if (editing && storeId) {
        await updateStore(storeId, common);
      } else {
        await registerStore({ ...common, businessNumber: businessNumber.trim() });
        try {
          const profile = await refreshSession();
          if (profile.role === 'SELLER') {
            router.replace('/seller/home');
            return;
          }
        } catch {
          // 매장 생성은 이미 완료됐다. 토큰 갱신 실패를 등록 실패로 오인시키지 않는다.
        }
        showAppAlert(
          '상점 등록은 완료됐어요',
          '판매자 권한을 새로 불러오려면 다시 로그인해주세요.',
          [{ text: '확인', onPress: () => router.replace('/my') }],
        );
        return;
      }
      router.replace('/seller/home');
    } catch (error) {
      showAppAlert(editing ? '매장 정보를 수정하지 못했어요' : '상점을 등록하지 못했어요', error instanceof Error ? error.message : '잠시 후 다시 시도해주세요.');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <SellerShell back title="매장 정보" description="등록된 정보를 확인하고 있어요."><LoadingState compact label="매장 정보를 불러오고 있어요"/></SellerShell>;

  return (
    <SellerShell back title={editing ? '매장 정보 관리' : '우리 가게 등록하기'} description={editing ? '고객에게 보여줄 매장 정보를 관리하세요.' : '등록 완료 후 판매자 권한이 적용돼요.'} storeName={editing ? storeName || '내 매장' : '등록 전'}>
      {!editing ? <View style={styles.roleNotice}><View style={styles.noticeIcon}><Ionicons name="shield-checkmark-outline" size={19} color={colors.green700}/></View><View style={styles.noticeCopy}><Text style={styles.noticeTitle}>등록 전에는 대시보드에 들어갈 수 없어요</Text><Text style={styles.noticeBody}>상점 등록과 서버 권한 변경이 모두 끝난 뒤 상품·주문·정산 메뉴가 열립니다.</Text></View></View> : null}
      <View><Text style={styles.label}>매장 이미지</Text><View style={styles.imageEditor}><View style={styles.coverPreview}><Image source={getStoreCoverImageSource({ category, coverImageUrl })} style={StyleSheet.absoluteFillObject}/><View style={styles.coverShade}/><Text style={styles.coverLabel}>커버 이미지</Text><Pressable accessibilityRole="button" onPress={() => showAppAlert('이미지 업로드 준비 중', '업로드 API 연결 전까지 카테고리 더미 이미지를 사용합니다.')} style={styles.coverEdit}><Ionicons name="camera-outline" size={16} color={colors.ink900}/><Text style={styles.imageEditText}>변경</Text></Pressable></View><View style={styles.profileRow}><Image source={getStoreProfileImageSource({ category, profileImageUrl })} style={styles.profilePreview}/><View style={styles.profileCopy}><Text style={styles.profileTitle}>상점 프로필</Text><Text style={styles.profileDescription}>홈·목록·주문내역에서 매장을 대표해요.</Text></View><Pressable accessibilityRole="button" onPress={() => showAppAlert('이미지 업로드 준비 중', '업로드 API 연결 전까지 카테고리 더미 이미지를 사용합니다.')} style={styles.profileEdit}><Text style={styles.imageEditText}>변경</Text></Pressable></View></View></View>
      <Field label="상점명" value={storeName} onChangeText={setStoreName} placeholder="예: 성수 베이커리"/>
      {!editing ? <Field label="사업자등록번호" value={businessNumber} onChangeText={setBusinessNumber} placeholder="000-00-00000" keyboardType="number-pad"/> : null}
      <Field label="상점 주소" value={storeAddress} onChangeText={setStoreAddress} placeholder="도로명 주소를 입력해주세요"/>
      <Field label="전화번호" value={storePhone} onChangeText={setStorePhone} placeholder="02-0000-0000" keyboardType="phone-pad"/>
      <View style={styles.timeRow}><View style={styles.timeField}><Field label="영업 시작" value={openTime} onChangeText={setOpenTime} placeholder="09:00" keyboardType="numbers-and-punctuation"/></View><View style={styles.timeField}><Field label="영업 종료" value={closeTime} onChangeText={setCloseTime} placeholder="21:00" keyboardType="numbers-and-punctuation"/></View></View>
      <View><Text style={styles.label}>카테고리</Text><View style={styles.categories}>{categories.map(([key, label]) => <Pressable key={key} onPress={() => setCategory(key)} style={({ pressed }) => [styles.category, category === key && styles.categoryActive, pressed && styles.pressed]}><Text style={[styles.categoryText, category === key && styles.categoryTextActive]}>{label}</Text></Pressable>)}</View></View>
      <PrimaryButton disabled={submitting} label={submitting ? (editing ? '수정하는 중…' : '권한을 적용하는 중…') : (editing ? '매장 정보 저장' : '상점 등록하고 판매 시작')} onPress={() => void submit()} />
    </SellerShell>
  );
}

function Field({ label, ...props }: { label: string; value: string; onChangeText: (value: string) => void; placeholder: string; keyboardType?: 'default' | 'number-pad' | 'phone-pad' | 'numbers-and-punctuation' }) {
  return <View><Text style={styles.label}>{label}</Text><TextInput {...props} placeholderTextColor={colors.ink400} style={styles.input}/></View>;
}

const styles = StyleSheet.create({
  roleNotice: { padding: 14, flexDirection: 'row', gap: 11, borderRadius: radius.input, backgroundColor: colors.green50, borderWidth: 1, borderColor: colors.green200 },
  noticeIcon: { width: 36, height: 36, borderRadius: 18, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.white },
  noticeCopy: { flex: 1 },
  noticeTitle: { color: colors.ink900, fontFamily: fonts.body, fontSize: 13, fontWeight: '800' },
  noticeBody: { marginTop: 4, color: colors.ink700, fontFamily: fonts.body, fontSize: 11, lineHeight: 17 },
  imageEditor: { overflow: 'hidden', borderRadius: radius.card, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.lineStrong }, coverPreview: { position: 'relative', height: 150, justifyContent: 'flex-end', padding: 12, overflow: 'hidden' }, coverShade: { ...StyleSheet.absoluteFillObject, backgroundColor: 'rgba(0,0,0,.14)' }, coverLabel: { color: colors.white, fontFamily: fonts.body, fontSize: 13, fontWeight: '900' }, coverEdit: { position: 'absolute', right: 10, bottom: 10, minHeight: 36, paddingHorizontal: 11, flexDirection: 'row', alignItems: 'center', gap: 5, borderRadius: radius.control, backgroundColor: 'rgba(255,255,255,.94)' }, imageEditText: { color: colors.ink900, fontFamily: fonts.body, fontSize: 11, fontWeight: '800' }, profileRow: { minHeight: 88, paddingHorizontal: 12, flexDirection: 'row', alignItems: 'center', gap: 11, borderTopWidth: 1, borderTopColor: colors.line }, profilePreview: { width: 58, height: 58, borderRadius: 15, backgroundColor: colors.canvas }, profileCopy: { flex: 1, minWidth: 0 }, profileTitle: { color: colors.ink900, fontFamily: fonts.body, fontSize: 13, fontWeight: '900' }, profileDescription: { marginTop: 4, color: colors.ink500, fontFamily: fonts.body, fontSize: 10, lineHeight: 15 }, profileEdit: { minWidth: 52, minHeight: 40, alignItems: 'center', justifyContent: 'center', borderRadius: radius.control, backgroundColor: colors.canvas },
  label: { marginBottom: 7, color: colors.ink900, fontFamily: fonts.body, fontSize: 12, fontWeight: '800' },
  input: { height: 52, paddingHorizontal: 14, color: colors.ink900, fontFamily: fonts.body, fontSize: 15, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.lineStrong, borderRadius: radius.control },
  timeRow: { flexDirection: 'row', gap: 10 },
  timeField: { flex: 1 },
  categories: { flexDirection: 'row', flexWrap: 'wrap', gap: 7 },
  category: { minHeight: 40, paddingHorizontal: 13, alignItems: 'center', justifyContent: 'center', borderRadius: radius.pill, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.lineStrong },
  categoryActive: { borderColor: colors.green700, backgroundColor: colors.green50 },
  categoryText: { color: colors.ink700, fontFamily: fonts.body, fontSize: 12, fontWeight: '600' },
  categoryTextActive: { color: colors.green700, fontWeight: '800' },
  pressed: { opacity: .7 },
});
