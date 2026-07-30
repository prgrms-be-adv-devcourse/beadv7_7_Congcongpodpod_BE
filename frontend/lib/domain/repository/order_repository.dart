import '../model/order.dart';
import '../model/pickup_code.dart';

/// 주문 관련 기능 계약. 실제 dio 호출은 lib/data/repository/order_repository_impl.dart가 담당한다.
abstract interface class OrderRepository {
  /// 주문 생성(결제 포함) — `POST /orders/cartItems/{cartItemId}`. 바디 없음 — 장바구니
  /// 아이템을 서버가 그대로 스냅샷해서 주문을 만든다(storeId/dishId/dishName/quantity/
  /// unitPrice/pickupStartAt/pickupEndAt 전부 서버가 채움, phone도 회원정보 내부 조회로
  /// 채워짐 — 2026-07-28 백엔드 계약 변경, PR #116/#130). 성공하는 즉시 예치금이
  /// 차감된다(ADR 001). 예치금 부족(`DEP001`), 재고 부족(`D003`) 등은 [AppException]으로
  /// 정규화돼서 던져진다.
  Future<Order> createOrder({required int cartItemId});

  /// 내 주문 목록 (`GET /orders?status=`). `status`가 null이면 전체(쿼리 파라미터 자체를 안 보냄).
  /// 페이지네이션 UI는 아직 없어서 첫 페이지(기본 size)만 가져온다 — store_repository와 같은 이유.
  Future<List<Order>> getMyOrders({String? status});

  /// 주문 단건 조회 (`GET /orders/{orderId}`).
  Future<Order> getOrder(int orderId);

  /// 주문 취소 (`PATCH /orders/{orderId}/cancel`). 요청 바디 없음 — 2026-07-27 실제 코드
  /// 확인 결과 `cancelReason`을 안 받는 걸로 이미 구현돼 있음(2026-07-26 PO 확정:
  /// 구매자는 취소 사유를 고르지 않는다는 결정이 API에도 그대로 반영됨).
  Future<Order> cancelOrder(int orderId);

  /// 픽업코드 조회 (`GET /orders/{orderId}/pickupCode`). 본인 주문이면서 픽업 가능
  /// 상태(`PICKUP_READY`)가 아니면 404(`ORD001` 계열)가 난다.
  Future<PickupCode> getPickupCode(int orderId);

  /// 매장별 주문 목록 (`GET /orders/stores/{storeId}?status=`, SELLER 전용, S3).
  Future<List<Order>> getStoreOrders({required int storeId, String? status});

  /// 매장 주문 접수 (`POST /orders/{orderId}/accept`, SELLER 전용) —
  /// `RESERVED → PICKUP_READY` + 픽업코드 발급. 응답(`OrderReceptionResponse`)은
  /// 화면에서 안 쓰고 목록 재조회로 갱신하므로 반환값 없이 성공 여부만 알리면 된다.
  Future<void> acceptOrder(int orderId);

  /// 매장 주문 거절 (`POST /orders/{orderId}/reject`, SELLER 전용) —
  /// `RESERVED → REJECTED`. `reason`은 `OrderRejectReason`(order_reject_reason.dart) 값.
  Future<void> rejectOrder({required int orderId, required String reason});

  /// 픽업 상태 변경 (`PATCH /orders/{orderId}/pickup`, SELLER 전용) —
  /// `PICKUP_READY → PICKED_UP`(픽업완료) 또는 `PICKUP_READY → NO_SHOW`(노쇼처리)만 허용.
  /// `PICKUP_READY` 자체는 이 API가 아니라 accept로 만들어진다(백엔드 `OrderService`
  /// switch문이 `PICKED_UP`/`NO_SHOW`만 처리 — 2026-07-27 소스 확인).
  Future<void> updatePickupStatus({
    required int orderId,
    required String status,
  });
}
