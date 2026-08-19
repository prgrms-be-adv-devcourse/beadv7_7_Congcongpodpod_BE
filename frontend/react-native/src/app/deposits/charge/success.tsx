import { router, useLocalSearchParams } from 'expo-router';
import { Page, Panel, PrimaryButton, Row } from '@/components/page';
export default function ChargeSuccess() { const { amount, balance } = useLocalSearchParams<{ amount?: string; balance?: string }>(); return <Page title="충전이 완료됐어요" description="예치금에 즉시 반영되었습니다."><Panel tone="green"><Row label="충전 금액" value={`${Number(amount ?? 0).toLocaleString()}원`} /><Row label="현재 잔액" value={`${Number(balance ?? 0).toLocaleString()}원`} strong /></Panel><PrimaryButton label="잔액 확인" onPress={() => router.replace('/deposits')} /></Page>; }
