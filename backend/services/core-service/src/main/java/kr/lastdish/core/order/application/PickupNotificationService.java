package kr.lastdish.core.order.application;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import kr.lastdish.core.order.application.event.OrderNotificationEventWriter;
import kr.lastdish.core.order.domain.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PickupNotificationService {

  private static final long DEADLINE_NOTICE_MINUTES = 15L;

  private final OrderRepository orderRepository;
  private final OrderNotificationEventWriter notificationEventWriter;

  @Transactional
  public int notifyDueOrders(LocalDateTime now) {
    LocalDateTime minuteStart = now.truncatedTo(ChronoUnit.MINUTES);
    LocalDateTime minuteEnd = minuteStart.plusMinutes(1);

    var pickupStartTargets =
        orderRepository.findPickupStartNotificationTargets(
            minuteStart.toLocalTime(), minuteStart, minuteStart.plusDays(1));
    pickupStartTargets.forEach(notificationEventWriter::appendPickupStarted);

    var deadlineSoonTargets =
        orderRepository.findPickupDeadlineSoonNotificationTargets(
            minuteStart.plusMinutes(DEADLINE_NOTICE_MINUTES),
            minuteEnd.plusMinutes(DEADLINE_NOTICE_MINUTES));
    deadlineSoonTargets.forEach(notificationEventWriter::appendPickupDeadlineSoon);

    return pickupStartTargets.size() + deadlineSoonTargets.size();
  }
}
