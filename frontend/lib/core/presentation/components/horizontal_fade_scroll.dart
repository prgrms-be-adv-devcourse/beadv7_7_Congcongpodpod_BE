import 'package:flutter/material.dart';

/// 가로 스크롤 칩 목록(카테고리/상태 필터) 오른쪽 끝을 살짝 흐리게 만들어
/// "옆으로 더 있다"는 걸 보여준다. 실제 스크롤 위치를 추적하지 않고 항상
/// 오른쪽 끝만 흐리게 하는 단순한 방식 — 칩 목록은 짧아서 이 정도로 충분하다.
class HorizontalFadeScroll extends StatelessWidget {
  const HorizontalFadeScroll({required this.child, super.key});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return ShaderMask(
      shaderCallback: (bounds) => const LinearGradient(
        begin: Alignment.centerRight,
        end: Alignment.centerLeft,
        colors: [Colors.transparent, Colors.black],
        stops: [0.0, 0.08],
      ).createShader(bounds),
      blendMode: BlendMode.dstIn,
      child: child,
    );
  }
}
