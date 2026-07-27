import 'package:riverpod/riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../core/network/dio_provider.dart';
import '../../data/repository/store_repository_impl.dart';
import '../../domain/repository/store_repository.dart';

part 'store_repository_provider.g.dart';

/// StoreRepository를 조립해 앱 전역에 제공한다 (auth_repository_provider.dart와 같은 패턴).
/// Store 조회는 로그인 없이도 되는 공개 API라,
/// tokenStorage를 안 기다려도 돼서 auth와 달리 Future가 아니라 그냥 값을 바로 만든다.
/// 목록/상세 화면이 공유하므로 store 폴더 상위에 둔다.
@riverpod
StoreRepository storeRepository(Ref ref) {
  return StoreRepositoryImpl(dio: ref.watch(dioProvider));
}
