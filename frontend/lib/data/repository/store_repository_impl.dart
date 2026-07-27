import 'package:dio/dio.dart';

import '../../core/network/api_error_mapper.dart';
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
}
