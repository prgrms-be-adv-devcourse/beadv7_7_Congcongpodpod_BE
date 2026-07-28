import 'package:freezed_annotation/freezed_annotation.dart';

part 'cart.freezed.dart';
part 'cart.g.dart';

/// 장바구니 안의 상품 1건. 백엔드 `CartItemResponse`와 대응:
/// `{ cartItemId, dishId, dishName, unitPrice, quantity, subtotalPrice }`.
///
/// 장바구니는 상품 1개 단위로 단순화돼 있어, `Cart.items`는 항상 0건 또는 1건이다 —
/// 여러 개를 다루는 UI(수량 합산, 리스트 스크롤 등)는 만들지 않는다.
@freezed
class CartItem with _$CartItem {
  const factory CartItem({
    required int cartItemId,
    required int dishId,
    required String dishName,
    // 2026-07-27 실제 로컬 연동 테스트로 확인: 백엔드가 `BigDecimal`이라 실제로
    // "10000.00"처럼 소수점을 붙여 내려온다(int로 받으면 파싱 시 캐스팅 에러) — num으로 수정.
    required num unitPrice,
    required int quantity,
    required num subtotalPrice,
  }) = _CartItem;

  factory CartItem.fromJson(Map<String, Object?> json) =>
      _$CartItemFromJson(json);
}

/// 장바구니 전체. 백엔드 `CartResponse{ cartId, memberId, items[], totalPrice }`와 대응.
@freezed
class Cart with _$Cart {
  const factory Cart({
    required int cartId,
    required int memberId,
    required List<CartItem> items,
    required num totalPrice,
  }) = _Cart;

  factory Cart.fromJson(Map<String, Object?> json) => _$CartFromJson(json);
}
