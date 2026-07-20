package kr.lastdish.core.cart.application;

import java.util.List;
import kr.lastdish.core.cart.domain.Cart;
import kr.lastdish.core.cart.domain.CartItem;
import kr.lastdish.core.cart.domain.CartItemRepository;
import kr.lastdish.core.cart.domain.CartRepository;
import kr.lastdish.core.cart.presentation.dto.CartItemAddRequest;
import kr.lastdish.core.cart.presentation.dto.CartItemResponse;
import kr.lastdish.core.cart.presentation.dto.CartItemUpdateRequest;
import kr.lastdish.core.cart.presentation.dto.CartResponse;
import kr.lastdish.core.common.exception.BusinessException;
import kr.lastdish.core.common.exception.ErrorCode;
import kr.lastdish.core.dish.application.DishFacade;
import kr.lastdish.core.dish.application.dto.DishSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartService {

  private final CartRepository cartRepository;
  private final CartItemRepository cartItemRepository;
  private final DishFacade dishFacade;

  @Transactional
  public CartItemResponse addItem(Long cartId, CartItemAddRequest request) {
    Cart cart = getCartOrThrow(cartId);
    DishSnapshot dish =
        dishFacade
            .findDishSnapshot(request.dishId())
            .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "상품을 찾을 수 없습니다."));

    long quantity = request.quantity() != null ? request.quantity() : 1L;
    if (quantity > dish.stockQuantity()) {
      throw new BusinessException(ErrorCode.SOLD_OUT);
    }

    // 장바구니 1개 = 상품 1개: 기존에 담긴 상품이 있으면 교체(upsert), 없으면 새로 담는다.
    CartItem cartItem =
        cartItemRepository
            .findByCartId(cart.getId())
            .map(
                existing -> {
                  existing.replace(dish.dishId(), dish.dishName(), dish.unitPrice(), quantity);
                  return existing;
                })
            .orElseGet(
                () ->
                    cartItemRepository.save(
                        CartItem.create(
                            cart.getId(),
                            dish.dishId(),
                            dish.dishName(),
                            dish.unitPrice(),
                            quantity)));

    return CartItemResponse.from(cartItem);
  }

  // 회원의 장바구니를 조회한다. 없으면 새로 만든다. Cart는 별도의 생성 API가 없음.
  @Transactional
  public CartResponse getCartByMemberId(Long memberId) {
    Cart cart =
        cartRepository
            .findByMemberId(memberId)
            .orElseGet(() -> cartRepository.save(Cart.create(memberId)));
    return toResponse(cart);
  }

  @Transactional
  public CartItemResponse updateItemQuantity(
      Long cartId, Long itemId, CartItemUpdateRequest request) {
    getCartOrThrow(cartId);
    CartItem cartItem = getCartItemOrThrow(cartId, itemId);
    cartItem.changeQuantity(request.quantity());
    return CartItemResponse.from(cartItem);
  }

  @Transactional
  public void removeItem(Long cartId, Long itemId) {
    getCartOrThrow(cartId);
    CartItem cartItem = getCartItemOrThrow(cartId, itemId);
    cartItemRepository.delete(cartItem);
  }

  @Transactional
  public void clearCart(Long cartId) {
    Cart cart = getCartOrThrow(cartId);
    cartItemRepository.deleteByCartId(cart.getId());
  }

  private Cart getCartOrThrow(Long cartId) {
    return cartRepository
        .findById(cartId)
        .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "장바구니를 찾을 수 없습니다."));
  }

  private CartItem getCartItemOrThrow(Long cartId, Long itemId) {
    CartItem cartItem =
        cartItemRepository
            .findById(itemId)
            .orElseThrow(
                () -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "장바구니 상품을 찾을 수 없습니다."));
    if (!cartItem.getCartId().equals(cartId)) {
      throw new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "장바구니 상품을 찾을 수 없습니다.");
    }
    return cartItem;
  }

  private CartResponse toResponse(Cart cart) {
    List<CartItem> items =
        cartItemRepository.findByCartId(cart.getId()).map(List::of).orElseGet(List::of);
    return CartResponse.of(cart, items);
  }
}
