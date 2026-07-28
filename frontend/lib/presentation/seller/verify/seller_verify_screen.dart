import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/presentation/components/placeholder_screen.dart';
import '../../../core/routing/route_paths.dart';

/// 사업자 인증(판매자 전환) 화면 (S0, `/seller/verify`). 구매자
/// 계정에서 사업자등록번호 검증을 거쳐 SELLER로 승급. 실제 검증 API는
/// 존재하지 않음(PO가 신규 API 요청함) — 화면만 준비.
/// S1~S3 전체가 이 화면 통과를 전제로 하는 게이트 화면이라, 지금은
/// 판매자 화면 4개로 바로 이동하는 버튼을 노출한다.
class SellerVerifyScreen extends StatelessWidget {
  const SellerVerifyScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return PlaceholderScreen(
      screenCode: 'S0',
      title: '사업자 인증(판매자 전환)',
      description:
          '사업자등록번호 검증 화면. 검증 API 자체가 없어 백엔드 대기 — '
          '지금은 인증 없이 판매자 화면으로 바로 이동만 가능.',
      actions: [
        PlaceholderAction(
          '매장 등록/수정(S1)',
          () => context.push(RoutePaths.sellerStore),
        ),
        PlaceholderAction(
          '상품 등록/관리(S2)',
          () => context.push(RoutePaths.sellerDishes),
        ),
        PlaceholderAction(
          '주문 접수/픽업 처리(S3)',
          () => context.push(RoutePaths.sellerOrders),
        ),
        PlaceholderAction(
          '정산 조회(S4)',
          () => context.push(RoutePaths.sellerSettlements),
        ),
      ],
    );
  }
}
