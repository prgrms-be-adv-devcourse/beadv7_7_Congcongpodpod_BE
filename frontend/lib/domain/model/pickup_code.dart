import 'package:freezed_annotation/freezed_annotation.dart';

part 'pickup_code.freezed.dart';
part 'pickup_code.g.dart';

/// 픽업코드 정보. 백엔드 `PickupCodeResponse`(`GET /orders/{orderId}/pickupCode`)와 대응:
/// `{ orderId, dishName, pickupCode, pickupStartAt, pickupEndAt }`.
/// 본인 주문이면서 픽업 가능 상태(`PICKUP_READY`)일 때만 조회된다(ADR 013) — 그 외
/// 상태에서 호출하면 404가 나므로, 화면에서 상태를 먼저 확인하고 호출해야 한다.
@freezed
class PickupCode with _$PickupCode {
  const factory PickupCode({
    required int orderId,
    required String dishName,
    required String pickupCode,
    required String pickupStartAt,
    required String pickupEndAt,
  }) = _PickupCode;

  factory PickupCode.fromJson(Map<String, Object?> json) =>
      _$PickupCodeFromJson(json);
}
