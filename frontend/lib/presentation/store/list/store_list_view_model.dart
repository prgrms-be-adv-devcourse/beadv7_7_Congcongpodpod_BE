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
@riverpod
class StoreListViewModel extends _$StoreListViewModel {
  static const _mockLatitude = 37.4979; // 강남역 부근(임시)
  static const _mockLongitude = 127.0276;

  @override
  Future<List<Store>> build() {
    final repository = ref.watch(storeRepositoryProvider);
    return repository.getNearbyStores(
      latitude: _mockLatitude,
      longitude: _mockLongitude,
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
