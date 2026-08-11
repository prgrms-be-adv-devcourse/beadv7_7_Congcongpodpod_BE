package kr.lastdish.core.order.application;

import java.time.LocalDateTime;
import kr.lastdish.core.order.application.event.OrderStatusChangedEventWriter;
import kr.lastdish.core.order.domain.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PickupExpirationService {

  private final OrderRepository orderRepository;
  private final OrderStatusChangedEventWriter orderStatusChangedEventWriter;

  @Transactional
  public int expire(LocalDateTime now) {
    var expirationTargets = orderRepository.findPickupExpirationTargets(now);

    expirationTargets.forEach(
        order -> {
          order.markNoShow();
          orderStatusChangedEventWriter.append(order);
        });
    return expirationTargets.size();
  }
}
