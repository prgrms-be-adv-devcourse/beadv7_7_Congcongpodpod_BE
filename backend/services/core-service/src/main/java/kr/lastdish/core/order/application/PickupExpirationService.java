package kr.lastdish.core.order.application;

import java.time.LocalDateTime;
import java.util.List;
import kr.lastdish.core.store.application.StoreFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PickupExpirationService {

  private final StoreFacade storeFacade;
  private final PickupExpirationStoreProcessor storeProcessor;

  // 마감 시각이 지난 매장을 찾고, 해당 매장의 미픽업 주문과 상품 마감 처리
  public int expire(LocalDateTime now) {
    List<Long> closingStoreIds = storeFacade.findStoreIdsReadyToClose(now);
    int expiredCount = 0;

    for (Long storeId : closingStoreIds) {
      try {
        expiredCount += storeProcessor.expireStore(storeId, now);
      } catch (RuntimeException exception) {
        log.error("매장 마감 처리에 실패하여 수동 처리가 필요합니다. storeId={}, closingAt={}", storeId, now, exception);
        rescheduleFailedStore(storeId, now);
      }
    }
    return expiredCount;
  }

  private void rescheduleFailedStore(Long storeId, LocalDateTime now) {
    try {
      storeFacade.rescheduleNextClosingAtAfterFailure(storeId, now);
    } catch (RuntimeException exception) {
      log.error("마감 실패 매장의 다음 마감 시각 갱신에도 실패했습니다. storeId={}", storeId, exception);
    }
  }
}
