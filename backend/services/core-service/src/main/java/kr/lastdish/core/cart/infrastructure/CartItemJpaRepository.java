package kr.lastdish.core.cart.infrastructure;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import kr.lastdish.core.cart.domain.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface CartItemJpaRepository extends JpaRepository<CartItem, Long> {
  Optional<CartItem> findByCartId(Long cartId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  List<CartItem> findAllByDishId(Long dishId);

  void deleteByCartId(Long cartId);
}
