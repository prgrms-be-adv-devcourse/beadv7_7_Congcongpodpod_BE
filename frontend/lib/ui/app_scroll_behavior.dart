import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';

/// 기본 `MaterialScrollBehavior`는 마우스를 드래그 스크롤 허용 기기 목록에서
/// 뺀다(터치/스타일러스만 허용) — 텍스트 선택 등과 충돌을 피하려는 의도지만,
/// 그 결과 Flutter Web/데스크톱에서 마우스로 가로 스크롤 영역(카테고리 필터 칩 등)을
/// 드래그해도 안 움직인다(2026-07-30 발견, "뒤쪽 카테고리를 드래그로 못 누른다"
/// 제보). 마우스도 드래그 기기로 허용해서 이 프로젝트 전역에 적용한다.
class AppScrollBehavior extends MaterialScrollBehavior {
  @override
  Set<PointerDeviceKind> get dragDevices => {
    ...super.dragDevices,
    PointerDeviceKind.mouse,
  };
}
