package kr.lastdish.core.cart.domain;

import java.util.Optional;

public interface CartRepository {
  Cart save(Cart cart);

  Optional<Cart> findById(Long id);

  Optional<Cart> findByMemberId(Long memberId);
}
