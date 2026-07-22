package kr.lastdish.core.cart.infrastructure;

import java.util.Optional;
import kr.lastdish.core.cart.domain.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartJpaRepository extends JpaRepository<Cart, Long> {
  Optional<Cart> findByMemberId(Long memberId);
}
