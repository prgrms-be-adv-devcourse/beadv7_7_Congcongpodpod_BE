/// 액세스/리프레시 토큰 저장 계약. 구현은 lib/data/data_source/local/ 에 둔다.
/// 1차 구현은 SharedPrefsTokenStorage(shared_preferences, 평문) — 시간 남으면
/// SecureTokenStorage(flutter_secure_storage)로 교체 예정. 이 인터페이스만
/// 지키면 상위 레이어(dio interceptor 등)는 구현 교체를 몰라도 된다.
abstract interface class TokenStorage {
  Future<void> saveTokens({
    required String accessToken,
    required String refreshToken,
  });

  Future<String?> getAccessToken();

  Future<String?> getRefreshToken();

  Future<void> clear();
}
