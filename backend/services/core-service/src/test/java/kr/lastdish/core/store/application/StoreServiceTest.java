package kr.lastdish.core.store.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Optional;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.core.common.exception.ErrorCode;
import kr.lastdish.core.store.domain.Category;
import kr.lastdish.core.store.domain.Store;
import kr.lastdish.core.store.domain.StorePayoutAccountRepository;
import kr.lastdish.core.store.domain.StoreRepository;
import kr.lastdish.core.store.domain.StoreStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

  @Mock private StoreRepository storeRepository;
  @Mock private StorePayoutAccountRepository payoutAccountRepository;

  private StoreService storeService;

  @BeforeEach
  void setUp() {
    storeService = new StoreService(storeRepository, payoutAccountRepository);
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
  void 삭제됐거나_존재하지_않는_매장도_주문할_수_없다() {
    when(storeRepository.findById(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> storeService.validateOpen(1L))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ORDER_STORE_CLOSED);
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
}
