package kr.lastdish.core.cart.domain;

import java.util.Optional;

public interface CartItemRepository {
  CartItem save(CartItem cartItem);

  Optional<CartItem> findById(Long id);

  Optional<CartItem> findByCartId(Long cartId);

  void delete(CartItem cartItem);

  void deleteByCartId(Long cartId);
}
