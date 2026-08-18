package kr.lastdish.core.dish.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import kr.lastdish.core.dish.domain.Dish;
import kr.lastdish.core.dish.domain.DishRepository;
import kr.lastdish.core.dish.presentation.dto.DishCreateRequest;
import kr.lastdish.core.dish.presentation.dto.DishResponse;
import kr.lastdish.core.dish.presentation.dto.DishUpdateRequest;
import kr.lastdish.core.store.application.StoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DishFacadeTest {

  @Mock private DishRepository dishRepository;
  @Mock private DishService dishService;
  @Mock private StoreService storeService;
  @Mock private DishImageService dishImageService;

  private DishFacade dishFacade;

  @BeforeEach
  void setUp() {
    dishFacade = new DishFacade(dishRepository, dishService, storeService, dishImageService);
  }

  @Test
  void Dish_등록시_매장_영업시간을_검증한_뒤_등록을_위임한다() {
    DishCreateRequest request = createRequest();
    DishResponse expected = org.mockito.Mockito.mock(DishResponse.class);
    when(dishImageService.confirmUpload(7L, request.storeId(), request.imageKey()))
        .thenReturn("dish/1/test.jpg");
    when(dishService.createDish(request, "dish/1/test.jpg")).thenReturn(expected);

    DishResponse result = dishFacade.createDish(7L, request);

    verify(storeService).validateSeller(request.storeId(), 7L);
    verify(storeService)
        .validateDishPickupTime(
            request.storeId(), request.pickupStartTime(), request.pickupEndTime());
    verify(dishImageService).confirmUpload(7L, request.storeId(), request.imageKey());
    verify(dishService).createDish(request, "dish/1/test.jpg");
    assertThat(result).isSameAs(expected);
  }

  @Test
  void Dish_수정시_실제_Dish의_매장_영업시간을_검증한_뒤_수정을_위임한다() {
    Dish dish = createDish();
    DishUpdateRequest request = updateRequest();
    DishResponse expected = org.mockito.Mockito.mock(DishResponse.class);
    when(dishRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(dish);
    when(dishService.updateDish(10L, request)).thenReturn(expected);

    DishResponse result = dishFacade.updateDish(10L, request);

    verify(storeService)
        .validateDishPickupTime(
            dish.getStoreId(), request.pickupStartTime(), request.pickupEndTime());
    verify(dishService).updateDish(10L, request);
    assertThat(result).isSameAs(expected);
  }

  private DishCreateRequest createRequest() {
    return new DishCreateRequest(
        1L,
        "김치찌개",
        LocalDateTime.now(),
        "상품 설명",
        "한식",
        "tmp/dish/1/test.jpg",
        10L,
        BigDecimal.valueOf(10_000),
        BigDecimal.valueOf(7_000),
        LocalTime.of(18, 0),
        LocalTime.of(19, 0));
  }

  private DishUpdateRequest updateRequest() {
    return new DishUpdateRequest(
        10L,
        "김치찌개",
        LocalDateTime.now(),
        "상품 설명",
        null,
        10L,
        BigDecimal.valueOf(10_000),
        BigDecimal.valueOf(7_000),
        LocalTime.of(18, 0),
        LocalTime.of(19, 0));
  }

  private Dish createDish() {
    return Dish.create(
        1L,
        "김치찌개",
        LocalDateTime.now(),
        "상품 설명",
        "한식",
        null,
        10L,
        BigDecimal.valueOf(10_000),
        BigDecimal.valueOf(7_000),
        LocalTime.of(18, 0),
        LocalTime.of(19, 0));
  }
}
