package kr.lastdish.core.order.infrastructure;

import static kr.lastdish.core.order.application.OrderService.PICKUP_EXPIRATION_BATCH_SIZE;

import java.time.LocalDateTime;
import java.time.ZoneId;
import kr.lastdish.core.order.application.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PickupExpirationScheduler {

  private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");
  private final OrderService orderService;

  @Scheduled(cron = "${pickup.expiration.cron:0 0 * * * *}", zone = "Asia/Seoul")
  public int expirePickupOrders() {
    LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
    int totalExpiredCount = 0;
    int expiredCount;

    do {
      expiredCount = orderService.expirePickupOrders(now);
      totalExpiredCount += expiredCount;
    } while (expiredCount == PICKUP_EXPIRATION_BATCH_SIZE);

    return totalExpiredCount;
  }
}
