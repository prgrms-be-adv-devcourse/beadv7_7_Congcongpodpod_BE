import 'package:flutter/foundation.dart' show kDebugMode;
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/presentation/components/admit_one_stamp.dart';
import '../../../core/presentation/components/ticket_card.dart';
import '../../../core/routing/route_paths.dart';
import '../../../ui/app_colors.dart';
import '../../../ui/app_spacing.dart';
import 'login_view_model.dart';

/// 로그인 화면 — "입장권" 티켓 컴포넌트 사용
/// (픽업코드 화면(B13)과 같은 시각 언어를 공유하도록 설계).
class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key});

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  void _submit() {
    ref
        .read(loginViewModelProvider.notifier)
        .login(
          email: _emailController.text.trim(),
          password: _passwordController.text,
        );
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(loginViewModelProvider);
    final isLoading = state.isLoading;
    final textTheme = Theme.of(context).textTheme;

    ref.listen(loginViewModelProvider, (previous, next) {
      final wasLoading = previous?.isLoading ?? false;
      if (!wasLoading || next.isLoading) return; // 로딩이 끝난 전이가 아니면 무시.

      // ⚠️ next.hasValue로 성공 여부를 판단하면 안 된다 — AsyncNotifier는 로딩/에러
      // 상태로 전이할 때도 "직전 상태의 hasValue"를 그대로 이어받는다(copyWithPrevious,
      // 새로고침 중에도 이전 데이터를 화면에 계속 보여주기 위한 Riverpod의 의도된 동작).
      // 이 화면의 초기 상태(build()가 즉시 반환하는 AsyncData(null))부터 이미
      // hasValue=true라서, 그 뒤에 로그인이 실패해도 next.hasValue가 계속 true로 남는다
      // — 그래서 hasError를 먼저 확인해서 갈라야 한다.
      if (next.hasError) {
        // 스낵바엔 메시지 한 줄만 보이고 스택트레이스는 안 남아서, 예상 못 한 타입의
        // 예외(AppException이 아닌 raw TypeError 등)가 나오면 원인 위치를 알 방법이
        // 없었다 — 디버그 빌드에서만 콘솔에 전체 스택트레이스를 남긴다.
        if (kDebugMode) {
          debugPrint('[login] ${next.error}\n${next.stackTrace}');
        }
        ScaffoldMessenger.of(context)
          ..hideCurrentSnackBar()
          ..showSnackBar(SnackBar(content: Text(next.error.toString())));
      } else {
        context.go(RoutePaths.home);
      }
    });

    return Scaffold(
      body: SafeArea(
        // SingleChildScrollView는 shrinkWrap 옵션이 없어서(리스트류에만 있음),
        // 스크롤 방향(세로)엔 항상 무제한 높이를 자식에게 준다 — 그 안의 Center는
        // 그래서 실제로는 안 먹힌다. ConstrainedBox로 최소 높이를 화면 높이만큼
        // 강제해야 Center가 그 안에서 진짜로 세로 가운데 정렬된다.
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(
            horizontal: AppSpacing.lg,
            vertical: AppSpacing.xxl,
          ),
          child: ConstrainedBox(
            constraints: BoxConstraints(
              minHeight: MediaQuery.sizeOf(context).height,
            ),
            child: Center(
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 380),
                child: TicketCard(
                  header: Padding(
                    padding: const EdgeInsets.fromLTRB(
                      AppSpacing.lg,
                      AppSpacing.xxl,
                      AppSpacing.lg,
                      AppSpacing.xl,
                    ),
                    child: Stack(
                      children: [
                        Padding(
                          // 우측 도장(AdmitOneStamp)과 안 겹치게 텍스트 폭 확보.
                          padding: const EdgeInsets.only(right: 56),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                'LastDish',
                                style: textTheme.headlineLarge?.copyWith(
                                  color: AppColors.textOnCardboard,
                                  fontWeight: FontWeight.w800,
                                ),
                              ),
                              const SizedBox(height: AppSpacing.xs),
                              Text(
                                '오늘의 마지막 한 상, 입장권',
                                style: textTheme.bodySmall?.copyWith(
                                  color: AppColors.textOnCardboard.withValues(
                                    alpha: 0.8,
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ),
                        const Positioned(
                          right: 0,
                          top: AppSpacing.xs,
                          child: AdmitOneStamp(
                            color: AppColors.textOnCardboard,
                          ),
                        ),
                      ],
                    ),
                  ),
                  body: Padding(
                    padding: const EdgeInsets.fromLTRB(
                      AppSpacing.lg,
                      AppSpacing.lg,
                      AppSpacing.lg,
                      AppSpacing.sm,
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        _TicketField(
                          label: '이메일',
                          hint: 'name@example.com',
                          controller: _emailController,
                          keyboardType: TextInputType.emailAddress,
                          enabled: !isLoading,
                        ),
                        const SizedBox(height: AppSpacing.md),
                        _TicketField(
                          label: '비밀번호',
                          hint: '••••••••',
                          controller: _passwordController,
                          obscureText: true,
                          enabled: !isLoading,
                          onSubmitted: (_) => isLoading ? null : _submit(),
                        ),
                        const SizedBox(height: AppSpacing.lg),
                        ElevatedButton(
                          onPressed: isLoading ? null : _submit,
                          child: isLoading
                              ? const SizedBox(
                                  width: 20,
                                  height: 20,
                                  child: CircularProgressIndicator(
                                    strokeWidth: 2,
                                    color: Colors.white,
                                  ),
                                )
                              : const Text('입장하기'),
                        ),
                      ],
                    ),
                  ),
                  footer: Padding(
                    padding: const EdgeInsets.fromLTRB(
                      AppSpacing.lg,
                      AppSpacing.sm,
                      AppSpacing.lg,
                      AppSpacing.xl,
                    ),
                    child: Center(
                      child: Column(
                        children: [
                          GestureDetector(
                            onTap: isLoading
                                ? null
                                : () => context.push(RoutePaths.signup),
                            child: Text(
                              'NO. LD-0001 · 이메일로 회원가입',
                              style: textTheme.labelSmall?.copyWith(
                                color: AppColors.textMeta,
                                letterSpacing: 1.0,
                              ),
                            ),
                          ),
                          // 개발용 — 전체 17개 화면을 API 연동 전에 눌러볼 수 있는
                          // IA 지도. 데모/운영 빌드(release)에서는 안 보이게 한다.
                          if (kDebugMode) ...[
                            const SizedBox(height: AppSpacing.xs),
                            GestureDetector(
                              onTap: () =>
                                  context.push(RoutePaths.devScreenIndex),
                              child: Text(
                                '🗂 전체 화면 보기 (개발용)',
                                style: textTheme.labelSmall?.copyWith(
                                  color: AppColors.textMeta,
                                  letterSpacing: 1.0,
                                ),
                              ),
                            ),
                          ],
                        ],
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _TicketField extends StatelessWidget {
  const _TicketField({
    required this.label,
    required this.hint,
    required this.controller,
    this.obscureText = false,
    this.enabled = true,
    this.keyboardType,
    this.onSubmitted,
  });

  final String label;
  final String hint;
  final TextEditingController controller;
  final bool obscureText;
  final bool enabled;
  final TextInputType? keyboardType;
  final ValueChanged<String>? onSubmitted;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: textTheme.labelSmall?.copyWith(color: AppColors.textHint),
        ),
        TextField(
          controller: controller,
          obscureText: obscureText,
          enabled: enabled,
          keyboardType: keyboardType,
          onSubmitted: onSubmitted,
          style: textTheme.bodyMedium?.copyWith(color: AppColors.textStrong),
          decoration: InputDecoration(hintText: hint),
        ),
      ],
    );
  }
}
