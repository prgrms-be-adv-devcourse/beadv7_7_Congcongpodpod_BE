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
import kr.lastdish.common.event.DomainEvent;
import kr.lastdish.common.outbox.application.OutboxEventWriter;
import kr.lastdish.core.common.exception.ErrorCode;
import kr.lastdish.core.store.application.dto.StoreResult;
import kr.lastdish.core.store.application.dto.UpdateStoreCommand;
import kr.lastdish.core.store.domain.*;
import kr.lastdish.core.store.domain.event.StoreChangedEvent;
import kr.lastdish.core.store.domain.event.StoreDeletedEvent;
import kr.lastdish.core.store.domain.event.StoreStatusChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
  void records_event_when_store_changes() {
    Long storeId = 10L;
    Store store = createStore(LocalTime.now(), LocalTime.now());
    ReflectionTestUtils.setField(store, "id", storeId);

    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));

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

    ArgumentCaptor<DomainEvent> eventArgumentCaptor = ArgumentCaptor.forClass(DomainEvent.class);

    // when
    StoreResult result = storeService.update(storeId, store.getMemberId(), command);

    // then
    verify(outboxEventWriter).append(eventArgumentCaptor.capture());

    StoreChangedEvent event = (StoreChangedEvent) eventArgumentCaptor.getValue();

    assertThat(event.storeId()).isEqualTo(storeId);
    assertThat(event.aggregateVersion()).isEqualTo(1L);

    assertThat(event.payload().storeName()).isEqualTo("수정된 매장명");
    assertThat(event.payload().storeAddress()).isEqualTo("서울특별시 강남구 테헤란로 123");
    assertThat(event.payload().storePhone()).isEqualTo("02-1234-5678");
    assertThat(event.payload().latitude()).isEqualTo(BigDecimal.valueOf(37.5));
    assertThat(event.payload().longitude()).isEqualTo(BigDecimal.valueOf(127.0));
    assertThat(event.payload().category()).isEqualTo(Category.KOREAN);

    assertThat(result.storeId()).isEqualTo(storeId);
  }

  @Test
  void records_event_when_store_status_changes() {
    // given
    Long storeId = 10L;

    Store store = createStore(LocalTime.now(), LocalTime.now());
    ReflectionTestUtils.setField(store, "id", storeId);

    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));

    ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);

    // when
    StoreResult result =
        storeService.changeStatus(storeId, store.getMemberId(), StoreStatus.CLOSED);

    // then
    verify(outboxEventWriter).append(eventCaptor.capture());

    StoreStatusChangedEvent event = (StoreStatusChangedEvent) eventCaptor.getValue();

    assertThat(event.storeId()).isEqualTo(storeId);
    assertThat(event.aggregateVersion()).isEqualTo(1L);
    assertThat(event.payload().status()).isEqualTo(StoreStatus.CLOSED);

    assertThat(result.storeId()).isEqualTo(storeId);
    assertThat(result.status()).isEqualTo(StoreStatus.CLOSED);
  }

  @Test
  void records_event_when_store_is_deleted() {
    // given
    Long storeId = 10L;

    Store store = createStore(LocalTime.now(), LocalTime.now());
    ReflectionTestUtils.setField(store, "id", storeId);

    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));

    ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);

    // when
    storeService.deleteStore(storeId, store.getMemberId());

    // then
    assertThat(store.isDeleted()).isTrue();

    verify(payoutAccountRepository).deleteByStoreId(storeId);
    verify(outboxEventWriter).append(eventCaptor.capture());

    StoreDeletedEvent event = (StoreDeletedEvent) eventCaptor.getValue();

    assertThat(event.storeId()).isEqualTo(storeId);
    assertThat(event.aggregateVersion()).isEqualTo(1L);
  }
}
