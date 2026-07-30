import 'package:flutter/foundation.dart' show kDebugMode;
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/presentation/components/info_row.dart';
import '../../../core/routing/route_paths.dart';
import '../../../domain/model/dish.dart';
import '../../../domain/model/dish_status.dart';
import '../../../ui/app_colors.dart';
import '../../../ui/app_spacing.dart';
import '../../dish/dish_providers.dart';
import '../../store/list/store_list_view_model.dart';
import '../seller_store_id_provider.dart';
import '../store/seller_store_view_model.dart';
import 'seller_dish_view_model.dart';

/// 상품 등록/관리 화면 (S2, `/seller/dishes`). 매장:상품이 1:1(ADR 004)이라 목록이
/// 아니라 단건 — `GET /stores/{storeId}/dish`(판매 상태 무관, 2026-07-27 백엔드 신규
/// 반영, 이슈 #121)로 조회한다.
class SellerDishScreen extends ConsumerWidget {
  const SellerDishScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final storeIdAsync = ref.watch(sellerStoreIdProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('상품 등록/관리')),
      body: storeIdAsync.when(
        data: (storeId) => storeId == null
            ? _NoStoreYet(
                onRegister: () => context.push(RoutePaths.sellerStore),
              )
            : _DishManager(storeId: storeId),
        error: (error, _) => Center(child: Text(error.toString())),
        loading: () => const Center(child: CircularProgressIndicator()),
      ),
    );
  }
}

class _NoStoreYet extends StatelessWidget {
  const _NoStoreYet({required this.onRegister});

  final VoidCallback onRegister;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(AppSpacing.lg),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text(
              '먼저 매장을 등록해야 상품을 등록할 수 있어요',
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: AppSpacing.md),
            ElevatedButton(
              onPressed: onRegister,
              child: const Text('매장 등록하러 가기'),
            ),
          ],
        ),
      ),
    );
  }
}

class _DishManager extends ConsumerStatefulWidget {
  const _DishManager({required this.storeId});

  final int storeId;

  @override
  ConsumerState<_DishManager> createState() => _DishManagerState();
}

class _DishManagerState extends ConsumerState<_DishManager> {
  bool _editing = false;

  @override
  Widget build(BuildContext context) {
    final dishAsync = ref.watch(sellerDishProvider(widget.storeId));

    ref.listen(sellerDishActionViewModelProvider, (previous, next) {
      final wasLoading = previous?.isLoading ?? false;
      if (!wasLoading || next.isLoading) return;

      if (next.hasError) {
        if (kDebugMode)
          debugPrint('[seller_dish] ${next.error}\n${next.stackTrace}');
        ScaffoldMessenger.of(context)
          ..hideCurrentSnackBar()
          ..showSnackBar(SnackBar(content: Text(next.error.toString())));
        return;
      }
      if (next.valueOrNull == null) return; // 초기 상태.
      ref.invalidate(sellerDishProvider(widget.storeId));
      // 홈 화면(store_list_screen.dart)은 매장 목록에 dishes[]를 그대로
      // 임베딩해서 보여주는데(ADR 018) 이건 별도 캐시라, 여기만 무효화하면
      // 상품 수정/품절·마감 처리해도 홈에는 반영이 안 됐다(2026-07-30 발견).
      // 상품상세 캐시(dishProvider)도 체크아웃 화면 등에서 재사용되니 같이 비운다.
      ref.invalidate(storeListViewModelProvider);
      final dishId = dishAsync.valueOrNull?.dishId;
      if (dishId != null) ref.invalidate(dishProvider(dishId));
      setState(() => _editing = false);
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('반영했어요')));
    });

    return dishAsync.when(
      data: (dish) {
        if (dish == null || _editing) {
          return _DishForm(
            storeId: widget.storeId,
            existing: dish,
            onCancel: dish == null
                ? null
                : () => setState(() => _editing = false),
          );
        }
        // Scaffold body가 남은 화면 높이를 그대로 넘겨주는데, Padding+Card만
        // 반환하면 Card가 그 높이를 그대로 채워서 카드 배경이 화면 끝까지
        // 길게 늘어났다(2026-07-30 발견) — Align으로 내용물 높이만큼만
        // 차지하게 top으로 붙인다.
        return Align(
          alignment: Alignment.topCenter,
          child: Padding(
            padding: const EdgeInsets.all(AppSpacing.md),
            child: _DishCard(
              dish: dish,
              onEdit: () => setState(() => _editing = true),
            ),
          ),
        );
      },
      error: (error, _) => Center(child: Text('$error')),
      loading: () => const Center(child: CircularProgressIndicator()),
    );
  }
}

class _DishCard extends ConsumerWidget {
  const _DishCard({required this.dish, required this.onEdit});

  final Dish dish;
  final VoidCallback onEdit;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final textTheme = Theme.of(context).textTheme;
    final status = dish.dishStatus ?? 'ON_SALE';
    final notifier = ref.read(sellerDishActionViewModelProvider.notifier);

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(AppSpacing.md),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          // Column 기본값(mainAxisSize.max)이라 Align이 풀어준 여유 공간을
          // Column이 그대로 다 차지해버렸다(2026-07-30) — min으로 내용물
          // 높이만큼만 차지하게 한다.
          mainAxisSize: MainAxisSize.min,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    dish.dishName,
                    style: textTheme.titleMedium?.copyWith(
                      color: AppColors.textStrong,
                    ),
                  ),
                ),
                // Material Chip은 Flutter Web에서 한글 라벨 끝글자를 잘라먹는다
                // (troubleshooting 010, store_list_screen.dart 카테고리 뱃지와 같은
                // 문제라 2026-07-30에 같이 고침) — Container+Text로 직접 그린다.
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(
                    color: AppColors.primaryLight,
                    borderRadius: BorderRadius.circular(999),
                  ),
                  child: Text(
                    dishStatusLabel(status),
                    softWrap: false,
                    style: textTheme.labelSmall?.copyWith(color: AppColors.primaryDark),
                  ),
                ),
              ],
            ),
            const SizedBox(height: AppSpacing.sm),
            const Divider(height: 1),
            const SizedBox(height: AppSpacing.sm),
            InfoRow(label: '재고', value: '${dish.stockQuantity}개'),
            InfoRow(
              label: '가격',
              value:
                  '${dish.dishPrice.toInt()}원 → ${dish.discountPrice.toInt()}원',
            ),
            const SizedBox(height: AppSpacing.sm),
            Wrap(
              spacing: AppSpacing.sm,
              children: [
                OutlinedButton(onPressed: onEdit, child: const Text('수정')),
                if (status == 'ON_SALE') ...[
                  OutlinedButton(
                    style: OutlinedButton.styleFrom(
                      foregroundColor: AppColors.error,
                      side: const BorderSide(color: AppColors.error),
                    ),
                    onPressed: () => notifier.changeStatus(
                      dishId: dish.dishId,
                      dishStatus: 'SOLD_OUT',
                    ),
                    child: const Text('품절 처리'),
                  ),
                  OutlinedButton(
                    style: OutlinedButton.styleFrom(
                      foregroundColor: AppColors.error,
                      side: const BorderSide(color: AppColors.error),
                    ),
                    onPressed: () => notifier.changeStatus(
                      dishId: dish.dishId,
                      dishStatus: 'CLOSED',
                    ),
                    child: const Text('마감 처리'),
                  ),
                ] else if (status == 'SOLD_OUT' || status == 'CLOSED')
                  OutlinedButton(
                    onPressed: () => notifier.changeStatus(
                      dishId: dish.dishId,
                      dishStatus: 'ON_SALE',
                    ),
                    child: const Text('판매중으로 전환'),
                  ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _DishForm extends ConsumerStatefulWidget {
  const _DishForm({
    required this.storeId,
    required this.existing,
    required this.onCancel,
  });

  final int storeId;
  final Dish? existing;

  /// null이면 취소 버튼 자체를 안 보여준다 — 상품이 아예 없는 최초 등록 상태에서는
  /// "취소"해서 돌아갈 목록 화면이 없기 때문(dish == null이면 이 폼이 곧 이 화면 전체).
  final VoidCallback? onCancel;

  @override
  ConsumerState<_DishForm> createState() => _DishFormState();
}

class _DishFormState extends ConsumerState<_DishForm> {
  final _dishNameController = TextEditingController();
  final _descriptionController = TextEditingController();
  final _stockController = TextEditingController();
  final _priceController = TextEditingController();
  final _discountPriceController = TextEditingController();

  bool get _isEdit => widget.existing != null;

  @override
  void initState() {
    super.initState();
    final dish = widget.existing;
    if (dish != null) {
      _dishNameController.text = dish.dishName;
      _descriptionController.text = dish.description ?? '';
      _stockController.text = dish.stockQuantity.toString();
      _priceController.text = dish.dishPrice.toInt().toString();
      _discountPriceController.text = dish.discountPrice.toInt().toString();
    }
  }

  @override
  void dispose() {
    _dishNameController.dispose();
    _descriptionController.dispose();
    _stockController.dispose();
    _priceController.dispose();
    _discountPriceController.dispose();
    super.dispose();
  }

  String _nowIso() {
    final now = DateTime.now();
    String pad(int n) => n.toString().padLeft(2, '0');
    return '${now.year}-${pad(now.month)}-${pad(now.day)}T'
        '${pad(now.hour)}:${pad(now.minute)}:${pad(now.second)}';
  }

  void _submit() {
    final notifier = ref.read(sellerDishActionViewModelProvider.notifier);
    final stock = int.tryParse(_stockController.text.trim()) ?? 0;
    final price = num.tryParse(_priceController.text.trim()) ?? 0;
    final discountPrice =
        num.tryParse(_discountPriceController.text.trim()) ?? 0;
    final description = _descriptionController.text.trim().isEmpty
        ? null
        : _descriptionController.text.trim();

    if (_isEdit) {
      notifier.updateDish(
        dishId: widget.existing!.dishId,
        dishName: _dishNameController.text.trim(),
        registeredAt: widget.existing!.registeredAt,
        description: description,
        stockQuantity: stock,
        dishPrice: price,
        discountPrice: discountPrice,
      );
    } else {
      // Dish 등록 API가 픽업시간을 서버에서 채워주지 않아서(백엔드가 Store를
      // 직접 조회하면 store↔dish 순환 의존이 생김, 2026-07-30) 여기서 매장
      // 영업시간을 그대로 실어 보낸다. build()에서 이미 watch해둔 값이라
      // 보통 준비돼 있고, 혹시 아직 로딩 중이면 null로 보내 기존처럼
      // "픽업 시간 미정" 처리로 안전하게 빠진다.
      final store = ref
          .read(sellerStoreDetailProvider(widget.storeId))
          .valueOrNull;
      notifier.create(
        storeId: widget.storeId,
        dishName: _dishNameController.text.trim(),
        registeredAt: _nowIso(),
        description: description,
        stockQuantity: stock,
        dishPrice: price,
        discountPrice: discountPrice,
        pickupStartTime: store?.openTime,
        pickupEndTime: store?.closeTime,
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(sellerDishActionViewModelProvider);
    // 매장 영업시간을 미리 조회해둔다 — _submit()에서 그대로 실어 보낸다.
    ref.watch(sellerStoreDetailProvider(widget.storeId));
    final textTheme = Theme.of(context).textTheme;

    return SingleChildScrollView(
      padding: const EdgeInsets.all(AppSpacing.md),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(_isEdit ? '상품 수정' : '새 상품 등록', style: textTheme.titleSmall),
          const SizedBox(height: AppSpacing.sm),
          TextField(
            controller: _dishNameController,
            decoration: const InputDecoration(labelText: '상품명'),
          ),
          const SizedBox(height: AppSpacing.sm),
          TextField(
            controller: _descriptionController,
            decoration: const InputDecoration(labelText: '설명(선택)'),
          ),
          const SizedBox(height: AppSpacing.sm),
          TextField(
            controller: _stockController,
            keyboardType: TextInputType.number,
            decoration: const InputDecoration(labelText: '재고 수량'),
          ),
          const SizedBox(height: AppSpacing.sm),
          Row(
            children: [
              Expanded(
                child: TextField(
                  controller: _priceController,
                  keyboardType: TextInputType.number,
                  decoration: const InputDecoration(labelText: '정가'),
                ),
              ),
              const SizedBox(width: AppSpacing.sm),
              Expanded(
                child: TextField(
                  controller: _discountPriceController,
                  keyboardType: TextInputType.number,
                  decoration: const InputDecoration(labelText: '할인가'),
                ),
              ),
            ],
          ),
          const SizedBox(height: AppSpacing.md),
          SizedBox(
            height: 48,
            child: Row(
              // ElevatedButton만 전역 테마로 높이가 강제돼서(app_theme.dart)
              // OutlinedButton과 높이가 안 맞았다 — stretch로 맞춘다. Row가
              // SingleChildScrollView 안에 있어 높이가 무한이라, SizedBox로
              // Row 자체 높이를 먼저 고정해야 stretch가 제대로 동작한다.
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                if (widget.onCancel != null) ...[
                  Expanded(
                    child: OutlinedButton(
                      onPressed: state.isLoading ? null : widget.onCancel,
                      child: const Text('취소'),
                    ),
                  ),
                  const SizedBox(width: AppSpacing.sm),
                ],
                Expanded(
                  child: ElevatedButton(
                    onPressed: state.isLoading ? null : _submit,
                    child: state.isLoading
                        ? const SizedBox(
                            width: 20,
                            height: 20,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : Text(_isEdit ? '수정' : '등록'),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
