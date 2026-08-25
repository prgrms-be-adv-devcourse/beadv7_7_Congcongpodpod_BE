package kr.lastdish.core.store.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.api.exception.CommonErrorCode;
import kr.lastdish.common.event.DomainEvent;
import kr.lastdish.common.outbox.application.OutboxEventWriter;
import kr.lastdish.core.common.exception.ErrorCode;
import kr.lastdish.core.store.application.dto.RegisterStoreCommand;
import kr.lastdish.core.store.application.dto.StoreResult;
import kr.lastdish.core.store.application.dto.UpdateStoreCommand;
import kr.lastdish.core.store.domain.Category;
import kr.lastdish.core.store.domain.Store;
import kr.lastdish.core.store.domain.StorePayoutAccountRepository;
import kr.lastdish.core.store.domain.StoreRepository;
import kr.lastdish.core.store.domain.StoreStatus;
import kr.lastdish.core.store.domain.event.StoreChangedEvent;
import kr.lastdish.core.store.domain.event.StoreDeletedEvent;
import kr.lastdish.core.store.domain.event.StoreRegisteredEvent;
import kr.lastdish.core.store.domain.event.StoreStatusChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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

  @Test
  void OPEN_상태인_매장은_주문할_수_있다() {
    Store store = createStore(LocalTime.of(9, 0), LocalTime.of(22, 0));
    when(storeRepository.findById(1L)).thenReturn(Optional.of(store));

    assertThatCode(() -> storeService.validateOpen(1L)).doesNotThrowAnyException();
  }

  @Test
  void Dish_수정_조건은_Store를_한번_조회해_검증한다() {
    Store store = createStore(LocalTime.of(9, 0), LocalTime.of(22, 0));
    store.changeStatus(StoreStatus.CLOSED);
    when(storeRepository.findById(1L)).thenReturn(Optional.of(store));

    assertThatCode(
            () -> storeService.validateDishUpdate(1L, 1L, LocalTime.of(18, 0), LocalTime.of(19, 0)))
        .doesNotThrowAnyException();

    verify(storeRepository, times(1)).findById(1L);
  }

  @ParameterizedTest
  @EnumSource(
      value = StoreStatus.class,
      names = {"OPEN", "STOPPED"})
  void CLOSED가_아닌_매장은_Dish를_수정할_수_없다(StoreStatus status) {
    Store store = createStore(LocalTime.of(9, 0), LocalTime.of(22, 0));
    store.changeStatus(status);
    when(storeRepository.findById(1L)).thenReturn(Optional.of(store));

    assertThatThrownBy(
            () -> storeService.validateDishUpdate(1L, 1L, LocalTime.of(18, 0), LocalTime.of(19, 0)))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.STORE_MUST_BE_CLOSED_FOR_DISH_UPDATE);
  }

  @Test
  void 영업_상태가_아닌_매장은_주문할_수_없다() {
    Store store = createStore(LocalTime.of(9, 0), LocalTime.of(22, 0));
    store.changeStatus(StoreStatus.CLOSED);
    when(storeRepository.findById(1L)).thenReturn(Optional.of(store));

    assertThatThrownBy(() -> storeService.validateOpen(1L))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ORDER_STORE_CLOSED);
  }

  @Test
  void 삭제됐거나_존재하지_않는_매장은_ENTITY_NOT_FOUND를_던진다() {
    when(storeRepository.findById(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> storeService.validateOpen(1L))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(CommonErrorCode.ENTITY_NOT_FOUND);
  }

  private Store createStore(LocalTime openTime, LocalTime closeTime) {
    return new Store(
        1L,
        "테스트 매장",
        "123-45-67890",
        "서울시 강남구",
        "명정빌딩",
        "02-1234-5678",
        openTime,
        closeTime,
        BigDecimal.valueOf(37.5),
        BigDecimal.valueOf(127.0),
        Category.KOREAN,
        LocalDateTime.of(2026, 8, 10, 12, 0));
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
            "명정빌딩",
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
    ArgumentCaptor<StoreChangedEvent> eventArgumentCaptor =
        ArgumentCaptor.forClass(StoreChangedEvent.class);

    verify(outboxEventWriter).append(eventArgumentCaptor.capture());

    var event = eventArgumentCaptor.getValue();

    assertThat(event.storeId()).isEqualTo(storeId);
    assertThat(event.aggregateVersion()).isEqualTo(1L);
    assertThat(event.payload().storeId()).isEqualTo(storeId);

    assertThat(result.storeId()).isEqualTo(storeId);
  }

  @Test
  void changes_store_status_through_locked_lookup() {
    // given
    Long storeId = 10L;

    Store store = createStore(LocalTime.now(), LocalTime.now());
    ReflectionTestUtils.setField(store, "id", storeId);

    // when
    StoreResult result = storeService.changeStatus(store, StoreStatus.CLOSED);

    // then
    ArgumentCaptor<StoreStatusChangedEvent> eventCaptor =
        ArgumentCaptor.forClass(StoreStatusChangedEvent.class);

    verify(outboxEventWriter).append(eventCaptor.capture());

    var event = eventCaptor.getValue();

    assertThat(event.storeId()).isEqualTo(storeId);
    assertThat(event.aggregateVersion()).isEqualTo(1L);
    assertThat(event.payload().storeId()).isEqualTo(storeId);

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

    ArgumentCaptor<StoreDeletedEvent> eventCaptor =
        ArgumentCaptor.forClass(StoreDeletedEvent.class);

    verify(outboxEventWriter).append(eventCaptor.capture());

    var event = eventCaptor.getValue();

    assertThat(event.storeId()).isEqualTo(storeId);
    assertThat(event.aggregateVersion()).isEqualTo(1L);
    assertThat(event.payload().storeId()).isEqualTo(storeId);
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

  @Test
  void registers_store_and_records_registered_event() {
    // given
    Long storeId = 10L;
    Long memberId = 1L;

    RegisterStoreCommand command =
        new RegisterStoreCommand(
            memberId,
            "테스트 매장",
            "123-45-67890",
            "서울특별시 강남구 테헤란로 123",
            "명정빌딩",
            "02-1234-5678",
            LocalTime.of(9, 0),
            LocalTime.of(21, 0),
            new BigDecimal("37.501"),
            new BigDecimal("127.039"),
            Category.KOREAN,
            List.of(DayOfWeek.MONDAY));

    when(storeRepository.existsByMemberId(memberId)).thenReturn(false);
    when(storeRepository.existsByBusinessNumber(command.businessNumber())).thenReturn(false);

    when(storeRepository.save(any(Store.class)))
        .thenAnswer(
            invocation -> {
              Store store = invocation.getArgument(0);
              ReflectionTestUtils.setField(store, "id", storeId);
              return store;
            });

    ArgumentCaptor<Store> storeCaptor = ArgumentCaptor.forClass(Store.class);

    ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);

    // when
    StoreResult result = storeService.register(command);

    // then
    verify(storeRepository).existsByMemberId(memberId);
    verify(storeRepository).existsByBusinessNumber(command.businessNumber());
    verify(storeRepository).save(storeCaptor.capture());

    Store savedStore = storeCaptor.getValue();

    assertThat(savedStore.getMemberId()).isEqualTo(memberId);
    assertThat(savedStore.getBusinessNumber()).isEqualTo(command.businessNumber());
    assertThat(savedStore.getHolidays()).hasSize(command.holidays().size());
    assertThat(savedStore.getNextClosingAt()).isNotNull();

    // 회원 권한을 SELLER로 변경하기 위한 이벤트 검증
    verify(outboxEventWriter, times(2)).append(eventCaptor.capture());

    List<DomainEvent> capturedEvents = eventCaptor.getAllValues();
    StoreRegisteredEvent event =
        capturedEvents.stream()
            .filter(e -> e instanceof StoreRegisteredEvent)
            .map(StoreRegisteredEvent.class::cast)
            .findFirst()
            .orElseThrow();

    assertThat(event.storeId()).isEqualTo(storeId);
    assertThat(event.aggregateVersion()).isEqualTo(1L);
    assertThat(event.payload().memberId()).isEqualTo(memberId);
  }
}
