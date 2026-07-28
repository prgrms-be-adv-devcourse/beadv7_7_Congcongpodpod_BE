import 'package:flutter/material.dart';

/// 티켓 하단 장식용 바코드 느낌 세로줄. 진짜 바코드가 아니라 "인쇄된 티켓" 분위기만 낸다.
class BarcodeStrip extends StatelessWidget {
  const BarcodeStrip({required this.color, super.key});

  final Color color;

  static const _heights = [1.0, 0.7, 1.0, 0.55, 0.9, 0.6, 1.0, 0.75, 0.5, 1.0];
  static const _height = 14.0;

  @override
  Widget build(BuildContext context) {
    return Opacity(
      opacity: 0.5,
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          for (final h in _heights) ...[
            Container(width: 1.5, height: _height * h, color: color),
            const SizedBox(width: 1.5),
          ],
        ],
      ),
    );
  }
}
