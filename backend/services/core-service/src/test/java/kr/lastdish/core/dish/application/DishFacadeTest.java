package kr.lastdish.core.dish.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.core.common.exception.ErrorCode;
import kr.lastdish.core.dish.domain.Dish;
import kr.lastdish.core.dish.domain.DishRepository;
import kr.lastdish.core.dish.domain.DishStatus;
import kr.lastdish.core.dish.presentation.dto.DishCreateRequest;
import kr.lastdish.core.dish.presentation.dto.DishResponse;
import kr.lastdish.core.dish.presentation.dto.DishStatusRequest;
import kr.lastdish.core.dish.presentation.dto.DishUpdateRequest;
import kr.lastdish.core.order.application.OrderService;
import kr.lastdish.core.store.application.StoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DishFacadeTest {

  @Mock private DishRepository dishRepository;
  @Mock private DishService dishService;
  @Mock private StoreService storeService;
  @Mock private DishImageService dishImageService;
  @Mock private OrderService orderService;

  private DishFacade dishFacade;

  @BeforeEach
  void setUp() {
    dishFacade =
        new DishFacade(dishRepository, dishService, storeService, dishImageService, orderService);
  }

  @Test
  void Dish_생성시_검증_이미지_확정_DB_저장_순서로_실행한다() {
    DishCreateRequest request = createRequest();
    DishResponse expected = org.mockito.Mockito.mock(DishResponse.class);
    when(dishImageService.confirmUpload(7L, 1L, request.imageKey())).thenReturn("dish/1/test.jpg");
    when(dishService.createDish(request, "dish/1/test.jpg")).thenReturn(expected);

    DishResponse result = dishFacade.createDish(7L, request);

    InOrder inOrder = inOrder(storeService, dishService, dishImageService);
    inOrder.verify(storeService).validateSeller(1L, 7L);
    inOrder
        .verify(storeService)
        .validateDishPickupTime(1L, request.pickupStartTime(), request.pickupEndTime());
    inOrder.verify(dishService).validateCreateDish(request);
    inOrder.verify(dishImageService).confirmUpload(7L, 1L, request.imageKey());
    inOrder.verify(dishService).createDish(request, "dish/1/test.jpg");
    assertThat(result).isSameAs(expected);
  }

  @Test
  void 이미지가_없으면_S3_확인없이_Dish를_생성한다() {
    DishCreateRequest request = createRequest(null);
    DishResponse expected = org.mockito.Mockito.mock(DishResponse.class);
    when(dishService.createDish(request, null)).thenReturn(expected);

    DishResponse result = dishFacade.createDish(7L, request);

    InOrder inOrder = inOrder(storeService, dishService);
    inOrder.verify(storeService).validateSeller(1L, 7L);
    inOrder
        .verify(storeService)
        .validateDishPickupTime(1L, request.pickupStartTime(), request.pickupEndTime());
    inOrder.verify(dishService).validateCreateDish(request);
    inOrder.verify(dishService).createDish(request, null);
    verifyNoInteractions(dishImageService);
    assertThat(result).isSameAs(expected);
  }

  @Test
  void 판매중인_Dish는_주문할_수_있다() {
    Dish dish = createDish();
    when(dishRepository.findAvailableById(10L)).thenReturn(Optional.of(dish));

    dishFacade.validateAvailable(10L);

    verify(dishRepository).findAvailableById(10L);
  }

  @Test
  void 삭제됐거나_판매중이_아닌_Dish는_주문할_수_없다() {
    Dish soldOutDish = createDish();
    soldOutDish.updateStatus(DishStatus.SOLD_OUT);
    when(dishRepository.findAvailableById(10L)).thenReturn(Optional.of(soldOutDish));
    when(dishRepository.findAvailableById(20L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> dishFacade.validateAvailable(10L))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.DISH_NOT_ON_SALE);
    assertThatThrownBy(() -> dishFacade.validateAvailable(20L))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.DISH_NOT_ON_SALE);
  }

  @Test
  void Dish_DB_저장이_실패하면_확정한_이미지를_보상_삭제한다() {
    DishCreateRequest request = createRequest();
    RuntimeException databaseException = new RuntimeException("DB 저장 실패");
    when(dishImageService.confirmUpload(7L, 1L, request.imageKey())).thenReturn("dish/1/test.jpg");
    when(dishService.createDish(request, "dish/1/test.jpg")).thenThrow(databaseException);

    assertThatThrownBy(() -> dishFacade.createDish(7L, request)).isSameAs(databaseException);

    verify(dishImageService).deleteImageSafely("dish/1/test.jpg");
  }

  @Test
  void 이미지_확정_DB_커밋이_실패하면_최종_이미지를_보상_삭제한다() {
    DishCreateRequest request = createRequest();
    RuntimeException commitException = new RuntimeException("업로드 확정 DB 커밋 실패");
    when(dishImageService.confirmUpload(7L, 1L, request.imageKey())).thenThrow(commitException);

    assertThatThrownBy(() -> dishFacade.createDish(7L, request)).isSameAs(commitException);

    verify(dishImageService).deleteImageSafely("dish/1/test.jpg");
  }

  @Test
  void 이미지_확정_검증이_실패하면_최종_이미지를_삭제하지_않는다() {
    DishCreateRequest request = createRequest();
    BusinessException validationException =
        new BusinessException(ErrorCode.PRESIGNED_UPLOAD_INVALID_STATE);
    when(dishImageService.confirmUpload(7L, 1L, request.imageKey())).thenThrow(validationException);

    assertThatThrownBy(() -> dishFacade.createDish(7L, request)).isSameAs(validationException);

    verify(dishImageService, never()).deleteImageSafely("dish/1/test.jpg");
  }

  @Test
  void Dish_수정시_소유권과_매장_상태와_진행중_주문을_검증한_뒤_수정을_위임한다() {
    Dish dish = createDish();
    DishUpdateRequest request = updateRequest();
    DishResponse expected = org.mockito.Mockito.mock(DishResponse.class);
    when(dishRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(dish);
    when(dishService.updateDish(10L, request)).thenReturn(expected);

    DishResponse result = dishFacade.updateDish(7L, 10L, request);

    InOrder inOrder = inOrder(storeService, orderService, dishService);
    inOrder
        .verify(storeService)
        .validateDishUpdate(
            dish.getStoreId(), 7L, request.pickupStartTime(), request.pickupEndTime());
    inOrder.verify(orderService).hasActiveOrdersForDish(10L);
    inOrder.verify(dishService).updateDish(10L, request);
    assertThat(result).isSameAs(expected);
  }

  @Test
  void 진행중인_주문이_있으면_Dish를_수정할_수_없다() {
    Dish dish = createDish();
    DishUpdateRequest request = updateRequest();
    when(dishRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(dish);
    when(orderService.hasActiveOrdersForDish(10L)).thenReturn(true);

    assertThatThrownBy(() -> dishFacade.updateDish(7L, 10L, request))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.DISH_HAS_ACTIVE_ORDERS);

    verify(dishService, never()).updateDish(10L, request);
  }

  @Test
  void Dish_상태_변경시_소유권을_검증한_뒤_변경을_위임한다() {
    Dish dish = createDish();
    DishStatusRequest request = org.mockito.Mockito.mock(DishStatusRequest.class);
    DishResponse expected = org.mockito.Mockito.mock(DishResponse.class);
    when(dishRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(dish);
    when(dishService.updateDishStatus(10L, request)).thenReturn(expected);

    DishResponse result = dishFacade.updateDishStatus(7L, 10L, request);

    InOrder inOrder = inOrder(storeService, dishService);
    inOrder.verify(storeService).validateSeller(dish.getStoreId(), 7L);
    inOrder.verify(dishService).updateDishStatus(10L, request);
    assertThat(result).isSameAs(expected);
  }

  @Test
  void Dish_재고_조정시_소유권을_검증한_뒤_조정을_위임한다() {
    Dish dish = createDish();
    DishResponse expected = org.mockito.Mockito.mock(DishResponse.class);
    when(dishRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(dish);
    when(dishService.adjustStock(10L, 5L)).thenReturn(expected);

    DishResponse result = dishFacade.adjustStock(7L, 10L, 5L);

    InOrder inOrder = inOrder(storeService, dishService);
    inOrder.verify(storeService).validateSeller(dish.getStoreId(), 7L);
    inOrder.verify(dishService).adjustStock(10L, 5L);
    assertThat(result).isSameAs(expected);
  }

  @Test
  void Dish_삭제시_소유권_검증_DB_삭제_S3_정리_순서로_실행한다() {
    Dish dish = createDish();
    when(dishRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(dish);
    when(dishService.deleteDish(10L)).thenReturn("dish/1/test.jpg");

    dishFacade.deleteDish(7L, 10L);

    InOrder inOrder = inOrder(storeService, dishService, dishImageService);
    inOrder.verify(storeService).validateSeller(dish.getStoreId(), 7L);
    inOrder.verify(dishService).deleteDish(10L);
    inOrder.verify(dishImageService).deleteImageSafely("dish/1/test.jpg");
  }

  private DishCreateRequest createRequest() {
    return createRequest("tmp/dish/1/test.jpg");
  }

  private DishCreateRequest createRequest(String imageKey) {
    return new DishCreateRequest(
        1L,
        "김치찌개",
        LocalDateTime.now(),
        "상품 설명",
        "한식",
        imageKey,
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
