import '../model/cart.dart';

/// 장바구니 기능 계약. 실제 dio 호출은 lib/data/repository/cart_repository_impl.dart가 담당한다.
abstract interface class CartRepository {
  /// 로그인한 회원의 장바구니를 가져온다. memberId를 파라미터로 받지 않는 이유:
  /// Order 계열 API처럼 Gateway가 `X-Authenticated-Member-Id` 헤더로 인증된 회원을
  /// 식별해줄 것으로 가정하고 설계했다 — 지금 실제 서버는 아직 이 방식이 아닐 수 있다
  /// (cart_repository_impl.dart 참고).
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
