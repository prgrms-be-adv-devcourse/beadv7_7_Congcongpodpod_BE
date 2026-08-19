import { router, useLocalSearchParams } from 'expo-router';
import { Page, Panel, PrimaryButton, Row } from '@/components/page';
export default function ChargeFail() { const { message, amount } = useLocalSearchParams<{ message?: string; amount?: string }>(); return <Page title="결제를 완료하지 못했어요" description={message ?? '결제 수단의 승인 상태를 확인하세요.'}><Panel tone="yellow"><Row label="처리 결과" value="승인 실패" /><Row label="결제 금액" value={`${Number(amount ?? 0).toLocaleString()}원`} strong /></Panel><PrimaryButton label="다시 시도" onPress={() => router.replace('/deposits/charge')} /></Page>; }
