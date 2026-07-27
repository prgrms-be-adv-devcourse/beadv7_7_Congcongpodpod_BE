import 'package:flutter/foundation.dart' show kDebugMode;
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/routing/route_paths.dart';
import '../../../domain/model/order.dart';
import '../../../ui/app_colors.dart';
import '../../../ui/app_spacing.dart';
import '../../cart/cart_view_model.dart';
import '../../deposit/deposit_providers.dart';
import '../../dish/dish_providers.dart';
import '../../member/member_repository_provider.dart';
import '../list/order_list_view_model.dart';
import '../../store/list/store_list_view_model.dart';
import 'checkout_view_model.dart';

/// 주문 확인(체크아웃) 화면 (B7, `/cart/checkout`). 픽업 시간을 정하고 주문을 생성하면
/// 예치금이 즉시 차감된다(ADR 001, 결제=예치금 차감). 장바구니가 상품 1개 단위로
/// 단순화돼 있어(ADR 004), 이 화면도 그 1개만 다룬다.
class CheckoutScreen extends ConsumerStatefulWidget {
  const CheckoutScreen({super.key});

  @override
  ConsumerState<CheckoutScreen> createState() => _CheckoutScreenState();
}

class _CheckoutScreenState extends ConsumerState<CheckoutScreen> {
  final _phoneController = TextEditingController();
  TimeOfDay? _pickupStart;
  TimeOfDay? _pickupEnd;
  bool _phonePrefilled = false;

  @override
  void dispose() {
    _phoneController.dispose();
    super.dispose();
  }

  String _formatTime(TimeOfDay time) =>
      '${time.hour.toString().padLeft(2, '0')}:${time.minute.toString().padLeft(2, '0')}';

  /// "HH:mm:ss" — OrderCreateRequest.pickupStartAt/pickupEndAt(LocalTime)이 요구하는 형식.
  /// 2026-07-27 실제 로컬 연동 테스트로 이 포맷이 맞는 걸 확인함.
  String _toApiTime(TimeOfDay time) => '${_formatTime(time)}:00';

  Future<void> _pickTime({required bool isStart}) async {
    final initial =
        (isStart ? _pickupStart : _pickupEnd) ?? TimeOfDay.now();
    final picked = await showTimePicker(context: context, initialTime: initial);
    if (picked == null) return;
    setState(() {
      if (isStart) {
        _pickupStart = picked;
      } else {
        _pickupEnd = picked;
      }
    });
  }

  int _toMinutes(TimeOfDay time) => time.hour * 60 + time.minute;

  @override
  Widget build(BuildContext context) {
    final cartAsync = ref.watch(cartViewModelProvider);
    final memberAsync = ref.watch(myInfoProvider);
    final checkoutState = ref.watch(checkoutViewModelProvider);
    final textTheme = Theme.of(context).textTheme;

    // 전화번호는 회원 정보 로딩이 끝나는 첫 프레임에 한 번만 채워준다 — 그 뒤엔 사용자가
    // 고친 값을 유지해야 하므로 매 build마다 덮어쓰면 안 된다.
    final member = memberAsync.valueOrNull;
    if (member != null && !_phonePrefilled) {
      _phonePrefilled = true;
      _phoneController.text = member.phone;
    }

    ref.listen(checkoutViewModelProvider, (previous, next) {
      final wasLoading = previous?.isLoading ?? false;
      if (!wasLoading || next.isLoading) return;

      if (next.hasError) {
        if (kDebugMode) {
          debugPrint('[checkout] ${next.error}\n${next.stackTrace}');
        }
        ScaffoldMessenger.of(context)
          ..hideCurrentSnackBar()
          ..showSnackBar(SnackBar(content: Text(next.error.toString())));
        return;
      }
      final order = next.valueOrNull;
      if (order == null) return; // 초기 상태(null) — 실제 성공 케이스가 아니다.
      _showSuccessDialog(context, order);
    });

    return Scaffold(
      appBar: AppBar(title: const Text('주문 확인')),
      body: cartAsync.when(
        data: (cart) {
          if (cart.items.isEmpty) {
            return const Center(child: Text('장바구니가 비어있어요'));
          }
          final item = cart.items.first;
          final dishAsync = ref.watch(dishProvider(item.dishId));

          return dishAsync.when(
            data: (dish) => _CheckoutForm(
              phoneController: _phoneController,
              pickupStart: _pickupStart,
              pickupEnd: _pickupEnd,
              onPickStart: () => _pickTime(isStart: true),
              onPickEnd: () => _pickTime(isStart: false),
              dishName: item.dishName,
              quantity: item.quantity,
              totalPrice: item.subtotalPrice,
              isSubmitting: checkoutState.isLoading,
              // 백엔드는 시간 순서를 검증 안 하니(OrderCreateRequest엔 @NotNull만 있음),
              // 프론트에서 최소한 "종료가 시작보다 늦어야 한다"는 어이없는 실수는 미리 막는다.
              canSubmit:
                  _phoneController.text.trim().isNotEmpty &&
                  _pickupStart != null &&
                  _pickupEnd != null &&
                  _toMinutes(_pickupEnd!) > _toMinutes(_pickupStart!),
              onSubmit: () => ref
                  .read(checkoutViewModelProvider.notifier)
                  .submit(
                    storeId: dish.storeId!,
                    dishId: item.dishId,
                    phone: _phoneController.text.trim(),
                    dishName: item.dishName,
                    quantity: item.quantity,
                    unitPrice: item.unitPrice,
                    pickupStartAt: _toApiTime(_pickupStart!),
                    pickupEndAt: _toApiTime(_pickupEnd!),
                  ),
              textTheme: textTheme,
            ),
            error: (error, _) =>
                Center(child: Text('상품 정보를 불러오지 못했어요\n$error', textAlign: TextAlign.center)),
            loading: () => const Center(child: CircularProgressIndicator()),
          );
        },
        error: (error, _) => Center(child: Text(error.toString())),
        loading: () => const Center(child: CircularProgressIndicator()),
      ),
    );
  }

  void _showSuccessDialog(BuildContext context, Order order) {
    showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (dialogContext) => AlertDialog(
        title: const Text('주문 완료!'),
        content: Text(
          '${order.dishName} ${order.quantity}개\n'
          '픽업 시간: ${order.pickupStartAt.substring(0, 5)} ~ '
          '${order.pickupEndAt.substring(0, 5)}\n'
          '결제 금액: ${order.totalPrice.toInt()}원',
        ),
        actions: [
          TextButton(
            onPressed: () {
              Navigator.of(dialogContext).pop();
              // 홈/주문내역 탭은 go_router의 StatefulShellRoute(IndexedStack)가 화면을
              // 계속 살려두기 때문에, 여기서 무효화 안 해두면 탭을 다시 봐도 주문 전
              // 캐시가 그대로 남아있는다(2026-07-27 발견 — 주문 후 주문내역 탭이 빈
              // 목록으로 보이다가 필터를 눌러야만 그제서야 새로 불러와졌던 버그).
              // 주문으로 실제 바뀌는 것들을 전부 무효화한다: 장바구니(서버가 비움),
              // 주문 목록(새 주문 추가됨), 예치금(차감됨), 이 상품의 재고(줄어듦).
              ref.invalidate(cartViewModelProvider);
              ref.invalidate(orderListViewModelProvider);
              ref.invalidate(depositBalanceProvider);
              ref.invalidate(depositHistoryProvider);
              ref.invalidate(dishProvider(order.dishId));
              // 홈 화면 카드는 dishProvider가 아니라 nearby 목록 자체(Store.dishes)에서
              // 재고를 보여주므로, 그 목록도 같이 무효화해야 홈으로 돌아갔을 때 줄어든
              // 재고가 바로 반영된다.
              ref.invalidate(storeListViewModelProvider);
              context.go(RoutePaths.home);
            },
            child: const Text('확인'),
          ),
        ],
      ),
    );
  }
}

class _CheckoutForm extends StatelessWidget {
  const _CheckoutForm({
    required this.phoneController,
    required this.pickupStart,
    required this.pickupEnd,
    required this.onPickStart,
    required this.onPickEnd,
    required this.dishName,
    required this.quantity,
    required this.totalPrice,
    required this.isSubmitting,
    required this.canSubmit,
    required this.onSubmit,
    required this.textTheme,
  });

  final TextEditingController phoneController;
  final TimeOfDay? pickupStart;
  final TimeOfDay? pickupEnd;
  final VoidCallback onPickStart;
  final VoidCallback onPickEnd;
  final String dishName;
  final int quantity;
  final num totalPrice;
  final bool isSubmitting;
  final bool canSubmit;
  final VoidCallback onSubmit;
  final TextTheme textTheme;

  String _label(TimeOfDay? time, String placeholder) => time == null
      ? placeholder
      : '${time.hour.toString().padLeft(2, '0')}:${time.minute.toString().padLeft(2, '0')}';

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(AppSpacing.md),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(AppSpacing.md),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '$dishName × $quantity',
                    style: textTheme.titleMedium?.copyWith(color: AppColors.textStrong),
                  ),
                  const SizedBox(height: AppSpacing.xs),
                  Text(
                    '결제 금액 ${totalPrice.toInt()}원 (예치금에서 즉시 차감)',
                    style: textTheme.bodySmall?.copyWith(color: AppColors.textHint),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: AppSpacing.md),
          Text('전화번호', style: textTheme.labelSmall),
          TextField(
            controller: phoneController,
            enabled: !isSubmitting,
            keyboardType: TextInputType.phone,
            decoration: const InputDecoration(hintText: '010-1234-5678'),
          ),
          const SizedBox(height: AppSpacing.md),
          Text('픽업 시간', style: textTheme.labelSmall),
          const SizedBox(height: AppSpacing.xs),
          Row(
            children: [
              Expanded(
                child: OutlinedButton(
                  onPressed: isSubmitting ? null : onPickStart,
                  child: Text(_label(pickupStart, '시작 시간 선택')),
                ),
              ),
              const SizedBox(width: AppSpacing.sm),
              const Text('~'),
              const SizedBox(width: AppSpacing.sm),
              Expanded(
                child: OutlinedButton(
                  onPressed: isSubmitting ? null : onPickEnd,
                  child: Text(_label(pickupEnd, '종료 시간 선택')),
                ),
              ),
            ],
          ),
          const SizedBox(height: AppSpacing.lg),
          ElevatedButton(
            onPressed: isSubmitting || !canSubmit ? null : onSubmit,
            child: isSubmitting
                ? const SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Text('주문하기'),
          ),
        ],
      ),
    );
  }
}
