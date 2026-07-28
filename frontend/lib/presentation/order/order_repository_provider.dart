import 'package:riverpod/riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../core/network/dio_provider.dart';
import '../../data/repository/order_repository_impl.dart';
import '../../domain/repository/order_repository.dart';

part 'order_repository_provider.g.dart';

/// OrderRepository를 조립해 앱 전역에 제공한다 (store_repository_provider.dart와 같은 패턴).
@riverpod
OrderRepository orderRepository(Ref ref) {
  return OrderRepositoryImpl(dio: ref.watch(dioProvider));
}
