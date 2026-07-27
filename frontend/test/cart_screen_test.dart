import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lastdish_app/domain/model/cart.dart';
import 'package:lastdish_app/domain/repository/cart_repository.dart';
import 'package:lastdish_app/presentation/cart/cart_repository_provider.dart';
import 'package:lastdish_app/presentation/cart/cart_screen.dart';

CartItem _item({int quantity = 1}) => CartItem(
  cartItemId: 10,
  dishId: 100,
  dishName: '순살치킨 서프라이즈백',
  unitPrice: 5000,
  quantity: quantity,
  subtotalPrice: 5000 * quantity,
);

class _FakeCartRepository implements CartRepository {
  _FakeCartRepository({List<CartItem>? items}) : _items = items ?? [];

  List<CartItem> _items;
  int getMyCartCallCount = 0;
  int updateCallCount = 0;
  int removeCallCount = 0;
  int clearCallCount = 0;

  @override
  Future<Cart> getMyCart() async {
    getMyCartCallCount++;
    return Cart(
      cartId: 1,
      memberId: 1,
      items: List.of(_items),
      totalPrice: _items.fold(0, (sum, i) => sum + i.subtotalPrice),
    );
  }

  @override
  Future<CartItem> addItem({
    required int cartId,
    required int dishId,
    required int quantity,
  }) async {
    throw UnimplementedError('이번 화면에는 담기 진입점이 없다');
  }

  @override
  Future<CartItem> updateItemQuantity({
    required int cartId,
    required int itemId,
    required int quantity,
  }) async {
    updateCallCount++;
    // 실제 네트워크처럼 약간의 지연을 둬서, 응답이 오기 전에 두 번째 탭이
    // 들어오는 "진짜 경합" 상황을 테스트에서 재현할 수 있게 한다.
    await Future<void>.delayed(const Duration(milliseconds: 20));
    final index = _items.indexWhere((i) => i.cartItemId == itemId);
    final updated = _items[index].copyWith(
      quantity: quantity,
      subtotalPrice: _items[index].unitPrice * quantity,
    );
    _items[index] = updated;
    return updated;
  }

  @override
  Future<void> removeItem({required int cartId, required int itemId}) async {
    removeCallCount++;
    _items.removeWhere((i) => i.cartItemId == itemId);
  }

  @override
  Future<void> clearCart(int cartId) async {
    clearCallCount++;
    _items = [];
  }
}

Future<void> _pump(WidgetTester tester, CartRepository repo) async {
  await tester.pumpWidget(
    ProviderScope(
      overrides: [cartRepositoryProvider.overrideWith((ref) => repo)],
      child: const MaterialApp(home: CartScreen()),
    ),
  );
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('장바구니가 비어있으면 안내 문구를 보여준다', (tester) async {
    final repo = _FakeCartRepository(items: []);
    await _pump(tester, repo);

    expect(find.text('장바구니가 비어있어요'), findsOneWidget);
  });

  testWidgets('상품이 있으면 이름/단가/수량/소계/합계를 보여준다', (tester) async {
    final repo = _FakeCartRepository(items: [_item(quantity: 2)]);
    await _pump(tester, repo);

    expect(find.text('순살치킨 서프라이즈백'), findsOneWidget);
    expect(find.text('개당 5000원'), findsOneWidget);
    expect(find.text('2'), findsOneWidget);
    expect(find.text('10000원'), findsWidgets); // 소계와 합계가 둘 다 10000원
  });

  testWidgets('+ 누르면 수량이 늘고 소계/합계가 갱신된다', (tester) async {
    final repo = _FakeCartRepository(items: [_item(quantity: 1)]);
    await _pump(tester, repo);

    await tester.tap(find.byIcon(Icons.add_circle_outline));
    await tester.pumpAndSettle();

    expect(repo.updateCallCount, 1);
    expect(find.text('2'), findsOneWidget);
    expect(find.text('10000원'), findsWidgets);
  });

  testWidgets('수량이 1일 때 - 버튼은 비활성화된다', (tester) async {
    final repo = _FakeCartRepository(items: [_item(quantity: 1)]);
    await _pump(tester, repo);

    final minusButton = tester.widget<IconButton>(
      find.widgetWithIcon(IconButton, Icons.remove_circle_outline),
    );
    expect(minusButton.onPressed, isNull, reason: '수량 1일 때 감소 버튼이 눌려선 안 된다');
  });

  testWidgets('수량이 2 이상이면 - 버튼이 활성화되고 누르면 수량이 준다', (tester) async {
    final repo = _FakeCartRepository(items: [_item(quantity: 2)]);
    await _pump(tester, repo);

    await tester.tap(find.byIcon(Icons.remove_circle_outline));
    await tester.pumpAndSettle();

    expect(repo.updateCallCount, 1);
    expect(find.text('1'), findsOneWidget);
  });

  testWidgets('삭제 버튼을 누르면 상품이 빠지고 빈 화면이 된다', (tester) async {
    final repo = _FakeCartRepository(items: [_item()]);
    await _pump(tester, repo);

    await tester.tap(find.byIcon(Icons.delete_outline));
    await tester.pumpAndSettle();

    expect(repo.removeCallCount, 1);
    expect(find.text('장바구니가 비어있어요'), findsOneWidget);
  });

  testWidgets('전체 비우기를 누르면 빈 화면이 된다', (tester) async {
    final repo = _FakeCartRepository(items: [_item()]);
    await _pump(tester, repo);

    await tester.tap(find.text('전체 비우기'));
    await tester.pumpAndSettle();

    expect(repo.clearCallCount, 1);
    expect(find.text('장바구니가 비어있어요'), findsOneWidget);
  });

  testWidgets('+ 를 연달아 두 번 빠르게 누르면 중복 호출 방지가 되는지', (tester) async {
    final repo = _FakeCartRepository(items: [_item(quantity: 1)]);
    await _pump(tester, repo);

    await tester.tap(find.byIcon(Icons.add_circle_outline));
    await tester.pump(); // 첫 요청이 아직 안 끝난 시점(20ms 지연 중)에 바로 한 번 더 탭
    await tester.tap(find.byIcon(Icons.add_circle_outline));
    await tester.pumpAndSettle();

    expect(
      repo.updateCallCount,
      1,
      reason: '중복 탭 방지(isBusy)가 안 먹히면 updateItemQuantity가 2번 불릴 것',
    );
  });

  testWidgets('조회 자체가 실패하면 에러 메시지를 보여준다', (tester) async {
    final repo = _FailingGetCartRepository();
    await _pump(tester, repo);

    expect(find.textContaining('장바구니를 불러올 수 없습니다'), findsOneWidget);
  });
}

class _FailingGetCartRepository implements CartRepository {
  @override
  Future<Cart> getMyCart() async {
    throw Exception('장바구니를 불러올 수 없습니다');
  }

  @override
  Future<CartItem> addItem({
    required int cartId,
    required int dishId,
    required int quantity,
  }) async => throw UnimplementedError();

  @override
  Future<CartItem> updateItemQuantity({
    required int cartId,
    required int itemId,
    required int quantity,
  }) async => throw UnimplementedError();

  @override
  Future<void> removeItem({required int cartId, required int itemId}) async {}

  @override
  Future<void> clearCart(int cartId) async {}
}
