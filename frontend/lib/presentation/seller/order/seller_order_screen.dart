import 'package:flutter/material.dart';

import '../../../core/presentation/components/placeholder_screen.dart';

/// 주문 접수/픽업 처리 화면 (S3, `/seller/orders`). 들어온 주문 확인, 픽업
/// 완료 처리가 목적. API는 존재하나 전이 트리거 방식 확인 필요.
///
/// 주문 거절/취소 사유 선택은 구매자(B10)가 아니라 여기서
/// 매장이 한다(2026-07-26 PO 확정) — 실제 사유 선택 UI는 다음 단계.
class SellerOrderScreen extends StatelessWidget {
  const SellerOrderScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return const PlaceholderScreen(
      screenCode: 'S3',
      title: '주문 접수/픽업 처리',
      description:
          '들어온 주문 확인, 픽업 완료 처리 화면. 주문 거절 시 사유'
          '(재고소진/품질문제/준비지연/매장휴무/시스템오류)를 매장이'
          '여기서 선택 — 다음 단계.',
      actions: [],
    );
  }
}
