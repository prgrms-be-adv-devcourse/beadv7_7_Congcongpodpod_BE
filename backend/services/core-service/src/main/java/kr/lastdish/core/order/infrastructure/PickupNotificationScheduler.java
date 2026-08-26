package kr.lastdish.core.order.infrastructure;

import java.time.LocalDateTime;
import java.time.ZoneId;
import kr.lastdish.core.order.application.PickupNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PickupNotificationScheduler {

  private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");

  private final PickupNotificationService pickupNotificationService;

  @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
  public void notifyPickupSchedule() {
    pickupNotificationService.notifyDueOrders(LocalDateTime.now(BUSINESS_ZONE));
  }
}
