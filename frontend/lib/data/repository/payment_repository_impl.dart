import 'package:dio/dio.dart';

import '../../core/network/api_error_mapper.dart';
import '../../domain/model/payment.dart';
import '../../domain/repository/payment_repository.dart';

/// PaymentRepository 실제 구현. Deposit처럼 래퍼(`{success,data,...}`) 없이 DTO를
/// 그대로 반환하는 쪽이라 `body['data']`를 벗기지 않고 response.data를 바로 쓴다
/// (deposit_repository_impl.dart와 같은 패턴, api-contracts.md 0절 참고).
class PaymentRepositoryImpl implements PaymentRepository {
  PaymentRepositoryImpl({required Dio dio}) : _dio = dio;

  final Dio _dio;

  @override
  Future<PaymentReady> ready({required num amount}) async {
    try {
      final response = await _dio.post(
        '/payments',
        data: {'amount': amount, 'pgProvider': 'TOSS'},
      );
      return PaymentReady.fromJson(response.data as Map<String, dynamic>);
    } on DioException catch (e) {
      throw mapCoreServiceError(e);
    }
  }

  @override
  Future<PaymentApprove> approve({
    required String paymentKey,
    required String orderId,
    required num amount,
  }) async {
    try {
      final response = await _dio.post(
        '/payments/approve',
        data: {'paymentKey': paymentKey, 'orderId': orderId, 'amount': amount},
      );
      return PaymentApprove.fromJson(response.data as Map<String, dynamic>);
    } on DioException catch (e) {
      throw mapCoreServiceError(e);
    }
  }
}
