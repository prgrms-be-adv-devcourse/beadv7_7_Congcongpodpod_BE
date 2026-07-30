/// 라우트 경로 상수.
/// 이번 주말 목표 범위는 login~cart, 스트레치로 checkout.
abstract final class RoutePaths {
  // 이번 주말 목표 범위
  static const login = '/login';
  static const signup = '/signup';
  static const home = '/';
  static const storeDetail = '/stores/:storeId';
  static const dishDetail = '/dishes/:dishId';
  static const cart = '/cart';
  static const checkout = '/cart/checkout'; // 스트레치

  // 기능 동결(7/28)까지 순연 — screens.md B8~B13
  static const orders = '/orders';
  static const orderDetail = '/orders/:orderId';
  static const orderCancel = '/orders/:orderId/cancel';
  static const deposits = '/deposits';
  static const me = '/me';
  static const orderPickup = '/orders/:orderId/pickup';
  static const depositCharge = '/deposits/charge';
  static const depositChargeSuccess = '/deposits/charge/success';
  static const depositChargeFail = '/deposits/charge/fail';
  static const sellerVerify = '/seller/verify';
  static const sellerDishes = '/seller/dishes';
  static const sellerStore = '/seller/store';
  static const sellerOrders = '/seller/orders';
  static const sellerSettlements = '/seller/settlements';

  // 개발용 — 전체 화면(IA) 목록에서 아무 화면이나 바로 이동
  static const devScreenIndex = '/dev/screens';

  static String storeDetailOf(String storeId) => '/stores/$storeId';
  static String dishDetailOf(String dishId) => '/dishes/$dishId';
  static String orderDetailOf(String orderId) => '/orders/$orderId';
  static String orderCancelOf(String orderId) => '/orders/$orderId/cancel';
  static String orderPickupOf(String orderId) => '/orders/$orderId/pickup';
}
