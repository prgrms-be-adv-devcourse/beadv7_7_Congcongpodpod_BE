import '../model/payment.dart';

/// 예치금 충전 결제(PG) 기능 계약. 실제 dio 호출은
/// lib/data/repository/payment_repository_impl.dart가 담당한다.
///
/// 결제는 예치금 충전 전용이다(ADR 001) — 주문 결제는 예치금 차감으로 처리되고
/// 이 컨텍스트를 거치지 않는다.
abstract interface class PaymentRepository {
  /// 결제 준비 (`POST /payments`). Toss 결제창을 띄우기 전에 먼저 호출해서
  /// `tossClientKey`와 `merchantOrderId`를 받아온다.
  Future<PaymentReady> ready({required num amount});

  /// 결제 승인 (`POST /payments/approve`). Toss 결제창이 `successUrl`로 리다이렉트하며
  /// 넘겨준 `paymentKey`/`orderId`/`amount`를 그대로 전달한다. 승인되면 예치금이 충전된다.
  Future<PaymentApprove> approve({
    required String paymentKey,
    required String orderId,
    required num amount,
  });
}
