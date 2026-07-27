import 'package:shared_preferences/shared_preferences.dart';

import '../../../domain/repository/token_storage.dart';

/// TokenStorage 1차 구현. 평문 저장이라 실제 서비스라면 부적합하지만,
/// 이번 프로젝트(부트캠프 발표용, 짧은 토큰 수명)에서는 우선 이걸로 진행하고
/// 시간 남으면 SecureTokenStorage(flutter_secure_storage)로 교체하기로 함.
class SharedPrefsTokenStorage implements TokenStorage {
  SharedPrefsTokenStorage(this._prefs);

  final SharedPreferences _prefs;

  static const _accessTokenKey = 'access_token';
  static const _refreshTokenKey = 'refresh_token';

  @override
  Future<void> saveTokens({
    required String accessToken,
    required String refreshToken,
  }) async {
    await _prefs.setString(_accessTokenKey, accessToken);
    await _prefs.setString(_refreshTokenKey, refreshToken);
  }

  @override
  Future<String?> getAccessToken() async => _prefs.getString(_accessTokenKey);

  @override
  Future<String?> getRefreshToken() async => _prefs.getString(_refreshTokenKey);

  @override
  Future<void> clear() async {
    await _prefs.remove(_accessTokenKey);
    await _prefs.remove(_refreshTokenKey);
  }
}
