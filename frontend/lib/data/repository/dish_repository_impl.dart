import 'package:dio/dio.dart';

import '../../core/network/api_error_mapper.dart';
import '../../domain/model/dish.dart';
import '../../domain/repository/dish_repository.dart';

/// DishRepository 실제 구현. dio로 core-service(Dish)를 호출한다.
/// (store_repository_impl.dart와 같은 구조.)
class DishRepositoryImpl implements DishRepository {
  DishRepositoryImpl({required Dio dio}) : _dio = dio;

  final Dio _dio;

  @override
  Future<Dish> getDish(int dishId) async {
    try {
      final response = await _dio.get('/dishes/$dishId');

      final body = response.data as Map<String, dynamic>;
      final data = body['data'] as Map<String, dynamic>;
      // 전체 DishResponse엔 dishStatus/storeId 등도 있지만, Dish 모델(원래
      // StoreDishResponse 대응용으로 만듦)엔 그 필드가 없다 — json_serializable은
      // 모델에 없는 키를 그냥 무시하므로 그대로 재사용해도 문제없다.
      return Dish.fromJson(data);
    } on DioException catch (e) {
      throw mapCoreServiceError(e);
    }
  }

  @override
  Future<List<Dish>> getDishesByStore(int storeId) async {
    try {
      final response = await _dio.get(
        '/dishes',
        queryParameters: {'storeId': storeId},
      );
      final body = response.data as Map<String, dynamic>;
      final data = body['data'] as List<dynamic>;
      return data.map((json) => Dish.fromJson(json as Map<String, dynamic>)).toList();
    } on DioException catch (e) {
      throw mapCoreServiceError(e);
    }
  }

  @override
  Future<Dish> createDish({
    required int storeId,
    required String dishName,
    required String registeredAt,
    String? description,
    String? thumbnailUrl,
    required int stockQuantity,
    required num dishPrice,
    required num discountPrice,
    String? pickupStartTime,
    String? pickupEndTime,
  }) async {
    try {
      final response = await _dio.post(
        '/dishes',
        data: {
          'storeId': storeId,
          'dishName': dishName,
          'registeredAt': registeredAt,
          if (description != null) 'description': description,
          if (thumbnailUrl != null) 'thumbnailUrl': thumbnailUrl,
          'stockQuantity': stockQuantity,
          'dishPrice': dishPrice,
          'discountPrice': discountPrice,
          // 서버가 "HH:mm" 형식만 파싱한다(DishCreateRequest의 @JsonFormat) — 매장의
          // openTime/closeTime은 시드데이터처럼 "09:00:00"(초 포함)로 올 수도 있어서
          // 앞 5자리만 잘라 보낸다(store_list_screen.dart의 표시 로직과 같은 이유).
          if (pickupStartTime != null)
            'pickupStartTime': _hhmm(pickupStartTime),
          if (pickupEndTime != null) 'pickupEndTime': _hhmm(pickupEndTime),
        },
      );
      return Dish.fromJson(_unwrap(response.data));
    } on DioException catch (e) {
      throw mapCoreServiceError(e);
    }
  }

  String _hhmm(String time) => time.length > 5 ? time.substring(0, 5) : time;

  @override
  Future<Dish> updateDish({
    required int dishId,
    required String dishName,
    required String registeredAt,
    String? description,
    String? thumbnailUrl,
    required int stockQuantity,
    required num dishPrice,
    required num discountPrice,
  }) async {
    try {
      final response = await _dio.put(
        '/dishes/$dishId',
        data: {
          'dishId': dishId,
          'dishName': dishName,
          'registeredAt': registeredAt,
          if (description != null) 'description': description,
          if (thumbnailUrl != null) 'thumbnailUrl': thumbnailUrl,
          'stockQuantity': stockQuantity,
          'dishPrice': dishPrice,
          'discountPrice': discountPrice,
        },
      );
      return Dish.fromJson(_unwrap(response.data));
    } on DioException catch (e) {
      throw mapCoreServiceError(e);
    }
  }

  @override
  Future<void> deleteDish(int dishId) async {
    try {
      await _dio.patch('/dishes/$dishId');
    } on DioException catch (e) {
      throw mapCoreServiceError(e);
    }
  }

  @override
  Future<Dish> updateDishStatus({required int dishId, required String dishStatus}) async {
    try {
      final response = await _dio.patch(
        '/dishes/$dishId/status',
        data: {'dishStatus': dishStatus},
      );
      return Dish.fromJson(_unwrap(response.data));
    } on DioException catch (e) {
      throw mapCoreServiceError(e);
    }
  }

  Map<String, dynamic> _unwrap(Object? responseData) {
    final body = responseData as Map<String, dynamic>;
    return body['data'] as Map<String, dynamic>;
  }
}
