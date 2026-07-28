/// 코너 radius 스케일. "shape 일관성" 원칙 — 이 값들 밖의 radius를 새로 만들지 않는다.
/// sm=작은뱃지, md=일반 카드(기본), pill=완전히 둥근 태그/칩,
/// sharp=티켓 컴포넌트 전용(버튼 등) — 종이 티켓의 각진 느낌을 의도적으로 유지.
abstract final class AppRadius {
  static const sharp = 4.0;
  static const sm = 8.0;
  static const md = 12.0;
  static const pill = 999.0;
}
