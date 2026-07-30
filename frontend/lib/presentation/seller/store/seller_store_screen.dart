import 'package:flutter/foundation.dart' show kDebugMode;
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/presentation/phone_format.dart';
import '../../../core/presentation/phone_input_formatter.dart';
import '../../../domain/model/category.dart';
import '../../../domain/model/store.dart';
import '../../../ui/app_spacing.dart';
import '../../auth/auth_repository_provider.dart';
import '../../member/member_repository_provider.dart';
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
      appBar: AppBar(title: const Text('매장 등록/수정')),
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

  // 백엔드(StoreCreateRequest)는 @NotBlank만 검증하고 형식은 안 본다 — 사업자등록번호
  // "123-45-67890" 형식은 프론트에서만 강제한다. 서버가 나중에 형식 검증을 추가해도
  // 이 정규식이 더 엄격한 쪽이라 안전하다.
  static final _businessNumberPattern = RegExp(r'^\d{3}-\d{2}-\d{5}$');

  static const _storeNameMaxLength = 50;

  final _storeNameController = TextEditingController();
  final _businessNumberController = TextEditingController();
  final _storeAddressController = TextEditingController();
  final _storePhoneController = TextEditingController();
  final _businessNumberFocus = FocusNode();
  final _storePhoneFocus = FocusNode();
  TimeOfDay _openTime = const TimeOfDay(hour: 9, minute: 0);
  TimeOfDay _closeTime = const TimeOfDay(hour: 22, minute: 0);
  String _category = categoryValues.first;
  bool _prefilled = false;

  // 사업자등록번호/전화번호는 자리수가 딱 맞아떨어지기 전까지는(타이핑 중이든
  // 지우는 중이든) "완성 전" 상태라 형식 에러에 걸리는 게 정상이다 — 근데 그걸 매
  // 키 입력마다 바로 보여주면 편집하는 동안 계속 빨간 글씨가 깜빡인다(2026-07-30
  // 발견). 그래서 이 두 필드는 포커스를 벗어난 뒤(한 번이라도 편집을 "끝낸" 뒤)에만
  // 에러를 보여준다 — 나머지 필드(매장명/주소)는 "비어있으면 에러"뿐이라 이미
  // isEmpty로 자연스럽게 숨겨지므로 이 처리가 따로 필요 없다.
  bool _businessNumberTouched = false;
  bool _storePhoneTouched = false;

  bool get _isEdit => widget.existing != null;

  @override
  void initState() {
    super.initState();
    _prefillIfNeeded();
    _businessNumberFocus.addListener(() {
      if (!_businessNumberFocus.hasFocus) {
        setState(() => _businessNumberTouched = true);
      }
    });
    _storePhoneFocus.addListener(() {
      if (!_storePhoneFocus.hasFocus) {
        setState(() => _storePhoneTouched = true);
      }
    });
  }

  void _prefillIfNeeded() {
    if (_prefilled || widget.existing == null) return;
    _prefilled = true;
    final store = widget.existing!;
    _storeNameController.text = store.storeName;
    _storeAddressController.text = store.storeAddress;
    _storePhoneController.text = formatPhone(store.storePhone);
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

  String? get _storeNameError {
    final value = _storeNameController.text.trim();
    if (value.isEmpty) return '매장명을 입력해 주세요';
    if (value.length > _storeNameMaxLength) return '매장명은 $_storeNameMaxLength자 이하로 입력해 주세요';
    return null;
  }

  String? get _businessNumberError {
    if (_isEdit) return null; // 등록 후엔 수정 불가 필드라 검증할 값 자체가 없음.
    final value = _businessNumberController.text.trim();
    if (value.isEmpty) return '사업자등록번호를 입력해 주세요';
    if (!_businessNumberPattern.hasMatch(value)) {
      return '000-00-00000 형식으로 입력해 주세요';
    }
    return null;
  }

  String? get _storeAddressError {
    if (_storeAddressController.text.trim().isEmpty) return '주소를 입력해 주세요';
    return null;
  }

  String? get _storePhoneError {
    final value = _storePhoneController.text.trim();
    if (value.isEmpty) return '매장 전화번호를 입력해 주세요';
    // 하이픈은 화면 표시용일 뿐 서버로 보낼 땐 다 떼어낸다(store_repository_impl.dart의
    // _digitsOnly) — 그래서 검증도 하이픈 위치가 아니라 숫자 자리수만 본다. 포맷터가
    // 타이핑/삭제 도중에 만들어내는 그룹 모양(2-3-4든 3-4-4든)과 무관하게, 최종
    // 자리수(9~11)만 맞으면 통과한다.
    final digits = value.replaceAll(RegExp(r'[^0-9]'), '');
    if (digits.length < 9 || digits.length > 11 || !digits.startsWith('0')) {
      return '전화번호 형식이 올바르지 않아요 (예: 02-1234-5678, 010-1234-5678)';
    }
    return null;
  }

  String? get _businessHoursError {
    // 새벽까지 영업하는 매장은 마감 시간이 오픈 시간보다 숫자상 이르다(예: 18:00
    // 오픈 ~ 02:00 마감 = 다음날 새벽 2시). 백엔드도 openTime/closeTime을 그냥
    // LocalTime 두 개로만 갖고 있고 순서를 강제하지 않는다(Store.java 확인,
    // 2026-07-30) — 그래서 여기서도 순서는 안 따지고, 오픈과 마감이 완전히
    // 같아서 영업시간이 0분이 되는 경우만 막는다.
    if (_openTime.hour == _closeTime.hour && _openTime.minute == _closeTime.minute) {
      return '오픈 시간과 마감 시간이 같을 수 없어요';
    }
    return null;
  }

  bool get _isFormValid =>
      _storeNameError == null &&
      _businessNumberError == null &&
      _storeAddressError == null &&
      _storePhoneError == null &&
      _businessHoursError == null;

  @override
  void dispose() {
    _storeNameController.dispose();
    _businessNumberController.dispose();
    _storeAddressController.dispose();
    _storePhoneController.dispose();
    _businessNumberFocus.dispose();
    _storePhoneFocus.dispose();
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
        // 등록 성공 시점의 access token은 아직 role=MEMBER다 — 서버가 role을
        // SELLER로 올려준 건 DB 상태일 뿐, 이미 발급된 토큰의 role 클레임은
        // 재발급 전까진 그대로다. 이 상태로 sellerStoreIdProvider를 바로
        // invalidate하면 GET /stores/mine(SELLER 전용)이 예전 토큰으로 나가서
        // 403이 난다(2026-07-30 발견). refresh()로 새 role이 반영된 토큰을 먼저
        // 받아온 뒤에 invalidate해야 한다.
        //
        // 등록 성공 후에도 이 화면(등록 폼)에 그대로 머물러 있으면 —
        // sellerStoreIdProvider가 새로 채워지면서 같은 화면이 "수정" 모드로
        // 다시 그려질 뿐이라 사용자는 등록이 됐는지 헷갈린다(2026-07-30 발견,
        // 등록 후 화면이 안 넘어간다는 제보). 그래서 refresh/invalidate가 끝난
        // 뒤 이 화면을 열었던 마이페이지로 돌아간다 — 이 화면은 항상
        // mypage_screen.dart에서 push로 열리므로 pop 한 번이면 된다.
        () async {
          final authRepository = await ref.read(authRepositoryProvider.future);
          await authRepository.refresh();
          ref.invalidate(sellerStoreIdProvider);
          // myInfoProvider(마이페이지의 구매자/판매자 배지, 메뉴 분기)는 GET
          // /members/me 응답의 role을 그대로 쓰는데, 이건 DB 값이라 토큰
          // 재발급과 무관하게 이미 SELLER로 나온다 — 그래도 마이페이지가 push로
          // 열려있던 동안 캐시된 이전 값을 계속 들고 있을 수 있어 별도로
          // 무효화해야 한다.
          ref.invalidate(myInfoProvider);
          if (context.mounted) context.pop();
        }();
      }
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(_isEdit ? '매장 정보를 수정했어요' : '매장을 등록했어요')),
      );
    });

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
          TextField(
            controller: _storeNameController,
            decoration: InputDecoration(
              errorText: _storeNameController.text.trim().isEmpty ? null : _storeNameError,
            ),
            onChanged: (_) => setState(() {}),
          ),
          const SizedBox(height: AppSpacing.md),
          Text('사업자등록번호', style: textTheme.labelSmall),
          TextField(
            controller: _businessNumberController,
            focusNode: _businessNumberFocus,
            enabled: !_isEdit,
            keyboardType: TextInputType.number,
            inputFormatters: [BusinessNumberInputFormatter()],
            decoration: InputDecoration(
              hintText: '123-45-67890',
              helperText: _isEdit ? '등록 후에는 수정할 수 없어요' : null,
              errorText: !_businessNumberTouched ? null : _businessNumberError,
            ),
            onChanged: (_) => setState(() {}),
          ),
          const SizedBox(height: AppSpacing.md),
          Text('주소', style: textTheme.labelSmall),
          TextField(
            controller: _storeAddressController,
            decoration: InputDecoration(
              errorText: _storeAddressController.text.trim().isEmpty ? null : _storeAddressError,
            ),
            onChanged: (_) => setState(() {}),
          ),
          const SizedBox(height: AppSpacing.md),
          Text('매장 전화번호', style: textTheme.labelSmall),
          TextField(
            controller: _storePhoneController,
            focusNode: _storePhoneFocus,
            keyboardType: TextInputType.phone,
            inputFormatters: [PhoneNumberInputFormatter()],
            decoration: InputDecoration(
              hintText: '02-1234-5678',
              errorText: !_storePhoneTouched ? null : _storePhoneError,
            ),
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
          if (_businessHoursError != null) ...[
            const SizedBox(height: AppSpacing.xs),
            Text(
              _businessHoursError!,
              style: textTheme.labelSmall?.copyWith(color: Theme.of(context).colorScheme.error),
            ),
          ],
          const SizedBox(height: AppSpacing.lg),
          ElevatedButton(
            onPressed: state.isLoading || !_isFormValid ? null : _submit,
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
