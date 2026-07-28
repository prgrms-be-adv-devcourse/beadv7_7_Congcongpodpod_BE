import 'package:dio/dio.dart';

import '../../core/domain/error/app_exception.dart';
import '../../core/network/api_error_mapper.dart';
import '../../domain/error/cart_exception.dart';
import '../../domain/model/cart.dart';
import '../../domain/repository/cart_repository.dart';
import '../../domain/repository/member_repository.dart';

/// CartRepository 실제 구현. dio로 core-service(Cart)를 호출한다.
/// (store_repository_impl.dart와 같은 구조 — dio 직접 호출 + mapCoreServiceError로 에러 정규화.)
class CartRepositoryImpl implements CartRepository {
  CartRepositoryImpl({required Dio dio, required MemberRepository memberRepository})
    : _dio = dio,
      _memberRepository = memberRepository;

  final Dio _dio;
  final MemberRepository _memberRepository;

  @override
  Future<Cart> getMyCart() async {
    try {
      // 실제 API는 `GET /carts/members/{memberId}` — memberId를 URL에 직접 넣어야 한다.
      // Order 계열처럼 Gateway 헤더(`X-Authenticated-Member-Id`) 기반으로 통일하는 작업이
      // 백엔드에서 진행 중이지만 아직 완료 전이라, 그때까지는 로그인 후 `/members/me`를
      // 호출해 얻은 memberId를 직접 넘긴다(2026-07-27 임시 우회, cart_repository.dart 참고).
      final member = await _memberRepository.getMyInfo();
      final response = await _dio.get('/carts/members/${member.id}');

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
