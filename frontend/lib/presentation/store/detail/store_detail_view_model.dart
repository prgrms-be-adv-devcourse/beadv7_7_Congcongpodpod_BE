import 'package:riverpod/riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../../domain/model/store.dart';
import '../store_repository_provider.dart';

part 'store_detail_view_model.g.dart';

/// 매장 상세 조회. 로그인/목록과 달리 클래스형 Notifier가 아니라 "함수형" Provider다 —
/// 커맨드(로그인, 새로고침 같은 동작)가 없고 storeId 하나로 결과가 정해지는 순수 조회라서
/// 이 편이 더 단순하다. 코드젠이 매개변수(storeId)를 보고 자동으로 "family"
/// (storeId마다 상태를 따로 캐싱하는 Provider)를 만들어준다 — 매장 A 상세를 보다가
/// 매장 B 상세로 가면 서로 다른 결과가 각자 캐싱된다.
///
/// 화면에서는 `ref.watch(storeDetailViewModelProvider(storeId))`처럼 파라미터를 넣어서 쓴다.
@riverpod
Future<Store> storeDetailViewModel(Ref ref, int storeId) {
  final repository = ref.watch(storeRepositoryProvider);
  return repository.getStoreDetail(storeId);
}
