package kr.lastdish.core.cart.application.event;

import jakarta.persistence.EntityManager;
import kr.lastdish.core.cart.domain.CartItem;
import kr.lastdish.core.cart.domain.CartItemRepository;
import kr.lastdish.core.cart.domain.CartItemStatus;
import kr.lastdish.core.common.event.EventMessage;
import kr.lastdish.core.common.event.EventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class DishStateChangedEventIntegrationTest {

  @Autowired private EventPublisher eventPublisher;

  @Autowired private CartItemRepository cartItemRepository;

  @Autowired private EntityManager entityManager;

  @Test
  void Spring_Event가_Cart_Listener에_전달되어_CartItem_상태를_변경한다() {
    // given
    CartItem cartItem =
        cartItemRepository.save(CartItem.create(1L, 10L, "김치찌개", BigDecimal.valueOf(8_000), 7L));

    EventMessage message =
        new EventMessage(
            UUID.randomUUID(),
            DishStateChangedEventListener.EVENT_TYPE,
            "DISH",
            10L,
            1L,
            2,
            "{\"available\":true,\"stockQuantity\":5}",
            Instant.now());

    // when
    eventPublisher.publish(message);

    /*
     * 영속성 컨텍스트에 있는 객체가 아니라 실제 DB 반영 결과를 확인하기 위해
     * 변경 내용을 flush한 뒤 영속성 컨텍스트를 초기화합니다.
     */
    entityManager.flush();
    entityManager.clear();

    CartItem updatedCartItem = cartItemRepository.findById(cartItem.getId()).orElseThrow();

    // then
    assertThat(updatedCartItem.getStatus()).isEqualTo(CartItemStatus.INSUFFICIENT_STOCK);
    assertThat(updatedCartItem.getLastAppliedDishVersion()).isEqualTo(1L);

    assertThat(updatedCartItem.isOrderable()).isFalse();
  }
}
