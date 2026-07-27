/// 장바구니 기능 전용 에러.
sealed class CartException implements Exception {
  const CartException(this.message);

  final String message;

  @override
  String toString() => message;
}

final class CartOutOfStockException extends CartException {
  const CartOutOfStockException([super.message = '재고가 부족합니다']);
}

final class CartNotFoundException extends CartException {
  const CartNotFoundException([super.message = '장바구니를 찾을 수 없습니다']);
}
