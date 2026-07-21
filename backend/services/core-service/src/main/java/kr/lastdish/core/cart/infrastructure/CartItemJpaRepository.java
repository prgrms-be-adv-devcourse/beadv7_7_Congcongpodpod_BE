package kr.lastdish.core.cart.infrastructure;

import java.util.List;
import java.util.Optional;
import kr.lastdish.core.cart.domain.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemJpaRepository extends JpaRepository<CartItem, Long> {
  Optional<CartItem> findByCartId(Long cartId);

  List<CartItem> findAllByDishId(Long dishId);

  void deleteByCartId(Long cartId);
}
