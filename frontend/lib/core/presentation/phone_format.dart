/// 전화번호를 화면 표시용으로 하이픈 포맷팅한다.
///
/// DB엔 숫자만 저장한다(store_repository_impl.dart, auth_repository_impl.dart
/// 참고, 2026-07-30) — 그래서 서버가 내려주는 값은 하이픈이 없다. 여기서 자릿수
/// 기준으로 형식을 붙여서 읽기 쉽게 보여준다. 이미 하이픈이 섞여 들어와도(예: 아직
/// 정규화 전 시드데이터) 먼저 숫자만 남기고 다시 포맷하므로 안전하다.
String formatPhone(String raw) {
  final digits = raw.replaceAll(RegExp(r'[^0-9]'), '');

  switch (digits.length) {
    case 11: // 010-1234-5678
      return '${digits.substring(0, 3)}-${digits.substring(3, 7)}-${digits.substring(7)}';
    case 10:
      return digits.startsWith('02')
          ? '${digits.substring(0, 2)}-${digits.substring(2, 6)}-${digits.substring(6)}' // 02-1234-5678
          : '${digits.substring(0, 3)}-${digits.substring(3, 6)}-${digits.substring(6)}'; // 031-123-4567
    case 9: // 02-123-4567
      return '${digits.substring(0, 2)}-${digits.substring(2, 5)}-${digits.substring(5)}';
    default:
      return raw; // 못 알아보는 형식이면 원본 그대로 — 잘못 잘라 보여주는 것보단 낫다.
  }
}
