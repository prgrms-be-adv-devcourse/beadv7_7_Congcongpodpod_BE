import 'package:flutter/material.dart';

import '../../../ui/app_colors.dart';

/// 웹/데스크톱처럼 화면이 넓을 때, 콘텐츠를 모바일 폭(480)으로 가운데 고정한다.
/// 배민/카카오 웹처럼 "넓은 브라우저 창 안에 모바일 앱이 떠 있는" 모양을 낸다.
/// MaterialApp.router의 builder에서 한 번만 감싸면 모든 화면에 자동 적용된다 —
/// 화면마다 따로 안 넣어도 된다.
///
/// 480px 밑(실제 모바일 기기)에서는 그냥 꽉 채운다 — 거기선 제약이 필요 없다.
class MobileWidthConstraint extends StatelessWidget {
  const MobileWidthConstraint({required this.child, super.key});

  final Widget child;

  static const _maxWidth = 480.0;

  @override
  Widget build(BuildContext context) {
    return ColoredBox(
      // 좌우 여백 색 — 앱 배경과 구분되게 살짝 어둡게.
      color: AppColors.surface,
      child: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: _maxWidth),
          child: ColoredBox(color: AppColors.background, child: child),
        ),
      ),
    );
  }
}
