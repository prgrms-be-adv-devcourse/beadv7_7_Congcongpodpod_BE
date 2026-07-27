import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../ui/app_colors.dart';
import '../../../ui/app_spacing.dart';
import 'store_detail_view_model.dart';

/// 매장 상세 화면 (B4). 지금은 매장 정보만 보여준다 — "판매 중인 상품 목록" 섹션은
/// 매장별 Dish 목록 조회 API가 아직 없어서 플레이스홀더만 둔다. API가 생기면 이 화면의
/// 아래쪽 Card 하나만 실제 목록 위젯으로 바꾸면 된다.
class StoreDetailScreen extends ConsumerWidget {
  const StoreDetailScreen({required this.storeId, super.key});

  final int storeId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    // family Provider(storeDetailViewModelProvider(storeId))는 파라미터를 넣어서 호출한다.
    // storeId가 다르면 Riverpod이 완전히 별개의 Provider로 취급해서 결과도 따로 캐싱한다 —
    // 매장 A를 본 다음 매장 B를 봐도 서로 섞이지 않는다.
    final storeAsync = ref.watch(storeDetailViewModelProvider(storeId));
    final textTheme = Theme.of(context).textTheme;

    return Scaffold(
      appBar: AppBar(title: const Text('매장 상세')),
      body: storeAsync.when(
        data: (store) => SingleChildScrollView(
          padding: const EdgeInsets.all(AppSpacing.md),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                store.storeName,
                style: textTheme.headlineSmall?.copyWith(
                  color: AppColors.textStrong,
                ),
              ),
              const SizedBox(height: AppSpacing.md),
              _InfoRow(label: '주소', value: store.storeAddress),
              _InfoRow(label: '전화', value: store.storePhone),
              _InfoRow(
                label: '영업시간',
                value: '${store.openTime} ~ ${store.closeTime}',
              ),
              if (store.holidays != null && store.holidays!.isNotEmpty)
                _InfoRow(label: '휴무일', value: store.holidays!.join(', ')),
              const SizedBox(height: AppSpacing.xl),
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(AppSpacing.md),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('판매 중인 상품', style: textTheme.titleMedium),
                      const SizedBox(height: AppSpacing.xs),
                      Text(
                        '상품 목록은 곧 제공될 예정이에요',
                        style: textTheme.bodySmall?.copyWith(
                          color: AppColors.textHint,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
        error: (error, stackTrace) => Center(
          child: Padding(
            padding: const EdgeInsets.all(AppSpacing.lg),
            child: Text(error.toString(), textAlign: TextAlign.center),
          ),
        ),
        loading: () => const Center(child: CircularProgressIndicator()),
      ),
    );
  }
}

/// "라벨: 값" 한 줄. 매장 정보처럼 짧은 항목이 여러 개 나열될 때 반복 쓰기 위한
/// 아주 작은 재사용 위젯 — 스타일 하나 바꾸면 모든 줄에 한 번에 적용된다.
class _InfoRow extends StatelessWidget {
  const _InfoRow({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    return Padding(
      padding: const EdgeInsets.only(bottom: AppSpacing.xs),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 64,
            child: Text(
              label,
              style: textTheme.bodySmall?.copyWith(color: AppColors.textHint),
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
