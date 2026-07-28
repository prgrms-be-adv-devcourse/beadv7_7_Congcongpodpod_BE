import '../model/cart.dart';

/// 장바구니 기능 계약. 실제 dio 호출은 lib/data/repository/cart_repository_impl.dart가 담당한다.
abstract interface class CartRepository {
  /// 로그인한 회원의 장바구니를 가져온다. memberId를 파라미터로 받지 않는 이유:
  /// 실제 API(`GET /carts/members/{memberId}`)는 memberId가 URL에 필요하지만, 이 인터페이스
  /// 계약 자체엔 안 두고 구현체(`cart_repository_impl.dart`)가 내부적으로 `/members/me`를
  /// 호출해 알아낸다 — 나중에 Order처럼 Gateway 헤더 기반(`X-Authenticated-Member-Id`)으로
  /// 바뀌어도 이 인터페이스와 호출부(화면)는 안 바뀐다, 구현체 한 곳만 고치면 된다.
  /// (2026-07-27 임시 우회 — 백엔드 헤더 기반 작업 진행 중, 완료되면 구현만 교체 예정.
  /// `aru-workspace-docs/lastdish/frontend/decisions/adr-draft-cart-member-id-resolution.md` 참고.)
  Future<Cart> getMyCart();

  /// 장바구니에 상품을 담는다. 이미 담긴 상품이 있으면 백엔드가 교체(upsert)한다
  /// (기존 CartItem 있으면 교체, 없으면 삽입). 지금은 이 메서드를 호출하는
  /// 화면(상품상세, B5)이 아직 없어 레포지토리 계약으로만 존재한다 — B5가 만들어지면 바로 연결.
  Future<CartItem> addItem({
    required int cartId,
    required int dishId,
    required int quantity,
  });

  /// 담긴 상품의 수량을 바꾼다. quantity는 1 이상이어야 한다(백엔드 제약).
  Future<CartItem> updateItemQuantity({
    required int cartId,
    required int itemId,
    required int quantity,
  });

  /// 담긴 상품 하나를 뺀다(장바구니 자체는 남는다).
  Future<void> removeItem({required int cartId, required int itemId});

  /// 장바구니를 통째로 비운다(장바구니 엔티티 자체는 삭제되지 않음).
  Future<void> clearCart(int cartId);
}
