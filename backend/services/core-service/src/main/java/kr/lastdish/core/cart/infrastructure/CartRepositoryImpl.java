package kr.lastdish.core.cart.infrastructure;

import java.util.Optional;
import kr.lastdish.core.cart.domain.Cart;
import kr.lastdish.core.cart.domain.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CartRepositoryImpl implements CartRepository {
  private final CartJpaRepository cartJpaRepository;

  @Override
  public Cart save(Cart cart) {
    return cartJpaRepository.save(cart);
  }

  @Override
  public Optional<Cart> findById(Long id) {
    return cartJpaRepository.findById(id);
  }

  @Override
  public Optional<Cart> findByMemberId(Long memberId) {
    return cartJpaRepository.findByMemberId(memberId);
  }
}
