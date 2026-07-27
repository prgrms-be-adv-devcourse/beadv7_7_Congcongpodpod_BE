import 'package:riverpod/riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../data/data_source/local/shared_prefs_token_storage.dart';
import '../../domain/repository/token_storage.dart';

part 'token_storage_provider.g.dart';

@Riverpod(keepAlive: true)
Future<SharedPreferences> sharedPreferences(Ref ref) {
  return SharedPreferences.getInstance();
}

/// 구현 교체 지점: SharedPrefsTokenStorage → SecureTokenStorage로 바꿀 때
/// 이 한 줄만 바꾸면 됨 (TokenStorage 인터페이스 뒤에 숨겨둔 이유).
@Riverpod(keepAlive: true)
Future<TokenStorage> tokenStorage(Ref ref) async {
  final prefs = await ref.watch(sharedPreferencesProvider.future);
  return SharedPrefsTokenStorage(prefs);
}
