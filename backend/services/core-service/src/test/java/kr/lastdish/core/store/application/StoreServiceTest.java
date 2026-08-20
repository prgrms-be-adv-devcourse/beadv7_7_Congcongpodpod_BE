package kr.lastdish.core.store.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.api.exception.CommonErrorCode;
import kr.lastdish.common.outbox.application.OutboxEventWriter;
import kr.lastdish.core.common.exception.ErrorCode;
import kr.lastdish.core.store.application.dto.StoreResult;
import kr.lastdish.core.store.application.dto.UpdateStoreCommand;
import kr.lastdish.core.store.domain.*;
import kr.lastdish.core.store.domain.event.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

  @Mock private StoreRepository storeRepository;
  @Mock private StorePayoutAccountRepository payoutAccountRepository;
  @Mock private OutboxEventWriter outboxEventWriter;

  private StoreService storeService;

  @BeforeEach
  void setUp() {
    storeService = new StoreService(storeRepository, payoutAccountRepository, outboxEventWriter);
  }

  @Test
  void 영업시간_안의_Dish_픽업시간을_허용한다() {
    when(storeRepository.findById(1L))
        .thenReturn(Optional.of(createStore(LocalTime.of(9, 0), LocalTime.of(22, 0))));

    assertThatCode(
            () -> storeService.validateDishPickupTime(1L, LocalTime.of(18, 0), LocalTime.of(22, 0)))
        .doesNotThrowAnyException();
  }

  @Test
  void 영업시간_밖의_Dish_픽업시간을_거부한다() {
    when(storeRepository.findById(1L))
        .thenReturn(Optional.of(createStore(LocalTime.of(9, 0), LocalTime.of(22, 0))));

    assertThatThrownBy(
            () -> storeService.validateDishPickupTime(1L, LocalTime.of(21, 0), LocalTime.of(23, 0)))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.DISH_PICKUP_TIME_OUTSIDE_STORE_HOURS);
  }

  @Test
  void 자정을_넘는_영업시간_안의_Dish_픽업시간을_허용한다() {
    when(storeRepository.findById(1L))
        .thenReturn(Optional.of(createStore(LocalTime.of(18, 0), LocalTime.of(2, 0))));

    assertThatCode(
            () -> storeService.validateDishPickupTime(1L, LocalTime.of(23, 0), LocalTime.of(1, 0)))
        .doesNotThrowAnyException();
  }

  private Store createStore(LocalTime openTime, LocalTime closeTime) {
    return new Store(
        1L,
        "테스트 매장",
        "123-45-67890",
        "서울시 강남구",
        "02-1234-5678",
        openTime,
        closeTime,
        BigDecimal.valueOf(37.5),
        BigDecimal.valueOf(127.0),
        Category.KOREAN);
  }

  @Test
  void updates_store_through_locked_lookup() {
    Long storeId = 10L;
    Store store = createStore(LocalTime.now(), LocalTime.now());
    ReflectionTestUtils.setField(store, "id", storeId);

    when(storeRepository.findWithLockById(storeId)).thenReturn(Optional.of(store));

    UpdateStoreCommand command =
        new UpdateStoreCommand(
            "수정된 매장명",
            "서울특별시 강남구 테헤란로 123",
            "02-1234-5678",
            LocalTime.of(9, 0),
            LocalTime.of(21, 0),
            BigDecimal.valueOf(37.5),
            BigDecimal.valueOf(127.0),
            Category.KOREAN,
            List.of(DayOfWeek.MONDAY));

    // when
    StoreResult result = storeService.update(storeId, store.getMemberId(), command);

    // then
    // TODO(#288): 리스너 구현 후 이벤트 발행을 켜면 아래 단언을 되살린다
    /*
    org.mockito.ArgumentCaptor<kr.lastdish.core.store.domain.event.StoreChangedEvent>
        eventArgumentCaptor =
            org.mockito.ArgumentCaptor.forClass(
                kr.lastdish.core.store.domain.event.StoreChangedEvent.class);

    verify(outboxEventWriter).append(eventArgumentCaptor.capture());

    var event = eventArgumentCaptor.getValue();

    assertThat(event.storeId()).isEqualTo(storeId);
    assertThat(event.aggregateVersion()).isEqualTo(1L);
    assertThat(event.payload().storeId()).isEqualTo(storeId);
    */

    assertThat(result.storeId()).isEqualTo(storeId);
  }

  @Test
  void changes_store_status_through_locked_lookup() {
    // given
    Long storeId = 10L;

    Store store = createStore(LocalTime.now(), LocalTime.now());
    ReflectionTestUtils.setField(store, "id", storeId);

    when(storeRepository.findWithLockById(storeId)).thenReturn(Optional.of(store));

    // when
    StoreResult result =
        storeService.changeStatus(storeId, store.getMemberId(), StoreStatus.CLOSED);

    // then
    // TODO(#288): 리스너 구현 후 이벤트 발행을 켜면 아래 단언을 되살린다
    /*
    org.mockito.ArgumentCaptor<kr.lastdish.core.store.domain.event.StoreStatusChangedEvent>
        eventCaptor =
            org.mockito.ArgumentCaptor.forClass(
                kr.lastdish.core.store.domain.event.StoreStatusChangedEvent.class);

    verify(outboxEventWriter).append(eventCaptor.capture());

    var event = eventCaptor.getValue();

    assertThat(event.storeId()).isEqualTo(storeId);
    assertThat(event.aggregateVersion()).isEqualTo(1L);
    assertThat(event.payload().storeId()).isEqualTo(storeId);
    */

    assertThat(result.storeId()).isEqualTo(storeId);
    assertThat(result.status()).isEqualTo(StoreStatus.CLOSED);
  }

  @Test
  void soft_deletes_store_and_removes_payout_account() {
    // given
    Long storeId = 10L;

    Store store = createStore(LocalTime.now(), LocalTime.now());
    ReflectionTestUtils.setField(store, "id", storeId);

    when(storeRepository.findWithLockById(storeId)).thenReturn(Optional.of(store));

    // when
    storeService.deleteStore(storeId, store.getMemberId());

    // then
    assertThat(store.isDeleted()).isTrue();

    verify(payoutAccountRepository).deleteByStoreId(storeId);
    // TODO(#288): 리스너 구현 후 이벤트 발행을 켜면 아래 단언을 되살린다
    /*
    org.mockito.ArgumentCaptor<kr.lastdish.core.store.domain.event.StoreDeletedEvent> eventCaptor =
        org.mockito.ArgumentCaptor.forClass(
            kr.lastdish.core.store.domain.event.StoreDeletedEvent.class);

    verify(outboxEventWriter).append(eventCaptor.capture());

    var event = eventCaptor.getValue();

    assertThat(event.storeId()).isEqualTo(storeId);
    assertThat(event.aggregateVersion()).isEqualTo(1L);
    assertThat(event.payload().storeId()).isEqualTo(storeId);
    */
  }

  @Test
  void returns_store_when_store_is_not_deleted() {
    // given
    Long storeId = 10L;

    Store store = createStore(LocalTime.now(), LocalTime.now());
    ReflectionTestUtils.setField(store, "id", storeId);

    assertThat(store.isDeleted()).isFalse();

    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));

    // when
    StoreResult result = storeService.getStore(storeId);

    // then
    assertThat(result.storeId()).isEqualTo(storeId);

    verify(storeRepository).findById(storeId);
  }

  @Test
  void throws_exception_when_store_not_found() {
    // given
    Long storeId = 10L;

    when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> storeService.getStore(storeId))
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception -> {
              assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.ENTITY_NOT_FOUND);
              assertThat(exception).hasMessage("매장을 찾을 수 없습니다.");
            });

    verify(storeRepository).findById(storeId);
  }
}
