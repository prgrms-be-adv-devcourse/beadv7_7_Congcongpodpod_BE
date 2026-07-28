import 'package:dio/dio.dart';

import '../../core/network/api_error_mapper.dart';
import '../../domain/model/order.dart';
import '../../domain/model/pickup_code.dart';
import '../../domain/repository/order_repository.dart';

/// OrderRepository 실제 구현. dio로 core-service(Order)를 호출한다.
/// (store_repository_impl.dart와 같은 구조.)
class OrderRepositoryImpl implements OrderRepository {
  OrderRepositoryImpl({required Dio dio}) : _dio = dio;

  final Dio _dio;

  @override
  Future<Order> createOrder({required int cartItemId}) async {
    try {
      final response = await _dio.post('/orders/cartItems/$cartItemId');
      return Order.fromJson(_unwrap(response.data));
    } on DioException catch (e) {
      throw mapCoreServiceError(e);
    }
  }

  @override
  Future<List<Order>> getMyOrders({String? status}) async {
    try {
      final response = await _dio.get(
        '/orders',
        queryParameters: {if (status != null) 'status': status},
      );
      final data = _unwrap(response.data);
      // Page<OrderResponse> — Spring Data 표준 포맷, 실제 목록은 `content`에 있다
      // (deposits/history와 같은 패턴). 페이지네이션 UI는 아직 없어서 첫 페이지만 쓴다.
      final content = data['content'] as List<dynamic>;
      return content
          .map((json) => Order.fromJson(json as Map<String, dynamic>))
          .toList();
    } on DioException catch (e) {
      throw mapCoreServiceError(e);
    }
  }

  @override
  Future<Order> getOrder(int orderId) async {
    try {
      final response = await _dio.get('/orders/$orderId');
      return Order.fromJson(_unwrap(response.data));
    } on DioException catch (e) {
      throw mapCoreServiceError(e);
    }
  }

  @override
  Future<Order> cancelOrder(int orderId) async {
    try {
      // 요청 바디 없음 — OrderController.cancelOrder가 실제로 바디를 안 받는다
      // (2026-07-26 PO 확정: 취소 사유는 매장/시스템 전용, 구매자는 안 고름).
      final response = await _dio.patch('/orders/$orderId/cancel');
      return Order.fromJson(_unwrap(response.data));
    } on DioException catch (e) {
      throw mapCoreServiceError(e);
    }
  }

  @override
  Future<PickupCode> getPickupCode(int orderId) async {
    try {
      final response = await _dio.get('/orders/$orderId/pickupCode');
      return PickupCode.fromJson(_unwrap(response.data));
    } on DioException catch (e) {
      throw mapCoreServiceError(e);
    }
  }

  @override
  Future<List<Order>> getStoreOrders({
    required int storeId,
    String? status,
  }) async {
    try {
      final response = await _dio.get(
        '/orders/stores/$storeId',
        queryParameters: {if (status != null) 'status': status},
      );
      final data = _unwrap(response.data);
      final content = data['content'] as List<dynamic>;
      return content
          .map((json) => Order.fromJson(json as Map<String, dynamic>))
          .toList();
    } on DioException catch (e) {
      throw mapCoreServiceError(e);
    }
  }

  @override
  Future<void> acceptOrder(int orderId) async {
    try {
      await _dio.post('/orders/$orderId/accept');
    } on DioException catch (e) {
      throw mapCoreServiceError(e);
    }
  }

  @override
  Future<void> rejectOrder({
    required int orderId,
    required String reason,
  }) async {
    try {
      await _dio.post('/orders/$orderId/reject', data: {'reason': reason});
    } on DioException catch (e) {
      throw mapCoreServiceError(e);
    }
  }

  @override
  Future<void> updatePickupStatus({
    required int orderId,
    required String status,
  }) async {
    try {
      await _dio.patch('/orders/$orderId/pickup', data: {'status': status});
    } on DioException catch (e) {
      throw mapCoreServiceError(e);
    }
  }

  Map<String, dynamic> _unwrap(Object? responseData) {
    final body = responseData as Map<String, dynamic>;
    return body['data'] as Map<String, dynamic>;
  }
}
