/// 여백 스케일. 화면마다 `SizedBox(height: 13)` 같은 임의값 대신 이 상수를 쓴다.
/// 4 단위 배수로 잡음 — 4px 그리드가 Material 3 기본 리듬과 맞음.
abstract final class AppSpacing {
  static const xs = 4.0;
  static const sm = 8.0;
  static const md = 16.0;
  static const lg = 24.0;
  static const xl = 32.0;
  static const xxl = 48.0;
}
