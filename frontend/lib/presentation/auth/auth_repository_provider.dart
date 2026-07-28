import 'package:riverpod/riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../core/network/dio_provider.dart';
import '../../core/network/token_storage_provider.dart';
import '../../data/repository/auth_repository_impl.dart';
import '../../domain/repository/auth_repository.dart';

part 'auth_repository_provider.g.dart';

/// AuthRepository를 조립해 앱 전역에 제공한다.
/// tokenStorage가 비동기(SharedPreferences 초기화 대기)라 이 Provider도 Future다 —
/// 쓰는 쪽에서 `await ref.read(authRepositoryProvider.future)` 로 가져온다.
/// 로그인/회원가입이 공유하므로 auth 폴더 상위에 둔다.
@riverpod
Future<AuthRepository> authRepository(Ref ref) async {
  final tokenStorage = await ref.watch(tokenStorageProvider.future);
  return AuthRepositoryImpl(
    dio: ref.watch(dioProvider),
    tokenStorage: tokenStorage,
  );
}
