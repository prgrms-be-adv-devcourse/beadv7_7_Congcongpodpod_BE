import 'dart:math' as math;

import 'package:flutter/material.dart';

/// 티켓 헤더 구석에 찍는 점선 원형 "ADMIT ONE" 도장. 실제 영화표 참고 —
/// 도장 느낌을 내려고 살짝 회전시키고 점선 테두리를 쓴다.
class AdmitOneStamp extends StatelessWidget {
  const AdmitOneStamp({required this.color, this.size = 44, super.key});

  final Color color;
  final double size;

  @override
  Widget build(BuildContext context) {
    return Transform.rotate(
      angle: -14 * math.pi / 180,
      child: SizedBox(
        width: size,
        height: size,
        child: CustomPaint(
          painter: _DashedCirclePainter(color: color),
          child: Center(
            child: Text(
              'ADMIT\nONE',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: size * 0.18,
                fontWeight: FontWeight.w800,
                color: color,
                height: 1.1,
                letterSpacing: 0.5,
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _DashedCirclePainter extends CustomPainter {
  const _DashedCirclePainter({required this.color});
  final Color color;

  static const _dashCount = 20;
  static const _dashFraction = 0.55; // 각 구간 중 실제로 그리는 비율

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = color
      ..style = PaintingStyle.stroke
      ..strokeWidth = 1.4;
    final radius = size.width / 2 - 1;
    final center = Offset(size.width / 2, size.height / 2);
    const dashAngle = 2 * math.pi / _dashCount;
    for (var i = 0; i < _dashCount; i++) {
      final start = i * dashAngle;
      canvas.drawArc(
        Rect.fromCircle(center: center, radius: radius),
        start,
        dashAngle * _dashFraction,
        false,
        paint,
      );
    }
  }

  @override
  bool shouldRepaint(covariant _DashedCirclePainter oldDelegate) => oldDelegate.color != color;
}
