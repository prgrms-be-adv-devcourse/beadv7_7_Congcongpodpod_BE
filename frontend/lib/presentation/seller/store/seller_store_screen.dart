import 'package:flutter/foundation.dart' show kDebugMode;
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../domain/model/category.dart';
import '../../../domain/model/store.dart';
import '../../../ui/app_spacing.dart';
import '../seller_store_id_provider.dart';
import 'seller_store_view_model.dart';

/// 매장 등록/수정 화면 (S1, `/seller/store`). storeId 캐시가 없으면 등록 폼,
/// 있으면 그 매장 정보를 불러와 수정 폼으로 보여준다 — storeId 확보 방법 자체가
/// 임시 방편이라(seller_store_id_provider.dart 참고) 이 화면이 그 캐시를 채우는
/// 유일한 지점이다.
///
/// 위/경도·휴무일은 입력 UI 없이 하드코딩(강남역 부근) — 실기기 위치 연동/휴무일
/// 선택 UI는 범위 밖(store_list_view_model.dart의 홈 화면과 같은 가정).
class SellerStoreScreen extends ConsumerWidget {
  const SellerStoreScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final storeIdAsync = ref.watch(sellerStoreIdProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('S1 · 매장 등록/수정')),
      body: storeIdAsync.when(
        data: (storeId) => storeId == null
            ? const _StoreForm(existing: null)
            : ref
                  .watch(sellerStoreDetailProvider(storeId))
                  .when(
                    data: (store) => _StoreForm(existing: store),
                    error: (error, _) =>
                        Center(child: Text('매장 정보를 불러오지 못했어요\n$error')),
                    loading: () => const Center(child: CircularProgressIndicator()),
                  ),
        error: (error, _) => Center(child: Text(error.toString())),
        loading: () => const Center(child: CircularProgressIndicator()),
      ),
    );
  }
}

class _StoreForm extends ConsumerStatefulWidget {
  const _StoreForm({required this.existing});

  /// null이면 등록 모드, 값이 있으면 그 매장 수정 모드.
  final Store? existing;

  @override
  ConsumerState<_StoreForm> createState() => _StoreFormState();
}

class _StoreFormState extends ConsumerState<_StoreForm> {
  // 강남역 부근 — store_list_view_model.dart의 홈 화면 목록 조회와 같은 임시 좌표.
  static const _fixedLatitude = 37.4979;
  static const _fixedLongitude = 127.0276;

  final _storeNameController = TextEditingController();
  final _businessNumberController = TextEditingController();
  final _storeAddressController = TextEditingController();
  final _storePhoneController = TextEditingController();
  TimeOfDay _openTime = const TimeOfDay(hour: 9, minute: 0);
  TimeOfDay _closeTime = const TimeOfDay(hour: 22, minute: 0);
  String _category = categoryValues.first;
  bool _prefilled = false;

  bool get _isEdit => widget.existing != null;

  @override
  void initState() {
    super.initState();
    _prefillIfNeeded();
  }

  void _prefillIfNeeded() {
    if (_prefilled || widget.existing == null) return;
    _prefilled = true;
    final store = widget.existing!;
    _storeNameController.text = store.storeName;
    _storeAddressController.text = store.storeAddress;
    _storePhoneController.text = store.storePhone;
    _openTime = _parseTime(store.openTime);
    _closeTime = _parseTime(store.closeTime);
    _category = store.category;
  }

  TimeOfDay _parseTime(String hhmm) {
    final parts = hhmm.split(':');
    return TimeOfDay(hour: int.parse(parts[0]), minute: int.parse(parts[1]));
  }

  String _formatTime(TimeOfDay time) =>
      '${time.hour.toString().padLeft(2, '0')}:${time.minute.toString().padLeft(2, '0')}';

  @override
  void dispose() {
    _storeNameController.dispose();
    _businessNumberController.dispose();
    _storeAddressController.dispose();
    _storePhoneController.dispose();
    super.dispose();
  }

  Future<void> _pickTime({required bool isOpen}) async {
    final initial = isOpen ? _openTime : _closeTime;
    final picked = await showTimePicker(context: context, initialTime: initial);
    if (picked == null) return;
    setState(() {
      if (isOpen) {
        _openTime = picked;
      } else {
        _closeTime = picked;
      }
    });
  }

  void _submit() {
    final notifier = ref.read(sellerStoreViewModelProvider.notifier);
    if (_isEdit) {
      notifier.updateStore(
        storeId: widget.existing!.storeId,
        storeName: _storeNameController.text.trim(),
        storeAddress: _storeAddressController.text.trim(),
        storePhone: _storePhoneController.text.trim(),
        openTime: _formatTime(_openTime),
        closeTime: _formatTime(_closeTime),
        latitude: _fixedLatitude,
        longitude: _fixedLongitude,
        category: _category,
      );
    } else {
      notifier.register(
        storeName: _storeNameController.text.trim(),
        businessNumber: _businessNumberController.text.trim(),
        storeAddress: _storeAddressController.text.trim(),
        storePhone: _storePhoneController.text.trim(),
        openTime: _formatTime(_openTime),
        closeTime: _formatTime(_closeTime),
        latitude: _fixedLatitude,
        longitude: _fixedLongitude,
        category: _category,
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(sellerStoreViewModelProvider);
    final textTheme = Theme.of(context).textTheme;

    ref.listen(sellerStoreViewModelProvider, (previous, next) {
      final wasLoading = previous?.isLoading ?? false;
      if (!wasLoading || next.isLoading) return;

      if (next.hasError) {
        if (kDebugMode) debugPrint('[seller_store] ${next.error}\n${next.stackTrace}');
        ScaffoldMessenger.of(context)
          ..hideCurrentSnackBar()
          ..showSnackBar(SnackBar(content: Text(next.error.toString())));
        return;
      }
      final store = next.valueOrNull;
      if (store == null) return;
      if (!_isEdit) {
        // 등록 성공 — sellerStoreIdProvider를 무효화해서 GET /stores/mine을 다시
        // 불러오면 방금 등록한 매장이 바로 반영된다.
        ref.invalidate(sellerStoreIdProvider);
      }
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(_isEdit ? '매장 정보를 수정했어요' : '매장을 등록했어요')),
      );
    });

    final canSubmit =
        _storeNameController.text.trim().isNotEmpty &&
        _storeAddressController.text.trim().isNotEmpty &&
        _storePhoneController.text.trim().isNotEmpty &&
        (_isEdit || _businessNumberController.text.trim().isNotEmpty);

    return SingleChildScrollView(
      padding: const EdgeInsets.all(AppSpacing.md),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          if (_isEdit)
            Padding(
              padding: const EdgeInsets.only(bottom: AppSpacing.sm),
              child: Text('매장 #${widget.existing!.storeId} 수정', style: textTheme.labelSmall),
            ),
          Text('매장명', style: textTheme.labelSmall),
          TextField(controller: _storeNameController, onChanged: (_) => setState(() {})),
          const SizedBox(height: AppSpacing.md),
          Text('사업자등록번호', style: textTheme.labelSmall),
          TextField(
            controller: _businessNumberController,
            enabled: !_isEdit,
            decoration: InputDecoration(
              hintText: '123-45-67890',
              helperText: _isEdit ? '등록 후에는 수정할 수 없어요' : null,
            ),
            onChanged: (_) => setState(() {}),
          ),
          const SizedBox(height: AppSpacing.md),
          Text('주소', style: textTheme.labelSmall),
          TextField(controller: _storeAddressController, onChanged: (_) => setState(() {})),
          const SizedBox(height: AppSpacing.md),
          Text('매장 전화번호', style: textTheme.labelSmall),
          TextField(
            controller: _storePhoneController,
            keyboardType: TextInputType.phone,
            onChanged: (_) => setState(() {}),
          ),
          const SizedBox(height: AppSpacing.md),
          Text('카테고리', style: textTheme.labelSmall),
          DropdownButton<String>(
            value: _category,
            isExpanded: true,
            items: [
              for (final value in categoryValues)
                DropdownMenuItem(value: value, child: Text(categoryLabel(value))),
            ],
            onChanged: (value) => setState(() => _category = value!),
          ),
          const SizedBox(height: AppSpacing.md),
          Text('영업 시간', style: textTheme.labelSmall),
          const SizedBox(height: AppSpacing.xs),
          Row(
            children: [
              Expanded(
                child: OutlinedButton(
                  onPressed: () => _pickTime(isOpen: true),
                  child: Text(_formatTime(_openTime)),
                ),
              ),
              const SizedBox(width: AppSpacing.sm),
              const Text('~'),
              const SizedBox(width: AppSpacing.sm),
              Expanded(
                child: OutlinedButton(
                  onPressed: () => _pickTime(isOpen: false),
                  child: Text(_formatTime(_closeTime)),
                ),
              ),
            ],
          ),
          const SizedBox(height: AppSpacing.lg),
          ElevatedButton(
            onPressed: state.isLoading || !canSubmit ? null : _submit,
            child: state.isLoading
                ? const SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : Text(_isEdit ? '수정하기' : '등록하기'),
          ),
        ],
      ),
    );
  }
}
