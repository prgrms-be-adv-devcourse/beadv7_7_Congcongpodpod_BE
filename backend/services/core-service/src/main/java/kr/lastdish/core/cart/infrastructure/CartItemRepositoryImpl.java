package kr.lastdish.core.cart.infrastructure;

import java.util.Optional;
import kr.lastdish.core.cart.domain.CartItem;
import kr.lastdish.core.cart.domain.CartItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CartItemRepositoryImpl implements CartItemRepository {
  private final CartItemJpaRepository cartItemJpaRepository;

  @Override
  public CartItem save(CartItem cartItem) {
    return cartItemJpaRepository.save(cartItem);
  }

  @Override
  public Optional<CartItem> findById(Long id) {
    return cartItemJpaRepository.findById(id);
  }

  @Override
  public Optional<CartItem> findByCartId(Long cartId) {
    return cartItemJpaRepository.findByCartId(cartId);
  }

  @Override
  public void delete(CartItem cartItem) {
    cartItemJpaRepository.delete(cartItem);
  }

  @Override
  public void deleteByCartId(Long cartId) {
    cartItemJpaRepository.deleteByCartId(cartId);
  }
}
