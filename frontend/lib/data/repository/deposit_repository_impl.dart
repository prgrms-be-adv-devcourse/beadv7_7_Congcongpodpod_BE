import 'package:dio/dio.dart';

import '../../core/network/api_error_mapper.dart';
import '../../domain/model/deposit.dart';
import '../../domain/repository/deposit_repository.dart';

/// DepositRepository 실제 구현. Auth처럼 래퍼(`{success,data,...}`) 없이 DTO를
/// 그대로 반환하는 쪽이라(api-contracts.md 0절), 다른 core-service repository들과
/// 달리 `body['data']`를 벗기지 않고 response.data를 바로 쓴다.
class DepositRepositoryImpl implements DepositRepository {
  DepositRepositoryImpl({required Dio dio}) : _dio = dio;

  final Dio _dio;

  @override
  Future<DepositBalance> getBalance() async {
    try {
      final response = await _dio.get('/deposits/balance');
      return DepositBalance.fromJson(response.data as Map<String, dynamic>);
    } on DioException catch (e) {
      throw mapCoreServiceError(e);
    }
  }

  @override
  Future<List<DepositHistoryEntry>> getHistory() async {
    try {
      final response = await _dio.get('/deposits/history');
      final body = response.data as Map<String, dynamic>;
      // Page<DepositHistoryResponse> — 래퍼는 없지만 Spring Data Page 자체 포맷은
      // 그대로라 content만 꺼내면 된다(orders/deposits 둘 다 같은 Page 포맷).
      final content = body['content'] as List<dynamic>;
      return content
          .map((json) => DepositHistoryEntry.fromJson(json as Map<String, dynamic>))
          .toList();
    } on DioException catch (e) {
      throw mapCoreServiceError(e);
    }
  }
}
