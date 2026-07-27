import 'package:dio/dio.dart';

import '../../core/domain/error/app_exception.dart';
import '../../core/network/api_error_mapper.dart';
import '../../domain/error/cart_exception.dart';
import '../../domain/model/cart.dart';
import '../../domain/repository/cart_repository.dart';

/// CartRepository 실제 구현. dio로 core-service(Cart)를 호출한다.
/// (store_repository_impl.dart와 같은 구조 — dio 직접 호출 + mapCoreServiceError로 에러 정규화.)
class CartRepositoryImpl implements CartRepository {
  CartRepositoryImpl({required Dio dio}) : _dio = dio;

  final Dio _dio;

  @override
  Future<Cart> getMyCart() async {
    try {
      // ⚠️ 가정한 경로다. 지금 실제로 구현된 API는 `GET /carts/members/{memberId}`라서
      // memberId를 URL에 직접 넣어야 하는데, 프론트는 아직 memberId를 알 방법이 없다
      // (`/members/me` 미구현). Order 계열처럼 Gateway가 `X-Authenticated-Member-Id`
      // 헤더로 회원을 식별해주는 방식으로 바뀔 것을 가정하고 `/carts/me`로 미리 짜둔 것 —
      // 실제로 이 경로가 없으면 404가 날 수 있다.
      final response = await _dio.get('/carts/me');

      final body = response.data as Map<String, dynamic>;
      final data = body['data'] as Map<String, dynamic>;
      return Cart.fromJson(data);
    } on DioException catch (e) {
      throw _mapCartError(e);
    }
  }

  @override
  Future<CartItem> addItem({
    required int cartId,
    required int dishId,
    required int quantity,
  }) async {
    try {
      final response = await _dio.post(
        '/carts/$cartId/items',
        data: {'dishId': dishId, 'quantity': quantity},
      );

      final body = response.data as Map<String, dynamic>;
      final data = body['data'] as Map<String, dynamic>;
      return CartItem.fromJson(data);
    } on DioException catch (e) {
      throw _mapCartError(e);
    }
  }

  @override
  Future<CartItem> updateItemQuantity({
    required int cartId,
    required int itemId,
    required int quantity,
  }) async {
    try {
      final response = await _dio.patch(
        '/carts/$cartId/items/$itemId',
        data: {'quantity': quantity},
      );

      final body = response.data as Map<String, dynamic>;
      final data = body['data'] as Map<String, dynamic>;
      return CartItem.fromJson(data);
    } on DioException catch (e) {
      throw _mapCartError(e);
    }
  }

  @override
  Future<void> removeItem({required int cartId, required int itemId}) async {
    try {
      await _dio.delete('/carts/$cartId/items/$itemId');
    } on DioException catch (e) {
      throw _mapCartError(e);
    }
  }

  @override
  Future<void> clearCart(int cartId) async {
    try {
      await _dio.delete('/carts/$cartId');
    } on DioException catch (e) {
      throw _mapCartError(e);
    }
  }

  /// core-service 공용 매퍼(mapCoreServiceError)로 먼저 정규화한 뒤, 장바구니 화면에서
  /// 특별히 구분해서 보여주고 싶은 두 가지 상황(재고 부족, 대상 없음)만 더 구체적인
  /// CartException으로 다시 감싼다 — 나머지는 일반 AppException 그대로 둔다.
  Exception _mapCartError(DioException e) {
    final mapped = mapCoreServiceError(e);
    if (mapped is ServerException) {
      switch (mapped.code) {
        case 'D003': // 재고 부족
        case 'C004': // 품절
          return const CartOutOfStockException();
        case 'C003': // 대상(Entity)을 찾을 수 없음
        case 'D002': // 상품을 찾을 수 없음
          return const CartNotFoundException();
      }
    }
    return mapped;
  }
}
