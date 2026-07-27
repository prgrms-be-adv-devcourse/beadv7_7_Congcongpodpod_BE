import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../core/routing/route_paths.dart';
import '../../ui/app_colors.dart';
import '../../ui/app_spacing.dart';

/// 개발용 IA(전체 화면) 지도 (`/dev/screens`). 순서대로 안 눌러도 17개 화면을
/// 전부 훑어볼 수 있는 진입점 — 화면 목록을 그대로 코드로 옮긴 것. 로그인 화면 footer에서 진입.
/// (B4/B5는 2026-07-26 PO 확정으로 폐기돼 목록에 없음 — 총 17개)
class ScreenIndexScreen extends StatelessWidget {
  const ScreenIndexScreen({super.key});

  static const _buyerScreens = [
    (code: 'B1', title: '로그인', route: RoutePaths.login),
    (code: 'B2', title: '회원가입', route: RoutePaths.signup),
    (code: 'B3', title: '홈(매장목록)', route: RoutePaths.home),
    (code: 'B6', title: '장바구니', route: RoutePaths.cart),
    (code: 'B7', title: '주문 확인(체크아웃)', route: RoutePaths.checkout),
    (code: 'B8', title: '내 주문목록', route: RoutePaths.orders),
    (code: 'B9', title: '주문 상세', route: '/orders/1'),
    (code: 'B10', title: '주문 취소 확인', route: '/orders/1/cancel'),
    (code: 'B11', title: '예치금 잔액/내역', route: RoutePaths.deposits),
    (code: 'B12', title: '마이페이지', route: RoutePaths.me),
    (code: 'B13', title: '픽업 확인', route: '/orders/1/pickup'),
    (code: 'B14', title: '예치금 충전', route: RoutePaths.depositCharge),
  ];

  static const _sellerScreens = [
    (code: 'S0', title: '사업자 인증(판매자 전환)', route: RoutePaths.sellerVerify),
    (code: 'S1', title: '매장 등록/수정', route: RoutePaths.sellerStore),
    (code: 'S2', title: '상품 등록/관리', route: RoutePaths.sellerDishes),
    (code: 'S3', title: '주문 접수/픽업 처리', route: RoutePaths.sellerOrders),
    (code: 'S4', title: '정산 조회', route: RoutePaths.sellerSettlements),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('전체 화면 보기 (17개)')),
      body: ListView(
        padding: const EdgeInsets.all(AppSpacing.md),
        children: [
          const _SectionLabel('구매자 (12)'),
          for (final screen in _buyerScreens)
            _ScreenTile(
              code: screen.code,
              title: screen.title,
              route: screen.route,
            ),
          const SizedBox(height: AppSpacing.lg),
          const _SectionLabel('판매자 (5)'),
          for (final screen in _sellerScreens)
            _ScreenTile(
              code: screen.code,
              title: screen.title,
              route: screen.route,
            ),
        ],
      ),
    );
  }
}

class _SectionLabel extends StatelessWidget {
  const _SectionLabel(this.label);
  final String label;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: AppSpacing.sm),
      child: Text(
        label,
        style: Theme.of(
          context,
        ).textTheme.labelLarge?.copyWith(color: AppColors.textHint),
      ),
    );
  }
}

class _ScreenTile extends StatelessWidget {
  const _ScreenTile({
    required this.code,
    required this.title,
    required this.route,
  });

  final String code;
  final String title;
  final String route;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: ListTile(
        leading: CircleAvatar(
          backgroundColor: AppColors.primaryLight,
          foregroundColor: AppColors.primaryDark,
          child: Text(code, style: const TextStyle(fontSize: 12)),
        ),
        title: Text(title),
        subtitle: Text(
          route,
          style: const TextStyle(color: AppColors.textHint),
        ),
        onTap: () => context.push(route),
      ),
    );
  }
}
