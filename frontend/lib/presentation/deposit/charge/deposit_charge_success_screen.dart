import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/token_storage_provider.dart';
import '../../../core/routing/browser_url.dart';
import '../../../core/routing/route_paths.dart';
import '../../../ui/app_spacing.dart';
import 'deposit_charge_success_view_model.dart';
import 'return_to_cart_flag.dart';

/// Toss 결제창이 성공 시 리다이렉트하는 화면 (`/deposits/charge/success`).
/// 이 앱은 hash 라우팅이라 Toss가 붙인 쿼리(`paymentKey`/`orderId`/`amount`)가
/// go_router의 `state.uri.queryParameters`가 아니라 브라우저 URL의 `#` 앞부분에
/// 실린다 — 그래서 `Uri.base.queryParameters`로 직접 읽는다
/// (deposit_charge_view_model.dart의 successUrl 조립 부분 참고).
class DepositChargeSuccessScreen extends ConsumerStatefulWidget {
  const DepositChargeSuccessScreen({super.key});

  @override
  ConsumerState<DepositChargeSuccessScreen> createState() =>
      _DepositChargeSuccessScreenState();
}

class _DepositChargeSuccessScreenState
    extends ConsumerState<DepositChargeSuccessScreen> {
  // 카트에서 "충전하러 가기"로 왔는지 여부. 승인 API 성공 여부와 무관하게 이
  // 화면에 들어오는 순간 한 번만 소비(읽고 지움)한다 — return_to_cart_flag.dart 참고.
  bool _returnToCart = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _approve());
    _consumeReturnToCartFlag();
  }

  Future<void> _consumeReturnToCartFlag() async {
    final prefs = await ref.read(sharedPreferencesProvider.future);
    final returnToCart = await consumeReturnToCartAfterCharge(prefs);
    if (mounted) setState(() => _returnToCart = returnToCart);
  }

  void _approve() {
    final query = Uri.base.queryParameters;
    final paymentKey = query['paymentKey'];
    final orderId = query['orderId'];
    final amount = num.tryParse(query['amount'] ?? '');
    stripLeakedQueryFromUrl();

    if (paymentKey == null || orderId == null || amount == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('결제 정보를 읽지 못했어요. 다시 시도해 주세요.')),
      );
      return;
    }

    ref
        .read(depositChargeSuccessViewModelProvider.notifier)
        .approve(paymentKey: paymentKey, orderId: orderId, amount: amount);
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(depositChargeSuccessViewModelProvider);
    final textTheme = Theme.of(context).textTheme;

    return Scaffold(
      appBar: AppBar(title: const Text('충전 처리 중')),
      body: Padding(
        padding: const EdgeInsets.all(AppSpacing.md),
        child: Center(
          child: state.when(
            data: (approve) {
              if (approve == null) {
                return const CircularProgressIndicator();
              }
              return Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(Icons.check_circle, size: 48, color: Colors.green),
                  const SizedBox(height: AppSpacing.md),
                  Text('충전 완료!', style: textTheme.titleLarge),
                  const SizedBox(height: AppSpacing.sm),
                  Text('현재 잔액 ${approve.depositBalance.toInt()}원'),
                  const SizedBox(height: AppSpacing.lg),
                  ElevatedButton(
                    onPressed: () {
                      // 결제 리다이렉트로 새로 로드된 페이지라 스택이 이 화면
                      // 하나뿐이다 — go()만 쓰면 스택이 그대로 1개라 뒤로가기가
                      // 안 생긴다. 홈으로 스택을 새로 잡고 그 위에 push해야
                      // 정상적으로 들어왔을 때와 같은 뒤로가기 동작이 된다.
                      // go() 직후 바로 push()하면 go()의 라우터 상태 반영이
                      // 아직 안 끝난 시점이라 push가 옛 스택(이 화면) 위에
                      // 얹혀서 뒤로가기가 이 화면으로 돌아오는 버그가 있었다
                      // (2026-07-30 발견) — 다음 프레임으로 미뤄서 go()가
                      // 완전히 반영된 뒤에 push하도록 고쳤다.
                      //
                      // 카트에서 "충전하러 가기"로 온 경우엔(_returnToCart)
                      // 예치금 화면 대신 카트로 돌려보낸다 — 돌아간 카트는
                      // depositBalanceProvider를 다시 watch하므로 새 잔액이
                      // 바로 반영돼 "주문하기"로 이어갈 수 있다.
                      context.go(RoutePaths.home);
                      WidgetsBinding.instance.addPostFrameCallback((_) {
                        if (context.mounted) {
                          context.push(
                            _returnToCart ? RoutePaths.cart : RoutePaths.deposits,
                          );
                        }
                      });
                    },
                    child: Text(_returnToCart ? '장바구니로 돌아가기' : '예치금 화면으로'),
                  ),
                ],
              );
            },
            loading: () => const Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                CircularProgressIndicator(),
                SizedBox(height: AppSpacing.md),
                Text('결제 승인 확인 중...'),
              ],
            ),
            error: (error, _) => Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.error_outline, size: 48, color: Colors.red),
                const SizedBox(height: AppSpacing.md),
                Text('충전 승인에 실패했어요\n$error', textAlign: TextAlign.center),
                const SizedBox(height: AppSpacing.lg),
                ElevatedButton(
                  onPressed: () => context.go(RoutePaths.depositCharge),
                  child: const Text('다시 시도'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
