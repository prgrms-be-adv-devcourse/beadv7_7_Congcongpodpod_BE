import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/network/token_storage_provider.dart';
import '../../core/presentation/components/placeholder_screen.dart';
import '../../core/routing/route_paths.dart';

/// 마이페이지 화면 (B12, `/me`). 하단 탭 중 하나. 회원 정보 조회/수정이
/// 목적이나 `GET/PUT /members/me` 컨트롤러 자체가 없어 백엔드 대기 —
/// 지금은 예치금/사업자인증/로그아웃 진입점만 제공.
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
    return PlaceholderScreen(
      screenCode: 'B12',
      title: '마이페이지',
      description: '회원 정보 조회/수정 화면. 컨트롤러 없음 — 화면 껍데기만.',
      actions: [
        PlaceholderAction(
          '예치금 잔액/내역(B11)',
          () => context.push(RoutePaths.deposits),
        ),
        PlaceholderAction(
          '사업자 인증(판매자 전환, S0)',
          () => context.push(RoutePaths.sellerVerify),
        ),
        PlaceholderAction('로그아웃', () => _logout(context, ref)),
      ],
    );
  }
}
