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
          .updateItemQuantity(
            cartId: cartId,
            itemId: itemId,
            quantity: quantity,
          ),
    );
  }

  Future<void> removeItem(int itemId) {
    final cartId = state.requireValue.cartId;
    return _mutate(
      () => ref
          .read(cartRepositoryProvider)
          .removeItem(cartId: cartId, itemId: itemId),
    );
  }

  Future<void> clear() {
    final cartId = state.requireValue.cartId;
    return _mutate(() => ref.read(cartRepositoryProvider).clearCart(cartId));
  }

  /// 변경 요청 하나를 보내고, 끝나면 최신 카트로 상태를 통째로 다시 채운다.
  ///
  /// 버튼 중복 탭 방지는 여기(state.isLoading)로 하지 않는다 — 직접 해봤다가
  /// 알게 된 것인데, `state = AsyncLoading()`을 수동으로 지정하면 Riverpod이 이걸
  /// "완전히 새로 로딩"(isReloading)으로 취급해 화면이 카트 대신 로딩 스피너로
  /// 통째로 바뀌어버린다(`ref.invalidateSelf()`가 만드는 "이어서 로딩"/isRefreshing과는
  /// 다른 경로다). 그래서 중복 탭 방지는 화면 쪽 로컬 상태(cart_screen.dart의
  /// `_isMutating`)로 옮기고, 여기서는 순수하게 "요청 보내고 최신 데이터로 채우기"만 한다.
  Future<void> _mutate(Future<void> Function() action) async {
    state = await AsyncValue.guard(() async {
      await action();
      return ref.read(cartRepositoryProvider).getMyCart();
    });
  }
}
