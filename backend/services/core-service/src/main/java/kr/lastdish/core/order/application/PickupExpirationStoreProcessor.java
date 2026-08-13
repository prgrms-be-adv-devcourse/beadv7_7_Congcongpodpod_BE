package kr.lastdish.core.order.application;

import java.time.LocalDateTime;
import kr.lastdish.core.dish.application.DishService;
import kr.lastdish.core.order.application.event.OrderStatusChangedEventWriter;
import kr.lastdish.core.order.domain.OrderRepository;
import kr.lastdish.core.store.application.StoreFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PickupExpirationStoreProcessor {

  private final OrderRepository orderRepository;
  private final OrderStatusChangedEventWriter orderStatusChangedEventWriter;
  private final StoreFacade storeFacade;
  private final DishService dishService;

  // 매장별 트랜잭션 분리하여 배치 처리
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public int expireStore(Long storeId, LocalDateTime now) {
    storeFacade.rescheduleNextClosingAt(storeId, now);

    var expirationTargets = orderRepository.findPickupExpirationTargets(storeId, now);
    expirationTargets.forEach(
        order -> {
          order.markNoShow(now);
          orderStatusChangedEventWriter.append(order);
        });

    dishService.closeSaleByStoreId(storeId);
    return expirationTargets.size();
  }
}
