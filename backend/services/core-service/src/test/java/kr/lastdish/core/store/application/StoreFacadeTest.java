package kr.lastdish.core.store.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import kr.lastdish.core.dish.application.DishService;
import kr.lastdish.core.dish.application.dto.InternalDishResult;
import kr.lastdish.core.dish.domain.Dish;
import kr.lastdish.core.store.application.dto.InternalStoreResult;
import kr.lastdish.core.store.application.dto.StoreResult;
import kr.lastdish.core.store.domain.Category;
import kr.lastdish.core.store.domain.Store;
import kr.lastdish.core.store.domain.StoreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StoreFacadeTest {
  @Mock private StoreService storeService;

  @Mock private DishService dishService;

  @Mock private StoreRepository storeRepository;

  @InjectMocks private StoreFacade storeFacade;

  @Test
  void reschedules_next_closing_at_through_locked_lookup() {
    Long storeId = 10L;
    LocalDateTime now = LocalDateTime.of(2026, 8, 20, 22, 0);
    Store store = createStore(LocalTime.of(9, 0), LocalTime.of(22, 0));

    when(storeRepository.findWithLockById(storeId)).thenReturn(Optional.of(store));

    storeFacade.rescheduleNextClosingAt(storeId, now);

    verify(storeRepository).findWithLockById(storeId);
  }

  @Test
  void returns_store_and_null_dish_when_store_exists_without_dish() {
    // given
    Long storeId = 10L;

    StoreResult storeResult = createStoreResult(storeId);

    when(storeService.getStore(storeId)).thenReturn(storeResult);
    when(dishService.getDishByStoreIdForRenewal(storeId)).thenReturn(Optional.empty());

    // when
    InternalStoreResult response = storeFacade.getDishAndStoreByStoreIdForRenewal(storeId);

    // then
    assertThat(response).isNotNull();
    assertThat(response.storeId()).isEqualTo(storeId);
    assertThat(response.dish()).isNull();

    verify(storeService).getStore(storeId);
    verify(dishService).getDishByStoreIdForRenewal(storeId);
  }

  @Test
  void returns_store_and_dish_when_both_exist() {
    // given
    Long storeId = 10L;
    Long stockQuantity = 100L;

    StoreResult storeResult = createStoreResult(storeId);
    InternalDishResult dishResult = createDishResult(stockQuantity);

    when(storeService.getStore(storeId)).thenReturn(storeResult);
    when(dishService.getDishByStoreIdForRenewal(storeId)).thenReturn(Optional.of(dishResult));

    // when
    InternalStoreResult response = storeFacade.getDishAndStoreByStoreIdForRenewal(storeId);

    // then
    assertThat(response).isNotNull();

    assertThat(response.storeId()).isEqualTo(storeId);

    assertThat(response.dish()).isNotNull();
    assertThat(response.dish().dishId()).isEqualTo(dishResult.dishId());
    assertThat(response.dish().storeId()).isEqualTo(storeId);

    verify(storeService).getStore(storeId);
    verify(dishService).getDishByStoreIdForRenewal(storeId);
  }

  private StoreResult createStoreResult(Long storeId) {
    Store store = createStore(LocalTime.now(), LocalTime.now());
    ReflectionTestUtils.setField(store, "id", storeId);

    return StoreResult.from(store);
  }

  private InternalDishResult createDishResult(Long stockQuantity) {
    Dish dish = createDish(stockQuantity);
    ReflectionTestUtils.setField(dish, "id", 10L);

    return InternalDishResult.from(dish);
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

  private Dish createDish(Long stockQuantity) {
    return Dish.create(
        10L,
        "김치찌개",
        LocalDateTime.now(),
        "상품 설명",
        "한식",
        null,
        stockQuantity,
        BigDecimal.valueOf(10000),
        BigDecimal.ZERO,
        LocalTime.of(18, 0),
        LocalTime.of(19, 0));
  }
}
