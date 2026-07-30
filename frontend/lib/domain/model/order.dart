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
    // null 허용: 백엔드 Dish.create()가 pickupStartTime/pickupEndTime을 저장하는
    // 코드 경로 자체가 없어서(2026-07-30 발견), 지금 생성되는 모든 주문의 이 값이
    // 항상 null로 내려온다. required로 두면 Order.fromJson() 자체가 TypeError를
    // 던져서 주문 성공 다이얼로그·주문목록이 통째로 깨진다 — 근본 수정(Dish 등록 시
    // 픽업시간 저장)은 범위가 커서 일단 null을 그대로 받아들이게 완화한다.
    String? pickupStartAt,
    String? pickupEndAt,
  }) = _Order;

  factory Order.fromJson(Map<String, Object?> json) => _$OrderFromJson(json);
}
