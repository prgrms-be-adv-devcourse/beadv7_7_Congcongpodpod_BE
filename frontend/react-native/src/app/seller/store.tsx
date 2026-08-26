import { RoundedIcon as Ionicons } from '@/components/rounded-icon';
import { router } from 'expo-router';
import { useEffect, useState } from 'react';
import { Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import { OptimizedImage as Image } from '@/components/optimized-image';

import { LoadingState } from '@/components/loading-state';
import { PrimaryButton } from '@/components/page';
import { SellerShell } from '@/components/seller-shell';
import { colors, fonts, radius } from '@/constants/theme';
import { getStoreCoverImageSource, getStoreProfileImageSource } from '@/lib/food-image';
import { type GeocodingAddress, searchStoreAddresses } from '@/lib/geocoding';
import { showAppAlert } from '@/lib/app-overlay';
import { getMyStores, registerStore, registerStorePayoutAccount, updateStore, type StorePayoutAccount } from '@/lib/seller';
import { getStoreCategoryVisual, STORE_CATEGORY_KEYS, type StoreCategoryKey } from '@/lib/store-category';
import { useAuth } from '@/providers/auth-provider';

type Coordinates = { latitude: number; longitude: number };

const timePresets = [
  ['09:00', '21:00', '09–21시'],
  ['10:00', '22:00', '10–22시'],
  ['11:00', '23:00', '11–23시'],
] as const;

const formatBusinessNumber = (value: string) => {
  const digits = value.replace(/\D/g, '').slice(0, 10);
  return [digits.slice(0, 3), digits.slice(3, 5), digits.slice(5)].filter(Boolean).join('-');
};

const formatPhone = (value: string) => {
  const digits = value.replace(/\D/g, '').slice(0, 11);
  if (digits.startsWith('02')) {
    if (digits.length <= 2) return digits;
    if (digits.length <= 5) return `${digits.slice(0, 2)}-${digits.slice(2)}`;
    if (digits.length <= 9) return `${digits.slice(0, 2)}-${digits.slice(2, 5)}-${digits.slice(5)}`;
    return `${digits.slice(0, 2)}-${digits.slice(2, 6)}-${digits.slice(6)}`;
  }
  if (digits.length <= 3) return digits;
  if (digits.length <= 7) return `${digits.slice(0, 3)}-${digits.slice(3)}`;
  return `${digits.slice(0, 3)}-${digits.slice(3, digits.length - 4)}-${digits.slice(-4)}`;
};

const formatTime = (value: string) => {
  const digits = value.replace(/\D/g, '').slice(0, 4);
  return digits.length > 2 ? `${digits.slice(0, 2)}:${digits.slice(2)}` : digits;
};

export default function SellerStore() {
  const { member, refreshSession } = useAuth();
  const editing = member?.role === 'SELLER';
  const [storeId, setStoreId] = useState<number>();
  const [storeName, setStoreName] = useState('');
  const [businessNumber, setBusinessNumber] = useState('');
  const [storeAddress, setStoreAddress] = useState('');
  const [storeDetailAddress, setStoreDetailAddress] = useState('');
  const [storePhone, setStorePhone] = useState('');
  const [openTime, setOpenTime] = useState('09:00');
  const [closeTime, setCloseTime] = useState('21:00');
  const [category, setCategory] = useState<StoreCategoryKey>('KOREAN');
  const [coverImageUrl, setCoverImageUrl] = useState<string>();
  const [profileImageUrl, setProfileImageUrl] = useState<string>();
  const [coordinates, setCoordinates] = useState<Coordinates>();
  const [addressResults, setAddressResults] = useState<GeocodingAddress[]>([]);
  const [addressSearching, setAddressSearching] = useState(false);
  const [addressMessage, setAddressMessage] = useState('');
  const [loading, setLoading] = useState(editing);
  const [submitting, setSubmitting] = useState(false);
  const [bankName, setBankName] = useState('');
  const [accountNumber, setAccountNumber] = useState('');
  const [accountHolder, setAccountHolder] = useState('');
  const [accountSubmitting, setAccountSubmitting] = useState(false);
  const [registeredAccount, setRegisteredAccount] = useState<StorePayoutAccount>();

  useEffect(() => {
    if (!editing) return;
    void getMyStores()
      .then(([store]) => {
        if (!store) return;
        setStoreId(store.storeId);
        setStoreName(store.storeName);
        setBusinessNumber(store.businessNumber ?? '');
        setStoreAddress(store.address);
        setStoreDetailAddress(store.detailAddress ?? '');
        setStorePhone(store.phone ?? '');
        setOpenTime(store.openTime?.slice(0, 5) ?? '09:00');
        setCloseTime(store.closeTime?.slice(0, 5) ?? '21:00');
        if (STORE_CATEGORY_KEYS.includes(store.category as StoreCategoryKey)) setCategory(store.category as StoreCategoryKey);
        setCoverImageUrl(store.coverImageUrl);
        setProfileImageUrl(store.profileImageUrl);
        setCoordinates({ latitude: store.latitude, longitude: store.longitude });
      })
      .catch(() => showAppAlert('매장 정보를 불러오지 못했어요'))
      .finally(() => setLoading(false));
  }, [editing]);

  const changeAddress = (value: string) => {
    setStoreAddress(value);
    setCoordinates(undefined);
    setAddressResults([]);
    setAddressMessage(value.trim().length >= 2 ? '주소 검색을 눌러 정확한 위치를 선택해주세요.' : '');
  };

  const searchAddress = async () => {
    const query = storeAddress.trim();
    if (query.length < 2) {
      setAddressMessage('도로명이나 지번 주소를 두 글자 이상 입력해주세요.');
      return;
    }
    try {
      setAddressSearching(true);
      setAddressMessage('');
      setCoordinates(undefined);
      const results = await searchStoreAddresses(query);
      setAddressResults(results);
      if (results.length === 0) setAddressMessage('검색 결과가 없어요. 건물 번호까지 다시 입력해주세요.');
    } catch (error) {
      setAddressResults([]);
      setAddressMessage(error instanceof Error ? error.message : '주소를 검색하지 못했어요.');
    } finally {
      setAddressSearching(false);
    }
  };

  const selectAddress = (address: GeocodingAddress) => {
    setStoreAddress(address.roadAddress || address.jibunAddress);
    setCoordinates({ latitude: address.latitude, longitude: address.longitude });
    setAddressResults([]);
    setAddressMessage('');
  };

  const submit = async () => {
    if (!storeName.trim() || !storeAddress.trim() || !storePhone.trim() || (!editing && !businessNumber.trim())) {
      showAppAlert('필수 정보를 입력해주세요', '상점명, 주소, 전화번호와 사업자등록번호를 확인해주세요.');
      return;
    }
    if (!coordinates) {
      showAppAlert('매장 위치를 선택해주세요', '주소 검색 결과에서 정확한 주소를 선택해야 지도에 매장이 표시돼요.');
      return;
    }
    if (!/^([01]\d|2[0-3]):[0-5]\d$/.test(openTime) || !/^([01]\d|2[0-3]):[0-5]\d$/.test(closeTime)) {
      showAppAlert('영업시간을 확인해주세요', '09:00처럼 24시간 형식으로 입력해주세요.');
      return;
    }

    try {
      setSubmitting(true);
      const common = {
        storeName: storeName.trim(), storeAddress: storeAddress.trim(), storePhone: storePhone.trim(),
        ...(storeDetailAddress.trim() ? { storeDetailAddress: storeDetailAddress.trim() } : {}),
        openTime, closeTime, category, latitude: coordinates.latitude, longitude: coordinates.longitude, holidays: [],
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
        showAppAlert('상점 등록은 완료됐어요', '판매자 권한을 새로 불러오려면 다시 로그인해주세요.', [
          { text: '확인', onPress: () => router.replace('/my') },
        ]);
        return;
      }
      router.replace('/seller/home');
    } catch (error) {
      showAppAlert(editing ? '매장 정보를 수정하지 못했어요' : '상점을 등록하지 못했어요', error instanceof Error ? error.message : '잠시 후 다시 시도해주세요.');
    } finally {
      setSubmitting(false);
    }
  };

  const submitPayoutAccount = async () => {
    if (!storeId || !bankName.trim() || !accountNumber.trim() || !accountHolder.trim()) {
      showAppAlert('계좌 정보를 확인해주세요', '은행명, 계좌번호와 예금주를 모두 입력해주세요.');
      return;
    }
    try {
      setAccountSubmitting(true);
      const account = await registerStorePayoutAccount(storeId, {
        bankName: bankName.trim(),
        accountNumber: accountNumber.replace(/\D/g, ''),
        accountHolder: accountHolder.trim(),
      });
      setRegisteredAccount(account);
      setAccountNumber('');
      showAppAlert('정산 계좌를 등록했어요', `${account.bankName} ${account.accountNumber}\n예금주 ${account.accountHolder}`);
    } catch (error) {
      showAppAlert('정산 계좌를 등록하지 못했어요', error instanceof Error ? error.message : '잠시 후 다시 시도해주세요.');
    } finally {
      setAccountSubmitting(false);
    }
  };

  if (loading) return <SellerShell back title="매장 정보" description="등록된 정보를 확인하고 있어요."><LoadingState compact label="매장 정보를 불러오고 있어요"/></SellerShell>;

  return (
    <SellerShell back title={editing ? '매장 정보 관리' : '우리 가게 등록하기'} description={editing ? '고객에게 보여줄 매장 정보와 위치를 관리하세요.' : '정확한 정보를 입력하면 등록 후 바로 상품을 준비할 수 있어요.'} storeName={editing ? storeName || '내 매장' : '미등록'}>
      {!editing ? <View style={styles.roleNotice}><View style={styles.noticeCopy}><Text style={styles.noticeTitle}>등록 완료 후 판매자 메뉴가 열려요</Text><Text style={styles.noticeBody}>주소 위치까지 확인한 뒤 등록하면 상품·주문·정산을 관리할 수 있어요.</Text></View></View> : null}

      <View style={styles.formSection}>
        <SectionHeader title="매장 이미지" description="커버는 상세 화면, 프로필은 지도와 목록에서 사용돼요."/>
        <View style={styles.imageEditor}><View style={styles.coverPreview}><Image source={getStoreCoverImageSource({ category, coverImageUrl })} style={StyleSheet.absoluteFillObject}/><View style={styles.coverShade}/><Text style={styles.coverLabel}>매장 커버</Text><Pressable accessibilityRole="button" onPress={() => showAppAlert('이미지 업로드 준비 중', '이미지 저장 API가 연결되기 전에는 카테고리 기본 이미지를 사용해요.')} style={styles.coverEdit}><Ionicons name="camera-outline" size={16} color={colors.ink900}/><Text style={styles.imageEditText}>커버 변경</Text></Pressable></View><View style={styles.profileRow}><Image source={getStoreProfileImageSource({ category, profileImageUrl })} style={styles.profilePreview}/><View style={styles.profileCopy}><Text style={styles.profileTitle}>상점 프로필</Text><Text style={styles.profileDescription}>고객이 매장을 빠르게 알아보는 대표 이미지예요.</Text></View><Pressable accessibilityRole="button" onPress={() => showAppAlert('이미지 업로드 준비 중', '이미지 저장 API가 연결되기 전에는 카테고리 기본 이미지를 사용해요.')} style={styles.profileEdit}><Text style={styles.imageEditText}>변경</Text></Pressable></View></View>
      </View>

      <View style={styles.formSection}>
        <SectionHeader title="기본 정보" description="사업자 정보와 고객에게 표시될 매장명을 입력해주세요."/>
        <LabeledField label="상점명" value={storeName} onChangeText={setStoreName} placeholder="예: 남부터미널 라디키친" maxLength={40}/>
        {!editing ? <LabeledField label="사업자등록번호" value={businessNumber} onChangeText={(value) => setBusinessNumber(formatBusinessNumber(value))} placeholder="000-00-00000" keyboardType="number-pad" maxLength={12}/> : null}
        <LabeledField label="전화번호" value={storePhone} onChangeText={(value) => setStorePhone(formatPhone(value))} placeholder="02-0000-0000" keyboardType="phone-pad" maxLength={13}/>
      </View>

      <View style={styles.formSection}>
        <SectionHeader title="매장 위치" description="주소를 검색하고 정확한 후보를 선택하면 지도 좌표가 자동 저장돼요."/>
        <View style={styles.field}><Text style={styles.fieldLabel}>도로명 또는 지번 주소</Text><View style={styles.addressRow}><TextInput value={storeAddress} onChangeText={changeAddress} onSubmitEditing={() => void searchAddress()} placeholder="예: 서울 서초구 효령로 292" placeholderTextColor={colors.ink400} returnKeyType="search" style={[styles.input, styles.addressInput]}/><Pressable accessibilityRole="button" disabled={addressSearching} onPress={() => void searchAddress()} style={({ pressed }) => [styles.searchButton, (pressed || addressSearching) && styles.pressed]}><Ionicons name={addressSearching ? 'hourglass-outline' : 'search'} size={18} color={colors.white}/><Text style={styles.searchButtonText}>{addressSearching ? '검색 중' : '주소 검색'}</Text></Pressable></View></View>
        {addressMessage ? <View style={styles.addressMessage}><Ionicons name="information-circle-outline" size={16} color={colors.ink500}/><Text style={styles.addressMessageText}>{addressMessage}</Text></View> : null}
        {addressResults.length > 0 ? <View style={styles.addressResults}>{addressResults.map((address, index) => <Pressable key={`${address.latitude}-${address.longitude}-${index}`} accessibilityRole="button" onPress={() => selectAddress(address)} style={({ pressed }) => [styles.addressCandidate, pressed && styles.candidatePressed]}><View style={styles.candidateCopy}><Text style={styles.roadAddress}>{address.roadAddress || address.jibunAddress}</Text>{address.roadAddress && address.jibunAddress ? <Text style={styles.jibunAddress}>지번 {address.jibunAddress}</Text> : null}</View><Ionicons name="chevron-forward" size={17} color={colors.ink400}/></Pressable>)}</View> : null}
        {coordinates ? <View style={styles.addressConfirmed}><View style={styles.confirmCopy}><Text style={styles.confirmEyebrow}>지도 위치 확인 완료</Text><Text style={styles.confirmAddress}>{storeAddress}</Text><Text style={styles.coordinates}>{coordinates.latitude.toFixed(6)}, {coordinates.longitude.toFixed(6)}</Text></View></View> : null}
        <LabeledField label="상세주소 (선택)" value={storeDetailAddress} onChangeText={setStoreDetailAddress} placeholder="예: 2층 201호, 정문 오른쪽" maxLength={80}/>
      </View>

      <View style={styles.formSection}>
        <SectionHeader title="영업시간" description="고객이 픽업 가능한 매장 운영시간을 설정해주세요."/>
        <View style={styles.fieldRow}><View style={styles.flexField}><LabeledField label="영업 시작" value={openTime} onChangeText={(value) => setOpenTime(formatTime(value))} placeholder="09:00" keyboardType="number-pad" maxLength={5}/></View><View style={styles.timeArrow}><Ionicons name="arrow-forward" size={16} color={colors.ink400}/></View><View style={styles.flexField}><LabeledField label="영업 종료" value={closeTime} onChangeText={(value) => setCloseTime(formatTime(value))} placeholder="21:00" keyboardType="number-pad" maxLength={5}/></View></View>
        <View style={styles.quickRow}>{timePresets.map(([start, end, label]) => { const selected = openTime === start && closeTime === end; return <Pressable key={label} onPress={() => { setOpenTime(start); setCloseTime(end); }} style={[styles.quickButton, selected && styles.quickButtonActive]}><Text style={[styles.quickText, selected && styles.quickTextActive]}>{label}</Text></Pressable>; })}</View>
      </View>

      <View style={styles.formSection}>
        <SectionHeader title="매장 카테고리" description="지도 마커와 목록 필터에 사용될 대표 업종을 하나 선택해주세요."/>
        <View accessibilityRole="radiogroup" style={styles.categories}>{STORE_CATEGORY_KEYS.map((key) => { const visual = getStoreCategoryVisual(key); const selected = category === key; return <Pressable key={key} accessibilityRole="radio" accessibilityState={{ selected }} onPress={() => setCategory(key)} style={({ pressed }) => [styles.category, selected && styles.categoryActive, pressed && styles.pressed]}><Text style={[styles.categoryText, selected && styles.categoryTextActive]}>{visual.label}</Text></Pressable>; })}</View>
      </View>

      {editing && storeId ? <View style={styles.formSection}>
        <SectionHeader title="정산 계좌" description="판매 대금 정산에 사용할 본인 명의 계좌를 등록해주세요."/>
        {registeredAccount ? <View style={styles.accountConfirmed}><View style={styles.accountConfirmedCopy}><Text style={styles.accountConfirmedLabel}>등록 완료</Text><Text style={styles.accountConfirmedValue}>{registeredAccount.bankName} · {registeredAccount.accountNumber}</Text><Text style={styles.accountConfirmedHolder}>예금주 {registeredAccount.accountHolder}</Text></View><Ionicons name="checkmark-circle-outline" size={22} color={colors.green700}/></View> : <>
          <LabeledField label="은행명" value={bankName} onChangeText={setBankName} placeholder="예: 국민은행" maxLength={20}/>
          <LabeledField label="계좌번호" value={accountNumber} onChangeText={(value) => setAccountNumber(value.replace(/\D/g, '').slice(0, 20))} placeholder="숫자만 입력해주세요" keyboardType="number-pad" maxLength={20}/>
          <LabeledField label="예금주" value={accountHolder} onChangeText={setAccountHolder} placeholder="예: 홍길동" maxLength={30}/>
          <View style={styles.accountNotice}><Ionicons name="information-circle-outline" size={16} color={colors.ink500}/><Text style={styles.accountNoticeText}>등록한 계좌는 정산 처리에 사용되며 계좌번호는 이후 마스킹되어 표시됩니다.</Text></View>
          <Pressable accessibilityRole="button" disabled={accountSubmitting} onPress={() => void submitPayoutAccount()} style={({ pressed }) => [styles.accountButton, (pressed || accountSubmitting) && styles.pressed]}><Text style={styles.accountButtonText}>{accountSubmitting ? '등록하는 중…' : '정산 계좌 등록하기'}</Text></Pressable>
        </>}
      </View> : null}

      <PrimaryButton disabled={submitting} label={submitting ? (editing ? '저장하는 중…' : '등록하는 중…') : (editing ? '매장 정보 저장하기' : '상점 등록하고 판매 시작하기')} onPress={() => void submit()}/>
    </SellerShell>
  );
}

function SectionHeader({ title, description }: { title: string; description: string }) {
  return <View style={styles.sectionHeader}><View style={styles.sectionCopy}><Text style={styles.sectionTitle}>{title}</Text><Text style={styles.sectionDescription}>{description}</Text></View></View>;
}

function LabeledField({ label, ...props }: { label: string; value: string; onChangeText: (value: string) => void; placeholder: string; keyboardType?: 'default' | 'number-pad' | 'phone-pad'; maxLength?: number }) {
  return <View style={styles.field}><Text style={styles.fieldLabel}>{label}</Text><TextInput {...props} placeholderTextColor={colors.ink400} style={styles.input}/></View>;
}

const styles = StyleSheet.create({
  roleNotice: { padding: 14, flexDirection: 'row', gap: 11, borderRadius: radius.input, backgroundColor: colors.green50, borderWidth: 1, borderColor: colors.green200 },
  noticeCopy: { flex: 1 }, noticeTitle: { color: colors.ink900, fontFamily: fonts.body, fontSize: 13, fontWeight: '900' }, noticeBody: { marginTop: 4, color: colors.ink700, fontFamily: fonts.body, fontSize: 11, lineHeight: 17 },
  formSection: { padding: 14, gap: 12, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.lineStrong, borderRadius: radius.card },
  sectionHeader: { flexDirection: 'row', alignItems: 'center' }, sectionCopy: { flex: 1, gap: 2 }, sectionTitle: { color: colors.ink900, fontFamily: fonts.body, fontSize: 15, fontWeight: '900' }, sectionDescription: { color: colors.ink500, fontFamily: fonts.body, fontSize: 10, lineHeight: 15 },
  imageEditor: { overflow: 'hidden', borderRadius: radius.input, backgroundColor: colors.white, borderWidth: 1, borderColor: colors.line }, coverPreview: { position: 'relative', height: 170, justifyContent: 'flex-end', padding: 12, overflow: 'hidden' }, coverShade: { ...StyleSheet.absoluteFillObject, backgroundColor: 'rgba(0,0,0,.18)' }, coverLabel: { color: colors.white, fontFamily: fonts.body, fontSize: 13, fontWeight: '900' }, coverEdit: { position: 'absolute', right: 12, bottom: 12, minHeight: 40, paddingHorizontal: 12, flexDirection: 'row', alignItems: 'center', gap: 6, borderRadius: radius.control, backgroundColor: 'rgba(255,255,255,.94)' }, imageEditText: { color: colors.ink900, fontFamily: fonts.body, fontSize: 11, fontWeight: '900' },
  profileRow: { minHeight: 90, padding: 12, flexDirection: 'row', alignItems: 'center', gap: 11, borderTopWidth: 1, borderTopColor: colors.line }, profilePreview: { width: 58, height: 58, borderRadius: 15, backgroundColor: colors.canvas }, profileCopy: { flex: 1, minWidth: 0 }, profileTitle: { color: colors.ink900, fontFamily: fonts.body, fontSize: 13, fontWeight: '900' }, profileDescription: { marginTop: 4, color: colors.ink500, fontFamily: fonts.body, fontSize: 10, lineHeight: 15 }, profileEdit: { minWidth: 52, minHeight: 40, alignItems: 'center', justifyContent: 'center', borderRadius: radius.control, backgroundColor: colors.canvas },
  field: { gap: 6 }, fieldLabel: { color: colors.ink700, fontFamily: fonts.body, fontSize: 11, fontWeight: '800' }, input: { minHeight: 50, paddingHorizontal: 13, color: colors.ink900, fontFamily: fonts.body, fontSize: 15, backgroundColor: colors.canvasWarm, borderWidth: 1, borderColor: colors.line, borderRadius: radius.input },
  addressRow: { flexDirection: 'row', gap: 8 }, addressInput: { flex: 1, minWidth: 0 }, searchButton: { minWidth: 92, minHeight: 50, paddingHorizontal: 12, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 5, borderRadius: radius.input, backgroundColor: colors.ink900 }, searchButtonText: { color: colors.white, fontFamily: fonts.body, fontSize: 11, fontWeight: '900' },
  addressConfirmed: { padding: 12, flexDirection: 'row', alignItems: 'center', borderRadius: radius.input, backgroundColor: colors.green50, borderWidth: 1, borderColor: colors.green200 }, confirmCopy: { flex: 1, minWidth: 0 }, confirmEyebrow: { color: colors.green700, fontFamily: fonts.body, fontSize: 9, fontWeight: '900' }, confirmAddress: { marginTop: 2, color: colors.ink900, fontFamily: fonts.body, fontSize: 12, fontWeight: '800' }, coordinates: { marginTop: 2, color: colors.ink500, fontFamily: fonts.body, fontSize: 9, fontVariant: ['tabular-nums'] },
  addressMessage: { minHeight: 40, paddingHorizontal: 11, flexDirection: 'row', alignItems: 'center', gap: 7, borderRadius: radius.control, backgroundColor: colors.canvas }, addressMessageText: { flex: 1, color: colors.ink700, fontFamily: fonts.body, fontSize: 10, lineHeight: 15 }, addressResults: { overflow: 'hidden', borderWidth: 1, borderColor: colors.lineStrong, borderRadius: radius.input }, addressCandidate: { minHeight: 68, paddingHorizontal: 11, flexDirection: 'row', alignItems: 'center', gap: 9, backgroundColor: colors.white, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: colors.line }, candidatePressed: { backgroundColor: colors.green50 }, candidateCopy: { flex: 1, minWidth: 0 }, roadAddress: { color: colors.ink900, fontFamily: fonts.body, fontSize: 12, fontWeight: '800' }, jibunAddress: { marginTop: 4, color: colors.ink500, fontFamily: fonts.body, fontSize: 10 },
  fieldRow: { flexDirection: 'row', alignItems: 'flex-end', gap: 9 }, flexField: { flex: 1, minWidth: 0 }, timeArrow: { width: 22, height: 50, alignItems: 'center', justifyContent: 'center' }, quickRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 7 }, quickButton: { minHeight: 36, paddingHorizontal: 12, alignItems: 'center', justifyContent: 'center', borderWidth: 1, borderColor: colors.line, borderRadius: radius.pill, backgroundColor: colors.canvasWarm }, quickButtonActive: { borderColor: colors.ink900, backgroundColor: colors.ink900 }, quickText: { color: colors.ink700, fontFamily: fonts.body, fontSize: 10, fontWeight: '800' }, quickTextActive: { color: colors.white },
  categories: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 }, category: { minHeight: 40, paddingHorizontal: 12, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 5, borderRadius: radius.control, backgroundColor: colors.canvasWarm, borderWidth: 1, borderColor: colors.line }, categoryActive: { borderColor: colors.ink900, backgroundColor: colors.ink900 }, categoryText: { color: colors.ink700, fontFamily: fonts.body, fontSize: 11, fontWeight: '800' }, categoryTextActive: { color: colors.white }, pressed: { opacity: 0.66, transform: [{ scale: 0.99 }] },
  accountNotice: { minHeight: 42, paddingHorizontal: 11, flexDirection: 'row', alignItems: 'center', gap: 7, borderRadius: radius.control, backgroundColor: colors.canvas }, accountNoticeText: { flex: 1, color: colors.ink700, fontFamily: fonts.body, fontSize: 10, lineHeight: 15 }, accountButton: { minHeight: 48, alignItems: 'center', justifyContent: 'center', borderRadius: radius.input, backgroundColor: colors.ink900 }, accountButtonText: { color: colors.white, fontFamily: fonts.body, fontSize: 13, fontWeight: '900' }, accountConfirmed: { minHeight: 78, padding: 13, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 12, borderRadius: radius.input, backgroundColor: colors.green50, borderWidth: 1, borderColor: colors.green200 }, accountConfirmedCopy: { flex: 1, minWidth: 0 }, accountConfirmedLabel: { color: colors.green700, fontFamily: fonts.body, fontSize: 10, fontWeight: '900' }, accountConfirmedValue: { marginTop: 4, color: colors.ink900, fontFamily: fonts.body, fontSize: 14, fontWeight: '900', fontVariant: ['tabular-nums'] }, accountConfirmedHolder: { marginTop: 3, color: colors.ink500, fontFamily: fonts.body, fontSize: 10 },
});
