package kr.lastdish.core.store.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.api.exception.CommonErrorCode;
import kr.lastdish.core.dish.application.DishService;
import kr.lastdish.core.dish.application.dto.InternalDishResult;
import kr.lastdish.core.dish.presentation.dto.DishResponse;
import kr.lastdish.core.settlement.application.dto.StoreSettlementAccountResult;
import kr.lastdish.core.store.application.dto.InternalStoreResult;
import kr.lastdish.core.store.application.dto.NearbyStoreResult;
import kr.lastdish.core.store.application.dto.StorePageResult;
import kr.lastdish.core.store.application.dto.StoreResult;
import kr.lastdish.core.store.domain.Category;
import kr.lastdish.core.store.domain.Store;
import kr.lastdish.core.store.domain.StorePayoutAccountRepository;
import kr.lastdish.core.store.domain.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreFacade {
  private final StoreService storeService;
  private final DishService dishService;
  private final StorePayoutAccountRepository storePayoutAccountRepository;
  private final StoreRepository storeRepository;

  public void validateStoreOwner(Long storeId, Long memberId) {
    storeService.validateSeller(storeId, memberId);
  }

  public void validateOpen(Long storeId) {
    storeService.validateOpen(storeId);
  }

  public List<Long> findSettlementTargetStoreIds() {
    return storeService.findSettlementTargetStoreIds();
  }

  public Optional<StoreSettlementAccountResult> findSettlementAccount(Long storeId) {
    return storePayoutAccountRepository
        .findByStoreId(storeId)
        .map(
            account ->
                new StoreSettlementAccountResult(
                    account.getBankName(), account.getAccountNumber(), account.getAccountHolder()));
  }

  public Long findStoreIdByMemberId(Long memberId) {
    return storeRepository
        .findByMemberIdForSettlement(memberId)
        .map(Store::getId)
        .orElseThrow(
            () ->
                new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "회원이 소유한 매장을 찾을 수 없습니다."));
  }

  public DishResponse getMyDish(Long storeId, Long memberId) {
    storeService.getOwnedStore(storeId, memberId);
    return dishService.getDishByStoreId(storeId);
  }

  public List<DishResponse> getMyDishes(Long storeId, Long memberId) {
    storeService.getOwnedStore(storeId, memberId);
    return dishService.getDishesByStoreId(storeId);
  }

  // 검색 색인 재생성용 조회 — 매장 정보와 상품 정보를 합쳐 반환한다.
  public InternalStoreResult getDishAndStoreByStoreIdForRenewal(Long storeId) {
    StoreResult store = storeService.getStore(storeId);
    InternalDishResult dish = dishService.getDishByStoreIdForRenewal(storeId).orElse(null);

    return InternalStoreResult.from(store, dish);
  }

  public StorePageResult getNearbyStores(
      BigDecimal latitude,
      BigDecimal longitude,
      Category category,
      double radiusKm,
      int page,
      int size) {
    if (radiusKm <= 0) {
      throw new BusinessException(CommonErrorCode.INVALID_INPUT, "검색 반경은 0보다 커야 합니다.");
    }
    if (page < 0) {
      throw new BusinessException(CommonErrorCode.INVALID_INPUT, "페이지 번호는 0 이상이어야 합니다.");
    }
    if (size <= 0) {
      throw new BusinessException(CommonErrorCode.INVALID_INPUT, "페이지 크기는 0보다 커야 합니다.");
    }

    double latitudeDelta = radiusKm / 111.0;
    double longitudeDivisor = 111.0 * Math.cos(Math.toRadians(latitude.doubleValue()));
    if (Math.abs(longitudeDivisor) < 0.01) {
      longitudeDivisor = 0.01;
    }
    double longitudeDelta = radiusKm / longitudeDivisor;

    BigDecimal minLatitude = latitude.subtract(BigDecimal.valueOf(latitudeDelta));
    BigDecimal maxLatitude = latitude.add(BigDecimal.valueOf(latitudeDelta));
    BigDecimal minLongitude = longitude.subtract(BigDecimal.valueOf(longitudeDelta));
    BigDecimal maxLongitude = longitude.add(BigDecimal.valueOf(longitudeDelta));

    List<Store> stores =
        storeRepository.findOpenStoresByLocationRange(
            minLatitude, maxLatitude, minLongitude, maxLongitude, category, page, size);
    List<NearbyStoreResult> results =
        stores.stream()
            .map(
                store ->
                    new NearbyStoreResult(
                        StoreResult.from(store),
                        dishService.getOnSaleDishesByStoreId(store.getId())))
            .toList();
    long totalElements =
        storeRepository.countByLocationRange(
            minLatitude, maxLatitude, minLongitude, maxLongitude, category);

    return StorePageResult.of(results, page, size, totalElements);
  }
}
