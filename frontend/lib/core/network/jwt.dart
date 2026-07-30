import 'dart:convert';

/// JWT의 `exp` 클레임(초 단위 유닉스 타임)만 로컬에서 읽어 만료 여부를 판단한다.
/// 서명 검증은 하지 않는다 — 그건 이미 Gateway가 매 요청마다 하고 있고, 여기서는
/// "새로고침 직후 로그인/홈 중 어디로 보낼지"를 즉시 판단하기 위한 가벼운
/// 휴리스틱일 뿐이다(router.dart의 redirect에서 사용).
bool isJwtExpired(String token) {
  try {
    final parts = token.split('.');
    if (parts.length != 3) return true;
    final payload = utf8.decode(base64Url.decode(base64Url.normalize(parts[1])));
    final claims = jsonDecode(payload) as Map<String, dynamic>;
    final exp = claims['exp'];
    if (exp is! int) return true;
    return DateTime.now().isAfter(
      DateTime.fromMillisecondsSinceEpoch(exp * 1000),
    );
  } catch (_) {
    return true; // 파싱 실패 시 만료된 것으로 취급 — 안전한 쪽으로 기운다.
  }
}
