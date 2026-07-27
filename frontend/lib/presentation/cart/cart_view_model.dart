import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../domain/model/cart.dart';
import 'cart_repository_provider.dart';

part 'cart_view_model.g.dart';

/// 장바구니 화면의 상태. store_list_view_model.dart와 같은 이유로 `AsyncNotifier<Cart>` —
/// 화면에 데이터(Cart) 자체가 필요해서 build()가 조회 결과를 그대로 상태로 들고 있는다.
///
/// 수량변경/삭제/비우기 이후에 카트를 "부분적으로" 다시 계산하지 않고, 매번 서버에서
/// 통째로 다시 받아온다 — 장바구니가 상품 1개뿐이라 그 비용이
/// 아주 작아서, 굳이 로컬에서 subtotal/totalPrice를 다시 계산하는 로직을 만들지 않았다
/// (틀리기 쉬운 계산을 서버에 맡기는 게 더 안전하기도 하다).
@riverpod
class CartViewModel extends _$CartViewModel {
  @override
  Future<Cart> build() {
    final repository = ref.watch(cartRepositoryProvider);
    return repository.getMyCart();
  }

  Future<void> updateQuantity(int itemId, int quantity) {
    final cartId = state.requireValue.cartId;
    return _mutate(
      () => ref
          .read(cartRepositoryProvider)
          .updateItemQuantity(cartId: cartId, itemId: itemId, quantity: quantity),
    );
  }

  Future<void> removeItem(int itemId) {
    final cartId = state.requireValue.cartId;
    return _mutate(
      () => ref.read(cartRepositoryProvider).removeItem(cartId: cartId, itemId: itemId),
    );
  }

  Future<void> clear() {
    final cartId = state.requireValue.cartId;
    return _mutate(() => ref.read(cartRepositoryProvider).clearCart(cartId));
  }

  /// 변경 요청 하나를 보내고, 성공하면 최신 카트로 상태를 다시 채운다.
  ///
  /// 버튼 중복 탭 방지는 여기(state.isLoading)로 하지 않는다 — 직접 해봤다가
  /// 알게 된 것인데, `state = AsyncLoading()`을 수동으로 지정하면 Riverpod이 이걸
  /// "완전히 새로 로딩"(isReloading)으로 취급해 화면이 카트 대신 로딩 스피너로
  /// 통째로 바뀌어버린다(`ref.invalidateSelf()`가 만드는 "이어서 로딩"/isRefreshing과는
  /// 다른 경로다). 그래서 중복 탭 방지는 화면 쪽 로컬 상태(cart_screen.dart의
  /// `_isMutating`)로 옮기고, 여기서는 순수하게 "요청 보내고 최신 데이터로 채우기"만 한다.
  ///
  /// ⚠️ 2026-07-27 수정: 예전엔 `AsyncValue.guard`로 감싸서 실패하면 state를
  /// `AsyncError`로 바꿨는데, 그러면 `cart_screen.dart`의 `cartAsync.when()`이 error
  /// 분기로 넘어가면서 화면에 이미 그려져 있던 카트(상품/수량 등)가 통째로 에러 메시지로
  /// 바뀌어버렸다 — 재고 초과처럼 "이번 변경 하나만 실패한" 상황인데도 마치 상품이
  /// 사라진 것처럼 보이는 버그였다. 이제는 실패하면 state를 안 건드리고(마지막으로
  /// 성공했던 카트가 화면에 그대로 남는다) 예외만 위로 던져서, 호출부(cart_screen.dart의
  /// `_run`)가 스낵바 하나로만 알려주게 한다.
  Future<void> _mutate(Future<void> Function() action) async {
    await action();
    state = AsyncData(await ref.read(cartRepositoryProvider).getMyCart());
  }
}
