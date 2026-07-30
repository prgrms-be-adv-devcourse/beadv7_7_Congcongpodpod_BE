import 'package:freezed_annotation/freezed_annotation.dart';

part 'payment.freezed.dart';
part 'payment.g.dart';

/// 결제 준비 응답. 백엔드 `PaymentReadyResponse`(`POST /payments`)와 대응:
/// `{ paymentId, merchantOrderId, amount, approvedStatus, tossClientKey }`.
/// Deposit과 같은 이유로 `ApiResponse` 래퍼가 없다(api-contracts.md 0절, 2026-07-28
/// Payment 섹션 신규 추가분) — repository가 `response.data`를 그대로 파싱한다.
///
/// `merchantOrderId`가 Toss `requestPayment`의 `orderId`로, 그리고 승인(`approve`)
/// 요청의 `orderId`로 그대로 이어진다 — 백엔드가 발급한 값이지 주문(Order)의
/// 숫자 PK가 아니다.
@freezed
class PaymentReady with _$PaymentReady {
  const factory PaymentReady({
    required int paymentId,
    required String merchantOrderId,
    required num amount,
    required String approvedStatus,
    required String tossClientKey,
  }) = _PaymentReady;

  factory PaymentReady.fromJson(Map<String, Object?> json) =>
      _$PaymentReadyFromJson(json);
}

/// 결제 승인 응답. 백엔드 `PaymentApproveResponse`(`POST /payments/approve`)와 대응:
/// `{ paymentId, merchantOrderId, amount, approvedStatus, approvedAt, depositBalance }`.
/// 승인 성공 시 예치금 충전까지 서버에서 한 번에 처리되므로 `depositBalance`가
/// 충전 후 최종 잔액이다.
@freezed
class PaymentApprove with _$PaymentApprove {
  const factory PaymentApprove({
    required int paymentId,
    required String merchantOrderId,
    required num amount,
    required String approvedStatus,
    required String approvedAt,
    required num depositBalance,
  }) = _PaymentApprove;

  factory PaymentApprove.fromJson(Map<String, Object?> json) =>
      _$PaymentApproveFromJson(json);
}
