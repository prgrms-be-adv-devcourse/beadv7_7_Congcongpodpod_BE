import 'package:riverpod/riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../store/store_repository_provider.dart';

part 'seller_store_id_provider.g.dart';

/// 로그인한 셀러의 storeId. `GET /stores/mine`(2026-07-27 백엔드 반영,
/// adr-draft-seller-store-id-resolution.md 참고)으로 직접 얻는다 — 셀러당 매장
/// 1개를 가정하고 첫 번째 것만 쓴다(여러 개면 나중에 선택 UI 필요, 지금은 범위 밖).
/// 매장이 아직 없으면(신규 셀러) null — S2/S3가 이 값으로 "먼저 매장 등록" 안내를 보여준다.
///
/// 이전엔 로컬(SharedPreferences) 캐싱이었으나, 저 API가 실제로 나와서 걷어냈다 —
/// S1(매장 등록) 성공 시 이 provider를 invalidate하면 새로 등록된 매장이 바로 반영된다.
@riverpod
Future<int?> sellerStoreId(Ref ref) async {
  final stores = await ref.watch(storeRepositoryProvider).getMyStores();
  return stores.isEmpty ? null : stores.first.storeId;
}
