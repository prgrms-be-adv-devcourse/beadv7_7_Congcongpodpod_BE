import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lastdish_app/domain/model/store.dart';
import 'package:lastdish_app/domain/repository/store_repository.dart';
import 'package:lastdish_app/presentation/store/detail/store_detail_screen.dart';
import 'package:lastdish_app/presentation/store/store_repository_provider.dart';

class _FakeStoreRepository implements StoreRepository {
  _FakeStoreRepository({this.detail, this.error});

  Store? detail;
  Object? error;

  @override
  Future<List<Store>> getNearbyStores({
    required double latitude,
    required double longitude,
    double radiusKm = 3,
    int page = 0,
    int size = 10,
  }) async => [];

  @override
  Future<Store> getStoreDetail(int storeId) async {
    if (error != null) throw error!;
    return detail!;
  }
}

void main() {
  testWidgets('매장 정보를 보여준다(주소/전화/영업시간/휴무일)', (tester) async {
    final repo = _FakeStoreRepository(
      detail: const Store(
        storeId: 1,
        storeName: '김밥천국',
        storeAddress: '서울시 강남구 1번지',
        storePhone: '02-000-0001',
        openTime: '09:00',
        closeTime: '21:00',
        latitude: 37.4979,
        longitude: 127.0276,
        holidays: ['MONDAY'],
        status: 'OPEN',
      ),
    );

    await tester.pumpWidget(
      ProviderScope(
        overrides: [storeRepositoryProvider.overrideWith((ref) => repo)],
        child: const MaterialApp(home: StoreDetailScreen(storeId: 1)),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('김밥천국'), findsOneWidget);
    expect(find.text('서울시 강남구 1번지'), findsOneWidget);
    expect(find.text('02-000-0001'), findsOneWidget);
    expect(find.text('09:00 ~ 21:00'), findsOneWidget);
    expect(find.text('MONDAY'), findsOneWidget);
    expect(find.text('상품 목록은 곧 제공될 예정이에요'), findsOneWidget);
  });

  testWidgets('휴무일이 없으면 휴무일 행 자체를 안 보여준다', (tester) async {
    final repo = _FakeStoreRepository(
      detail: const Store(
        storeId: 1,
        storeName: '김밥천국',
        storeAddress: '서울시 강남구 1번지',
        storePhone: '02-000-0001',
        openTime: '09:00',
        closeTime: '21:00',
        latitude: 37.4979,
        longitude: 127.0276,
        status: 'OPEN',
      ),
    );

    await tester.pumpWidget(
      ProviderScope(
        overrides: [storeRepositoryProvider.overrideWith((ref) => repo)],
        child: const MaterialApp(home: StoreDetailScreen(storeId: 1)),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('휴무일'), findsNothing);
  });

  testWidgets('조회 실패 시 에러 메시지를 보여준다', (tester) async {
    final repo = _FakeStoreRepository(error: Exception('매장을 찾을 수 없습니다'));

    await tester.pumpWidget(
      ProviderScope(
        overrides: [storeRepositoryProvider.overrideWith((ref) => repo)],
        child: const MaterialApp(home: StoreDetailScreen(storeId: 999)),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.textContaining('매장을 찾을 수 없습니다'), findsOneWidget);
  });
}
