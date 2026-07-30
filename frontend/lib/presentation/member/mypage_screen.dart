import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/network/token_storage_provider.dart';
import '../../core/presentation/phone_format.dart';
import '../../core/routing/route_paths.dart';
import '../../domain/model/member.dart';
import '../../ui/app_colors.dart';
import '../../ui/app_spacing.dart';
import '../deposit/deposit_providers.dart';
import 'member_repository_provider.dart';

const _roleLabels = {'MEMBER': '구매자', 'SELLER': '판매자'};

/// 마이페이지 화면 (B12, `/me`). 하단 탭 중 하나.
///
/// 2026-07-29: `GET /members/me` 조회 연동 — 회원정보 카드(조회 전용, 수정은
/// 여전히 백엔드에 `PUT /members/me` 자체가 없어 범위 밖)를 실제로 그린다.
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
    final balanceAsync = ref.watch(depositBalanceProvider);
    final isSeller = memberAsync.valueOrNull?.role == 'SELLER';
    final textTheme = Theme.of(context).textTheme;

    return Scaffold(
      appBar: AppBar(title: const Text('마이페이지')),
      body: RefreshIndicator(
        onRefresh: () => ref.refresh(myInfoProvider.future),
        child: ListView(
          padding: const EdgeInsets.all(AppSpacing.md),
          children: [
            memberAsync.when(
              data: (member) => _ProfileCard(
                member: member,
                balance: balanceAsync.valueOrNull?.balance,
                textTheme: textTheme,
              ),
              error: (error, _) => Card(
                child: Padding(
                  padding: const EdgeInsets.all(AppSpacing.lg),
                  child: Text(error.toString(), textAlign: TextAlign.center),
                ),
              ),
              loading: () => const Padding(
                padding: EdgeInsets.symmetric(vertical: AppSpacing.lg),
                child: Center(child: CircularProgressIndicator()),
              ),
            ),
            const SizedBox(height: AppSpacing.lg),
            _MenuButton(
              label: '예치금 잔액/내역',
              onTap: () => context.push(RoutePaths.deposits),
            ),
            if (!isSeller)
              _MenuButton(
                // S0(사업자 인증) 게이트 없이 바로 매장등록으로 보낸다 — 검증 API가
                // 없어 S0는 이미 통과 전용 화면이었고(seller_verify_screen.dart 참고),
                // 매장 등록(S1)이 성공하면 서버가 SELLER 권한을 자동 부여한다
                // (StoreFacade.register → SellerRoleGrantPort, PR #118). 판매자
                // 전용 화면(S1~S3)은 이 버튼이 사라지고 isSeller 분기로만 보이므로,
                // 매장 등록 전에는 이 진입점 하나만 노출된다.
                label: '매장 등록하기',
                onTap: () => context.push(RoutePaths.sellerStore),
              ),
            if (isSeller) ...[
              _MenuButton(
                label: '매장 등록/수정',
                onTap: () => context.push(RoutePaths.sellerStore),
              ),
              _MenuButton(
                label: '상품 등록/관리',
                onTap: () => context.push(RoutePaths.sellerDishes),
              ),
              _MenuButton(
                label: '주문 접수/픽업 처리',
                onTap: () => context.push(RoutePaths.sellerOrders),
              ),
            ],
            Center(
              child: TextButton(
                onPressed: () => _logout(context, ref),
                child: const Text('로그아웃'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _ProfileCard extends StatelessWidget {
  const _ProfileCard({
    required this.member,
    required this.balance,
    required this.textTheme,
  });

  final Member member;
  // 예치금 잔액 — 아직 로딩 중이거나 조회에 실패했으면 null. 이 카드는 회원정보
  // 조회 실패와 별개로 그려지므로(memberAsync만 error 분기함), null이면 그냥 그
  // 줄을 생략한다(cart_screen.dart의 재고 표시와 같은 방식).
  final num? balance;
  final TextTheme textTheme;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(AppSpacing.lg),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Text(
                  member.name,
                  style: textTheme.titleLarge?.copyWith(
                    color: AppColors.textStrong,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const SizedBox(width: AppSpacing.sm),
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 8,
                    vertical: 2,
                  ),
                  decoration: BoxDecoration(
                    color: AppColors.primary.withValues(alpha: 0.1),
                    borderRadius: BorderRadius.circular(999),
                  ),
                  child: Text(
                    _roleLabels[member.role] ?? member.role,
                    style: textTheme.labelSmall?.copyWith(
                      color: AppColors.primary,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: AppSpacing.md),
            _InfoRow(label: '아이디', value: member.userName),
            _InfoRow(label: '이메일', value: member.email),
            _InfoRow(label: '전화번호', value: formatPhone(member.phone)),
            _InfoRow(label: '가입일', value: member.createdAt.split('T').first),
            if (balance != null)
              _InfoRow(label: '예치금', value: '${balance!.toInt()}원'),
          ],
        ),
      ),
    );
  }
}

class _InfoRow extends StatelessWidget {
  const _InfoRow({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: AppSpacing.xs),
      child: Row(
        children: [
          SizedBox(
            width: 72,
            child: Text(
              label,
              style: textTheme.labelSmall?.copyWith(color: AppColors.textHint),
            ),
          ),
          Expanded(
            child: Text(
              value,
              style: textTheme.bodyMedium?.copyWith(color: AppColors.textBody),
            ),
          ),
        ],
      ),
    );
  }
}

class _MenuButton extends StatelessWidget {
  const _MenuButton({required this.label, required this.onTap});

  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: AppSpacing.sm),
      child: OutlinedButton(onPressed: onTap, child: Text(label)),
    );
  }
}
