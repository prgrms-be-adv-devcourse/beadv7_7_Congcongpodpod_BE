import 'package:dio/dio.dart';

import '../../core/network/api_error_mapper.dart';
import '../../domain/model/dish.dart';
import '../../domain/model/store.dart';
import '../../domain/repository/store_repository.dart';

/// StoreRepository 실제 구현. dio로 core-service(Store)를 호출하고,
/// 실패하면 [mapCoreServiceError]로 우리 예외 타입으로 정규화한다 —
/// 화면은 DioException을 절대 보지 않는다(auth_repository_impl.dart와 같은 원칙).
class StoreRepositoryImpl implements StoreRepository {
  StoreRepositoryImpl({required Dio dio}) : _dio = dio;

  final Dio _dio;

  @override
  Future<List<Store>> getNearbyStores({
    required double latitude,
    required double longitude,
    double radiusKm = 3,
    int page = 0,
    int size = 10,
    String? category,
  }) async {
    try {
      final response = await _dio.get(
        '/stores/nearby',
        queryParameters: {
          'latitude': latitude,
          'longitude': longitude,
          'radiusKm': radiusKm,
          'page': page,
          'size': size,
          // null이면 dio가 쿼리 파라미터 자체를 안 붙인다 — 그래서 카테고리 없을 때
          // "category="처럼 빈 값을 보내는 게 아니라 파라미터가 통째로 빠진다(서버 쪽 optional과 일치).
          if (category != null) 'category': category,
        },
      );

      // core-service 응답은 { success, data, error, timestamp } 래퍼를 쓴다
      // — 실제 내용물은 항상 'data' 키 안에 있다.
      final body = response.data as Map<String, dynamic>;
      final data = body['data'] as Map<String, dynamic>;

      // StoreSearchResponse{ stores[], page, size, totalElements, totalPages }.
      // 지금은 페이지네이션 UI가 없어서 stores 목록만 쓰고 나머지 필드는 무시한다 —
      // 무한스크롤/페이지 이동이 필요해지면 그때 반환 타입을 넓힌다.
      final stores = data['stores'] as List<dynamic>;
      return stores
          .map((json) => Store.fromJson(json as Map<String, dynamic>))
          .toList();
    } on DioException catch (e) {
      throw mapCoreServiceError(e);
    }
  }

  @override
  Future<Store> getStoreDetail(int storeId) async {
    try {
      final response = await _dio.get('/stores/$storeId');

      final body = response.data as Map<String, dynamic>;
      final data = body['data'] as Map<String, dynamic>;
      return Store.fromJson(data);
    } on DioException catch (e) {
      throw mapCoreServiceError(e);
    }
  }

  @override
  Future<Store> registerStore({
    required String storeName,
    required String businessNumber,
    required String storeAddress,
    required String storePhone,
    required String openTime,
    required String closeTime,
    required double latitude,
    required double longitude,
    required String category,
  }) async {
    try {
      final response = await _dio.post(
        '/stores',
        data: {
          'storeName': storeName,
          'businessNumber': businessNumber,
          'storeAddress': storeAddress,
          'storePhone': _digitsOnly(storePhone),
          'openTime': openTime,
          'closeTime': closeTime,
          'latitude': latitude,
          'longitude': longitude,
          'category': category,
        },
      );
      return Store.fromJson(_unwrap(response.data));
    } on DioException catch (e) {
      throw mapCoreServiceError(e);
    }
  }

  @override
  Future<Store> updateStore({
    required int storeId,
    required String storeName,
    required String storeAddress,
    required String storePhone,
    required String openTime,
    required String closeTime,
    required double latitude,
    required double longitude,
    required String category,
  }) async {
    try {
      final response = await _dio.put(
        '/stores/$storeId',
        data: {
          'storeName': storeName,
          'storeAddress': storeAddress,
          'storePhone': _digitsOnly(storePhone),
          'openTime': openTime,
          'closeTime': closeTime,
          'latitude': latitude,
          'longitude': longitude,
          'category': category,
        },
      );
      return Store.fromJson(_unwrap(response.data));
    } on DioException catch (e) {
      throw mapCoreServiceError(e);
    }
  }

  @override
  Future<List<Store>> getMyStores() async {
    try {
      final response = await _dio.get('/stores/mine');
      final body = response.data as Map<String, dynamic>;
      final data = body['data'] as List<dynamic>;
      return data.map((json) => Store.fromJson(json as Map<String, dynamic>)).toList();
    } on DioException catch (e) {
      throw mapCoreServiceError(e);
    }
  }

  @override
  Future<Dish?> getMyDish(int storeId) async {
    try {
      final response = await _dio.get('/stores/$storeId/dish');
      return Dish.fromJson(_unwrap(response.data));
    } on DioException catch (e) {
      // D002 = 아직 상품을 등록 안 함 — 화면은 이걸 에러가 아니라 "등록 폼 보여주기"로 다뤄야 하므로 null.
      if (e.response?.statusCode == 404) return null;
      throw mapCoreServiceError(e);
    }
  }

  Map<String, dynamic> _unwrap(Object? responseData) {
    final body = responseData as Map<String, dynamic>;
    return body['data'] as Map<String, dynamic>;
  }

  // DB엔 숫자만 저장하기로 함(하이픈은 화면 표시 전용) — 백엔드가 아직 저장 시점에
  // 정규화를 안 해서, 우선 프론트가 보내는 시점에 하이픈을 뺀다(2026-07-30).
  // 백엔드가 저장 시점 정규화를 갖추면 이 처리는 지워도 된다.
  String _digitsOnly(String value) => value.replaceAll(RegExp(r'[^0-9]'), '');
}
