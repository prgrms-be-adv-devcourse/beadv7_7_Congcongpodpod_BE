/// `toss_payments_client.dart`가 웹이 아닌 플랫폼에서 조건부 임포트하는 대체 구현.
/// `dart:js_interop`은 웹 전용이라, Android/iOS/데스크톱 빌드가 이 파일을 컴파일한다
/// (지금은 실제로 호출될 일이 없다 — 예치금 충전 결제는 웹 배포 기준으로만 만들었다).
class TossPaymentsWebClient {
  static Future<void> requestCardPayment({
    required String tossClientKey,
    required String merchantOrderId,
    required num amount,
    required String orderName,
    required String successUrl,
    required String failUrl,
    required String customerEmail,
    required String customerName,
  }) {
    throw UnsupportedError('Toss Payments 결제창 연동은 Flutter Web에서만 지원한다.');
  }
}
