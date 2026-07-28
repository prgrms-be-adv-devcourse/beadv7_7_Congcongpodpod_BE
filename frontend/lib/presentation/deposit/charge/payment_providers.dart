import 'package:riverpod/riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../../core/network/dio_provider.dart';
import '../../../data/repository/payment_repository_impl.dart';
import '../../../domain/repository/payment_repository.dart';

part 'payment_providers.g.dart';

/// PaymentRepository를 조립해 앱 전역에 제공한다 (deposit_providers.dart와 같은 패턴).
@riverpod
PaymentRepository paymentRepository(Ref ref) {
  return PaymentRepositoryImpl(dio: ref.watch(dioProvider));
}
