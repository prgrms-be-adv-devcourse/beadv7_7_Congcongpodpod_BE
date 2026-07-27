import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:lastdish_app/core/routing/route_paths.dart';
import 'package:lastdish_app/domain/model/store.dart';
import 'package:lastdish_app/domain/repository/store_repository.dart';
import 'package:lastdish_app/presentation/store/list/store_list_screen.dart';
import 'package:lastdish_app/presentation/store/store_repository_provider.dart';

Store _store(int id, String name) => Store(
  storeId: id,
  storeName: name,
  storeAddress: '서울시 강남구 $id번지',
  storePhone: '02-000-000$id',
  openTime: '09:00',
  closeTime: '21:00',
  latitude: 37.4979,
  longitude: 127.0276,
  status: 'OPEN',
);

class _FakeStoreRepository implements StoreRepository {
  _FakeStoreRepository({this.stores, this.error});

  List<Store>? stores;
  Object? error;
  int callCount = 0;

  @override
  Future<List<Store>> getNearbyStores({
    required double latitude,
    required double longitude,
    double radiusKm = 3,
    int page = 0,
    int size = 10,
  }) async {
    callCount++;
    if (error != null) throw error!;
    return stores ?? [];
  }

  @override
  Future<Store> getStoreDetail(int storeId) async {
    return _store(storeId, '매장 $storeId');
  }
}

GoRouter _router() {
  return GoRouter(
    initialLocation: RoutePaths.home,
    routes: [
      GoRoute(
        path: RoutePaths.home,
        builder: (context, state) => const StoreListScreen(),
      ),
      GoRoute(
        path: RoutePaths.storeDetail,
        builder: (context, state) =>
            Text('STORE_DETAIL_${state.pathParameters['storeId']}'),
      ),
      GoRoute(
        path: RoutePaths.cart,
        builder: (context, state) => const Text('CART_SCREEN'),
      ),
    ],
  );
}

void main() {
  testWidgets('매장이 있으면 목록으로 보여주고, 탭하면 상세로 이동한다', (tester) async {
    final repo = _FakeStoreRepository(
      stores: [_store(1, '김밥천국'), _store(2, '국밥집')],
    );

    await tester.pumpWidget(
      ProviderScope(
        overrides: [storeRepositoryProvider.overrideWith((ref) => repo)],
        child: MaterialApp.router(routerConfig: _router()),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('김밥천국'), findsOneWidget);
    expect(find.text('국밥집'), findsOneWidget);

    await tester.tap(find.text('김밥천국'));
    await tester.pumpAndSettle();

    expect(find.text('STORE_DETAIL_1'), findsOneWidget);
  });

  testWidgets('매장이 없으면 빈 안내 문구를 보여준다', (tester) async {
    final repo = _FakeStoreRepository(stores: []);

    await tester.pumpWidget(
      ProviderScope(
        overrides: [storeRepositoryProvider.overrideWith((ref) => repo)],
        child: MaterialApp.router(routerConfig: _router()),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('주변에 등록된 매장이 없어요'), findsOneWidget);
  });

  testWidgets('실패하면 에러 메시지와 다시 시도 버튼을 보여주고, 눌러 재조회한다', (tester) async {
    final repo = _FakeStoreRepository(error: Exception('네트워크 오류'));

    await tester.pumpWidget(
      ProviderScope(
        overrides: [storeRepositoryProvider.overrideWith((ref) => repo)],
        child: MaterialApp.router(routerConfig: _router()),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.textContaining('네트워크 오류'), findsOneWidget);
    expect(repo.callCount, 1);

    await tester.tap(find.text('다시 시도'));
    await tester.pumpAndSettle();

    expect(repo.callCount, 2, reason: '다시 시도 버튼이 재조회를 안 함');
  });

  testWidgets('당겨서 새로고침하면 목록을 다시 불러온다', (tester) async {
    final repo = _FakeStoreRepository(stores: [_store(1, '김밥천국')]);

    await tester.pumpWidget(
      ProviderScope(
        overrides: [storeRepositoryProvider.overrideWith((ref) => repo)],
        child: MaterialApp.router(routerConfig: _router()),
      ),
    );
    await tester.pumpAndSettle();
    expect(repo.callCount, 1);

    await tester.fling(find.byType(ListView), const Offset(0, 300), 1000);
    await tester.pumpAndSettle();

    expect(repo.callCount, 2, reason: 'pull-to-refresh가 재조회를 안 함');
  });

  testWidgets('장바구니 아이콘을 누르면 장바구니 화면으로 이동한다', (tester) async {
    final repo = _FakeStoreRepository(stores: []);

    await tester.pumpWidget(
      ProviderScope(
        overrides: [storeRepositoryProvider.overrideWith((ref) => repo)],
        child: MaterialApp.router(routerConfig: _router()),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.byIcon(Icons.shopping_cart_outlined));
    await tester.pumpAndSettle();

    expect(find.text('CART_SCREEN'), findsOneWidget);
  });
}
