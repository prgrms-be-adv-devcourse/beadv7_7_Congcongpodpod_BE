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
    // 원 단위 정수로 가정(금액은 원 단위, 소수점 없음).
    // 백엔드가 소수점 있는 값을 내려주면(BigDecimal이라 이론상 가능) 이 타입을 num으로 바꿔야 한다.
    required int unitPrice,
    required int quantity,
    required int subtotalPrice,
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
    required int totalPrice,
  }) = _Cart;

  factory Cart.fromJson(Map<String, Object?> json) => _$CartFromJson(json);
}
