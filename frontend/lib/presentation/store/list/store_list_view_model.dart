import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../../domain/model/store.dart';
import '../store_repository_provider.dart';

part 'store_list_view_model.g.dart';

/// 매장 목록 화면의 상태. 로그인 ViewModel(`AsyncValue<void>`, "성공했나만 중요")과 달리
/// 여기선 데이터 자체(`List<Store>`)가 화면에 필요하다 — 그래서 `build()`가 API 결과를
/// 그대로 반환하고, `AsyncNotifier`가 그 반환값을 초기 상태로 잡아준다. 화면은
/// `ref.watch(storeListViewModelProvider)`로 이 상태를 읽고 `.when(data/error/loading)`으로 그린다.
///
/// ⚠️ 위/경도는 지금 하드코딩이다(강남역 부근 좌표) — 실기기 위치 권한 연동은
/// 범위 밖으로 미뤄뒀다.
/// 나중에 실제 위치로 바꿀 때는 이 두 상수를 GPS 값으로 교체하기만 하면 된다.
///
/// `ref.watch(selectedStoreCategoryProvider)`로 카테고리 필터 상태를 구독한다 — 사용자가
/// 칩을 눌러 필터를 바꾸면(`SelectedStoreCategory.select`) 그 상태가 바뀌고, Riverpod이
/// 이 Provider를 자동으로 다시 빌드해서 새 카테고리로 목록을 다시 조회해준다(수동으로
/// `refresh()`를 부를 필요 없음 — `ref.watch`가 의존성을 추적해주는 덕분).
@riverpod
class StoreListViewModel extends _$StoreListViewModel {
  static const _mockLatitude = 37.4979; // 강남역 부근(임시)
  static const _mockLongitude = 127.0276;

  @override
  Future<List<Store>> build() {
    final repository = ref.watch(storeRepositoryProvider);
    final category = ref.watch(selectedStoreCategoryProvider);
    return repository.getNearbyStores(
      latitude: _mockLatitude,
      longitude: _mockLongitude,
      category: category,
    );
  }

  /// 당겨서 새로고침(pull-to-refresh)용. `ref.invalidateSelf()`로 이 Provider를
  /// 무효화하면 `build()`가 처음부터 다시 실행되고, `future`로 그 결과를 기다린다 —
  /// RefreshIndicator는 이 Future가 끝나야 스피너를 멈춘다.
  Future<void> refresh() async {
    ref.invalidateSelf();
    await future;
  }
}

/// 홈 화면 상단 카테고리 필터 칩이 선택한 값. `null`이면 "전체"(필터 없음).
/// 상태 자체는 아주 단순해서(선택된 문자열 하나) `AsyncNotifier`가 아니라 그냥
/// 동기 `Notifier`로 둔다 — API 호출은 여기서 안 하고, 이 상태를 지켜보는
/// `StoreListViewModel.build()` 쪽에서 한다(관심사 분리).
@riverpod
class SelectedStoreCategory extends _$SelectedStoreCategory {
  @override
  String? build() => null;

  void select(String? category) => state = category;
}
