package kr.lastdish.core.order.application;

import java.time.LocalDateTime;
import java.util.List;
import kr.lastdish.core.dish.application.DishService;
import kr.lastdish.core.order.application.event.OrderStatusChangedEventWriter;
import kr.lastdish.core.order.domain.OrderRepository;
import kr.lastdish.core.store.application.StoreFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PickupExpirationService {

  private final OrderRepository orderRepository;
  private final OrderStatusChangedEventWriter orderStatusChangedEventWriter;
  private final StoreFacade storeFacade;
  private final DishService dishService;

  // 마감 시각이 지난 매장을 찾고, 해당 매장의 미픽업 주문과 상품 마감 처리
  @Transactional
  public int expire(LocalDateTime now) {
    List<Long> closingStoreIds = storeFacade.claimStoresReadyToClose(now);

    if (closingStoreIds.isEmpty()) {
      return 0;
    }

    var expirationTargets = orderRepository.findPickupExpirationTargets(closingStoreIds);

    expirationTargets.forEach(
        order -> {
          order.markNoShow(now);
          orderStatusChangedEventWriter.append(order);
        });

    closingStoreIds.forEach(dishService::closeSaleByStoreId);
    return expirationTargets.size();
  }
}
