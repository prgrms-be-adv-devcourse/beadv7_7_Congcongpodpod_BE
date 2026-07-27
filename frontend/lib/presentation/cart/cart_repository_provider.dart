import 'package:riverpod/riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../core/network/dio_provider.dart';
import '../../data/repository/cart_repository_impl.dart';
import '../../domain/repository/cart_repository.dart';

part 'cart_repository_provider.g.dart';

/// CartRepository를 조립해 앱 전역에 제공한다 (store_repository_provider.dart와 같은 패턴).
@riverpod
CartRepository cartRepository(Ref ref) {
  return CartRepositoryImpl(dio: ref.watch(dioProvider));
}
