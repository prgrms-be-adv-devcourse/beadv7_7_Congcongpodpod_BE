package kr.lastdish.core.cart.application.event;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import kr.lastdish.common.event.EventMessage;
import kr.lastdish.common.event.EventPublisher;
import kr.lastdish.core.cart.domain.CartItem;
import kr.lastdish.core.cart.domain.CartItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class DishPriceChangedEventIntegrationTest {

  @Autowired private EventPublisher eventPublisher;

  @Autowired private CartItemRepository cartItemRepository;

  @Autowired private EntityManager entityManager;

  @Test
  void Spring_Event가_Cart_Listener에_전달되어_CartItem_가격을_변경한다() {
    // given
    CartItem cartItem =
        cartItemRepository.save(
            CartItem.create(1L, 10L, "김치찌개", BigDecimal.valueOf(8_000), 2L, 1L));

    EventMessage message =
        new EventMessage(
            UUID.randomUUID(),
            DishPriceChangedEventListener.EVENT_TYPE,
            "DISH",
            10L,
            2L,
            1,
            "{\"unitPrice\":7000}",
            Instant.now());

    // when
    eventPublisher.publish(message);
    entityManager.flush();
    entityManager.clear();

    CartItem updatedCartItem = cartItemRepository.findById(cartItem.getId()).orElseThrow();

    // then
    assertThat(updatedCartItem.getUnitPrice()).isEqualByComparingTo("7000");
    assertThat(updatedCartItem.getSubtotalPrice()).isEqualByComparingTo("14000");
    assertThat(updatedCartItem.getLastAppliedDishPriceVersion()).isEqualTo(2L);
  }
}
