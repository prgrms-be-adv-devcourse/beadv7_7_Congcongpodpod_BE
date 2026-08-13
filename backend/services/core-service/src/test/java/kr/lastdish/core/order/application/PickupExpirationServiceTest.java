package kr.lastdish.core.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import kr.lastdish.core.store.application.StoreFacade;
import org.junit.jupiter.api.Test;

class PickupExpirationServiceTest {

  private final StoreFacade storeFacade = mock(StoreFacade.class);
  private final PickupExpirationStoreProcessor storeProcessor =
      mock(PickupExpirationStoreProcessor.class);
  private final PickupExpirationService pickupExpirationService =
      new PickupExpirationService(storeFacade, storeProcessor);

  @Test
  void 매장_마감_시_미픽업_주문과_상품_판매를_종료한다() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 10, 19, 0);
    List<Long> closingStoreIds = List.of(1L, 2L);
    when(storeFacade.findStoreIdsReadyToClose(now)).thenReturn(closingStoreIds);
    when(storeProcessor.expireStore(1L, now)).thenReturn(2);
    when(storeProcessor.expireStore(2L, now)).thenReturn(1);

    int expiredCount = pickupExpirationService.expire(now);

    assertThat(expiredCount).isEqualTo(3);
    verify(storeProcessor).expireStore(1L, now);
    verify(storeProcessor).expireStore(2L, now);
  }

  @Test
  void 한_매장_처리가_실패해도_다른_매장은_계속_처리한다() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 10, 19, 0);
    when(storeFacade.findStoreIdsReadyToClose(now)).thenReturn(List.of(1L, 2L));
    when(storeProcessor.expireStore(1L, now)).thenThrow(new RuntimeException("failure"));
    when(storeProcessor.expireStore(2L, now)).thenReturn(1);

    assertThat(pickupExpirationService.expire(now)).isEqualTo(1);
    verify(storeFacade).rescheduleNextClosingAtAfterFailure(1L, now);
    verify(storeProcessor).expireStore(2L, now);
  }

  @Test
  void 실패한_매장의_다음_마감_시각_갱신이_실패해도_다른_매장은_계속_처리한다() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 10, 19, 0);
    when(storeFacade.findStoreIdsReadyToClose(now)).thenReturn(List.of(1L, 2L));
    when(storeProcessor.expireStore(1L, now)).thenThrow(new RuntimeException("failure"));
    doThrow(new RuntimeException("reschedule failure"))
        .when(storeFacade)
        .rescheduleNextClosingAtAfterFailure(1L, now);
    when(storeProcessor.expireStore(2L, now)).thenReturn(1);

    assertThat(pickupExpirationService.expire(now)).isEqualTo(1);
    verify(storeProcessor).expireStore(2L, now);
  }
}
