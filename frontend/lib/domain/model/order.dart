import 'package:freezed_annotation/freezed_annotation.dart';

part 'order.freezed.dart';
part 'order.g.dart';

/// 주문 정보. 백엔드 `OrderResponse`와 대응:
/// `{ orderId, memberId, storeId, status, rejectReason, paymentStatus, phone,
///   dishId, dishName, quantity, unitPrice, totalPrice, pickupStartAt, pickupEndAt }`.
///
/// 지금은 주문 생성(체크아웃, B7) 결과 확인용으로만 쓴다 — 내 주문목록(B8)은 아직
/// 목업이라 이 모델을 안 씀(연동되면 그때 재사용).
@freezed
class Order with _$Order {
  const factory Order({
    required int orderId,
    required int memberId,
    required int storeId,
    // OrderStatus: RESERVED, PICKUP_READY, PICKED_UP, NO_SHOW, CANCELLED, REJECTED(ADR 014).
    // Store/Dish의 status/dishStatus와 같은 이유로 String 그대로 받는다.
    required String status,
    String? rejectReason,
    required String paymentStatus,
    required String phone,
    required int dishId,
    required String dishName,
    required int quantity,
    required num unitPrice,
    required num totalPrice,
    // "HH:mm:ss" 문자열 그대로 — store.dart의 openTime/closeTime과 같은 이유.
    required String pickupStartAt,
    required String pickupEndAt,
  }) = _Order;

  factory Order.fromJson(Map<String, Object?> json) => _$OrderFromJson(json);
}
