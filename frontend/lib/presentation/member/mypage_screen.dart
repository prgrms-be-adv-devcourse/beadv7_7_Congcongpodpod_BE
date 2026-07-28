import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/network/token_storage_provider.dart';
import '../../core/presentation/components/placeholder_screen.dart';
import '../../core/routing/route_paths.dart';
import 'member_repository_provider.dart';

/// 마이페이지 화면 (B12, `/me`). 하단 탭 중 하나. 회원 정보 조회/수정이
/// 목적이나 `GET/PUT /members/me` 컨트롤러 자체가 없어 백엔드 대기 —
/// 지금은 예치금/사업자인증/로그아웃 진입점만 제공.
///
/// 2026-07-27: role이 SELLER면 셀러 화면(S1~S3) 진입점도 추가로 보여준다 —
/// 구매자/판매자가 배타적이지 않다는 팀 확정(2026-07-22)에 따라, 로그인은
/// 하나로 유지하고 보유 역할에 따라 메뉴만 분기한다(`screens.md` §1 참고).
class MyPageScreen extends ConsumerWidget {
  const MyPageScreen({super.key});

  Future<void> _logout(BuildContext context, WidgetRef ref) async {
    // store_list_screen.dart의 로그아웃과 동일한 방식 — 백엔드 미구현이라
    // 클라이언트 측 토큰 삭제로 임시 대응.
    final storage = await ref.read(tokenStorageProvider.future);
    await storage.clear();
    if (context.mounted) {
      context.go(RoutePaths.login);
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final memberAsync = ref.watch(myInfoProvider);
    final isSeller = memberAsync.valueOrNull?.role == 'SELLER';

    return PlaceholderScreen(
      screenCode: 'B12',
      title: '마이페이지',
      description: '회원 정보 조회/수정 화면. 컨트롤러 없음 — 화면 껍데기만.',
      actions: [
        PlaceholderAction(
          '예치금 잔액/내역(B11)',
          () => context.push(RoutePaths.deposits),
        ),
        if (!isSeller)
          PlaceholderAction(
            '사업자 인증(판매자 전환, S0)',
            () => context.push(RoutePaths.sellerVerify),
          ),
        if (isSeller) ...[
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
        ],
        PlaceholderAction('로그아웃', () => _logout(context, ref)),
      ],
    );
  }
}
