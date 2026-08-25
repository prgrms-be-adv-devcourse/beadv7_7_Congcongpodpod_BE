package kr.lastdish.core.dish.application;

import java.util.Optional;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.core.dish.application.dto.DishSnapshot;
import kr.lastdish.core.dish.domain.Dish;
import kr.lastdish.core.dish.domain.DishRepository;
import kr.lastdish.core.dish.domain.DishStatus;
import kr.lastdish.core.dish.presentation.dto.DishCreateRequest;
import kr.lastdish.core.dish.presentation.dto.DishResponse;
import kr.lastdish.core.dish.presentation.dto.DishStatusRequest;
import kr.lastdish.core.dish.presentation.dto.DishUpdateRequest;
import kr.lastdish.core.store.application.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dish 유스케이스의 도메인 검증과 DB·S3 작업 순서를 조율하는 Facade입니다.
 *
 * <p>이미지 확정 후 Dish 저장이 실패하면 최종 이미지를 보상 삭제하며, Dish 삭제 후에는 연결된 이미지를 정리합니다.
 */
@Service
@RequiredArgsConstructor
public class DishFacade {

  private final DishRepository dishRepository;
  private final DishService dishService;
  private final StoreService storeService;
  private final DishImageService dishImageService;

  public DishResponse createDish(Long memberId, DishCreateRequest request) {
    storeService.validateSeller(request.storeId(), memberId);
    storeService.validateDishPickupTime(
        request.storeId(), request.pickupStartTime(), request.pickupEndTime());
    dishService.validateCreateDish(request);

    if (request.imageKey() == null) {
      return dishService.createDish(request, null);
    }

    String finalImageKey = DishImageService.resolveFinalKey(request.storeId(), request.imageKey());
    boolean uploadConfirmed = false;

    try {
      // T1: 임시 이미지를 최종 경로로 확정합니다.
      dishImageService.confirmUpload(memberId, request.storeId(), request.imageKey());
      uploadConfirmed = true;

      // T2: Dish를 DB에 저장합니다. DishService의 트랜잭션은 메서드 반환 전에 커밋됩니다.
      return dishService.createDish(request, finalImageKey);
    } catch (BusinessException exception) {
      if (uploadConfirmed) {
        dishImageService.deleteImageSafely(finalImageKey);
      }
      throw exception;
    } catch (RuntimeException exception) {
      // C1: 이미지 확정 DB 커밋 또는 Dish 저장 실패 시 최종 이미지를 보상 삭제합니다.
      dishImageService.deleteImageSafely(finalImageKey);
      throw exception;
    }
  }

  @Transactional
  public DishResponse updateDish(Long memberId, Long dishId, DishUpdateRequest request) {
    Dish dish = dishRepository.findByIdAndIsDeletedFalse(dishId);
    storeService.validateSeller(dish.getStoreId(), memberId);
    storeService.validateDishPickupTime(
        dish.getStoreId(), request.pickupStartTime(), request.pickupEndTime());
    return dishService.updateDish(dishId, request);
  }

  public DishResponse updateDishStatus(Long memberId, Long dishId, DishStatusRequest request) {
    Dish dish = dishRepository.findByIdAndIsDeletedFalse(dishId);
    storeService.validateSeller(dish.getStoreId(), memberId);
    return dishService.updateDishStatus(dishId, request);
  }

  public DishResponse adjustStock(Long memberId, Long dishId, Long quantityDelta) {
    Dish dish = dishRepository.findByIdAndIsDeletedFalse(dishId);
    storeService.validateSeller(dish.getStoreId(), memberId);
    return dishService.adjustStock(dishId, quantityDelta);
  }

  public void deleteDish(Long memberId, Long dishId) {
    Dish dish = dishRepository.findByIdAndIsDeletedFalse(dishId);
    storeService.validateSeller(dish.getStoreId(), memberId);

    // T1: DB에서 Dish를 소프트 삭제합니다.
    String imageKey = dishService.deleteDish(dishId);

    // T2: S3 이미지를 정리합니다. 실패 여부가 불명확한 S3 삭제는 DB 복구로 보상하지 않습니다.
    dishImageService.deleteImageSafely(imageKey);
  }

  /**
   * 조회 가능한(판매중) Dish의 스냅샷을 가져온다.
   *
   * <p>Optional을 반환하는 이유: 이 메서드는 "판매 가능한 Dish가 존재하는가"라는 사실만 확인한다. "없을 때 어떤 에러(404/409 등)로 처리할지"는 이
   * 데이터를 실제로 쓰는 호출자(예: CartService)가 자기 맥락에 맞게 결정할 문제이므로, 이 메서드에서 예외를 던지거나 null을 반환해 그 판단을 대신 내리지
   * 않는다.
   *
   * <p>호출자는 반드시 .orElseThrow(...) 등으로 "없음"의 의미를 직접 정해야 한다.
   */
  public Optional<DishSnapshot> findDishSnapshot(Long dishId) {
    return dishRepository
        .findAvailableById(dishId)
        .filter(dish -> dish.getDishStatus() == DishStatus.ON_SALE)
        .map(DishFacade::toSnapshot);
  }

  // 마감할인 서비스 특성상 스냅샷 단가는 discountPrice로 잡는다.
  private static DishSnapshot toSnapshot(Dish dish) {

    return new DishSnapshot(
        dish.getId(),
        dish.getStoreId(),
        dish.getDishName(),
        dish.getDiscountPrice(),
        dish.getDishPrice(),
        dish.getStockQuantity(),
        dish.getPickupStartTime(),
        dish.getPickupEndTime(),
        dish.getAggregateVersion());
  }

  public void decreaseStock(Long dishId, Long quantity) {
    dishService.decreaseStock(dishId, quantity);
  }

  public void increaseStock(Long dishId, Long quantity) {
    dishService.increaseStock(dishId, quantity);
  }
}
