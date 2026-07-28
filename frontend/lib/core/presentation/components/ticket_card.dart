import 'package:flutter/material.dart';

import '../../../ui/app_colors.dart';

/// 앱 전체가 공유하는 "티켓" 컴포넌트. 로그인 화면, 픽업코드 화면(B13) 등
/// 실제로 픽업코드를 보여주는 화면과 같은 시각 언어를 쓰기 위한 재사용 위젯.
///
/// 구성: 우측 상단 대각선 컷 카드 + 골판지 줄무늬 헤더 + 절취선(점선+펀칭홈) + 본문 + (선택)푸터.
class TicketCard extends StatelessWidget {
  const TicketCard({
    required this.header,
    required this.body,
    this.footer,
    this.stageColor = AppColors.surface,
    super.key,
  });

  /// 헤더 영역(카드보드 줄무늬 배경 위에 올라갈 위젯) — Padding 포함해서 넘길 것.
  final Widget header;

  /// 본문 영역(페이퍼 배경) — Padding 포함해서 넘길 것.
  final Widget body;

  /// 하단 영역(선택) — 일련번호/바코드 등.
  final Widget? footer;

  /// 카드가 놓이는 배경색. 절취선 펀칭홈이 이 색으로 뚫려 보이므로,
  /// 실제 화면(Scaffold)의 배경색과 맞춰야 자연스럽다.
  final Color stageColor;

  static const _cornerCut = 22.0;
  static const _radius = 14.0;

  @override
  Widget build(BuildContext context) {
    return CustomPaint(
      painter: _TicketOutlinePainter(cut: _cornerCut, radius: _radius),
      child: ClipPath(
        clipper: _TicketClipper(cut: _cornerCut, radius: _radius),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Stack(
              children: [
                const Positioned.fill(
                  child: CustomPaint(painter: _CardboardStripePainter()),
                ),
                header,
              ],
            ),
            _TicketPerforation(punchColor: stageColor),
            body,
            if (footer != null) footer!,
          ],
        ),
      ),
    );
  }
}

Path _cornerCutPath(Size size, {required double cut, required double radius}) {
  final path = Path()
    ..moveTo(radius, 0)
    ..lineTo(size.width - cut, 0)
    ..lineTo(size.width, cut)
    ..lineTo(size.width, size.height - radius)
    ..quadraticBezierTo(
      size.width,
      size.height,
      size.width - radius,
      size.height,
    )
    ..lineTo(radius, size.height)
    ..quadraticBezierTo(0, size.height, 0, size.height - radius)
    ..lineTo(0, radius)
    ..quadraticBezierTo(0, 0, radius, 0)
    ..close();
  return path;
}

class _TicketClipper extends CustomClipper<Path> {
  const _TicketClipper({required this.cut, required this.radius});
  final double cut;
  final double radius;

  @override
  Path getClip(Size size) => _cornerCutPath(size, cut: cut, radius: radius);

  @override
  bool shouldReclip(covariant CustomClipper<Path> oldClipper) => false;
}

/// 카드 배경(페이퍼) + 테두리를 클립 경로와 정확히 같은 모양으로 그린다.
/// (ClipPath의 자식만 잘리고 테두리는 안 따라가는 문제를 피하려고 별도 페인터로 그림.)
class _TicketOutlinePainter extends CustomPainter {
  const _TicketOutlinePainter({required this.cut, required this.radius});
  final double cut;
  final double radius;

  @override
  void paint(Canvas canvas, Size size) {
    final path = _cornerCutPath(size, cut: cut, radius: radius);
    canvas.drawPath(path, Paint()..color = AppColors.background);
    canvas.drawPath(
      path,
      Paint()
        ..color = AppColors.border
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1,
    );
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}

/// 골판지 줄무늬 — 높이에 상관없이 8px 단위로 반복되게 직접 그린다.
class _CardboardStripePainter extends CustomPainter {
  const _CardboardStripePainter();
  static const _bandHeight = 8.0;

  @override
  void paint(Canvas canvas, Size size) {
    canvas.drawRect(Offset.zero & size, Paint()..color = AppColors.cardboard);
    final darkPaint = Paint()..color = AppColors.cardboardDark;
    var y = 0.0;
    var isDark = false;
    while (y < size.height) {
      if (isDark) {
        canvas.drawRect(
          Rect.fromLTWH(0, y, size.width, _bandHeight),
          darkPaint,
        );
      }
      y += _bandHeight;
      isDark = !isDark;
    }
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}

/// 절취선: 점선 + 좌우 펀칭홈(원). 헤더와 본문 사이에 들어간다.
class _TicketPerforation extends StatelessWidget {
  const _TicketPerforation({required this.punchColor});
  final Color punchColor;

  static const _notchSize = 18.0;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: _notchSize,
      child: Stack(
        clipBehavior: Clip.none,
        children: [
          Positioned(
            left: _notchSize / 2,
            right: _notchSize / 2,
            top: _notchSize / 2 - 1,
            child: const CustomPaint(
              size: Size(double.infinity, 2),
              painter: _DashedLinePainter(),
            ),
          ),
          Positioned(left: -_notchSize / 2, top: 0, child: _notch()),
          Positioned(right: -_notchSize / 2, top: 0, child: _notch()),
        ],
      ),
    );
  }

  Widget _notch() => Container(
    width: _notchSize,
    height: _notchSize,
    decoration: BoxDecoration(
      color: punchColor,
      shape: BoxShape.circle,
      border: Border.all(color: AppColors.border),
    ),
  );
}

class _DashedLinePainter extends CustomPainter {
  const _DashedLinePainter();
  static const _dashWidth = 5.0;
  static const _dashSpace = 4.0;

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = AppColors.border
      ..strokeWidth = 2;
    var x = 0.0;
    while (x < size.width) {
      canvas.drawLine(
        Offset(x, size.height / 2),
        Offset(x + _dashWidth, size.height / 2),
        paint,
      );
      x += _dashWidth + _dashSpace;
    }
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}
