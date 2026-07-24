package kr.lastdish.core.store.application;

import java.math.BigDecimal;
import java.util.List;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.api.exception.CommonErrorCode;
import kr.lastdish.core.common.exception.ErrorCode;
import kr.lastdish.core.store.application.dto.*;
import kr.lastdish.core.store.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService {

  private final StoreRepository storeRepository;
  private final StorePayoutAccountRepository payoutAccountRepository;

  @Transactional
  public StoreResult register(RegisterStoreCommand command) {
    if (storeRepository.existsByMemberId(command.memberId())) {
      throw new BusinessException(CommonErrorCode.INVALID_INPUT, "회원은 하나의 매장만 등록할 수 있습니다.");
    }

    if (storeRepository.existsByBusinessNumber(command.businessNumber())) {
      throw new BusinessException(CommonErrorCode.INVALID_INPUT, "이미 등록된 사업자등록번호입니다.");
    }

    Store store =
        new Store(
            command.memberId(),
            command.storeName(),
            command.businessNumber(),
            command.storeAddress(),
            command.storePhone(),
            command.openTime(),
            command.closeTime(),
            command.latitude(),
            command.longitude());

    command.holidays().forEach(store::addHoliday);

    Store savedStore = storeRepository.save(store);

    return StoreResult.from(savedStore);
  }

  @Transactional
  public StoreResult update(Long storeId, Long memberId, UpdateStoreCommand command) {
    Store store = getOwnedStore(storeId, memberId);

    store.update(
        command.storeName(),
        command.storeAddress(),
        command.storePhone(),
        command.openTime(),
        command.closeTime(),
        command.latitude(),
        command.longitude());

    store.replaceHolidays(command.holidays());

    return StoreResult.from(store);
  }

  @Transactional
  public StoreResult changeStatus(Long storeId, Long memberId, StoreStatus status) {
    Store store = getOwnedStore(storeId, memberId);

    store.changeStatus(status);

    return StoreResult.from(store);
  }

  // 삭제 시 매장 조회, 수정, 상태 변경 제외, 매장 재등록은 재가입 필요
  // 매장 soft delete 시 휴무일 hard delete
  @Transactional
  public void deleteStore(Long storeId, Long memberId) {
    Store store = getOwnedStore(storeId, memberId);

    store.delete();
  }

  private Store getOwnedStore(Long storeId, Long memberId) {
    Store store =
        storeRepository
            .findById(storeId)
            .orElseThrow(
                () -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "매장을 찾을 수 없습니다."));

    if (!store.isOwnedBy(memberId)) {
      throw new BusinessException(CommonErrorCode.INVALID_INPUT, "해당 매장을 수정할 권한이 없습니다.");
    }

    return store;
  }

  // 매장 상세 조회
  public StoreResult getStore(Long storeId) {
    Store store =
        storeRepository
            .findById(storeId)
            .orElseThrow(() -> new IllegalArgumentException("매장을 찾을 수 없습니다."));

    return StoreResult.from(store);
  }

  // 위치 기반 조회(기본 구현)
  public StorePageResult getNearbyStores(
      BigDecimal latitude, BigDecimal longitude, double radiusKm, int page, int size) {
    if (radiusKm <= 0) {
      throw new BusinessException(CommonErrorCode.INVALID_INPUT, "검색 반경은 0보다 커야 합니다.");
    }

    if (page < 0) {
      throw new BusinessException(CommonErrorCode.INVALID_INPUT, "페이지 번호는 0 이상이어야 합니다.");
    }

    if (size <= 0) {
      throw new BusinessException(CommonErrorCode.INVALID_INPUT, "페이지 크기는 0보다 커야 합니다.");
    }

    double latitudeValue = latitude.doubleValue();

    double latitudeDelta = radiusKm / 111.0;

    double longitudeDivisor = 111.0 * Math.cos(Math.toRadians(latitudeValue));

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
            minLatitude, maxLatitude, minLongitude, maxLongitude, page, size);

    long totalElements =
        storeRepository.countByLocationRange(minLatitude, maxLatitude, minLongitude, maxLongitude);

    return StorePageResult.of(stores, page, size, totalElements);
  }

  // 매장 정산 계좌
  @Transactional
  public PayoutAccountResult registerPayoutAccount(
      Long storeId, Long memberId, String accountNumber, String accountHolder) {
    getOwnedStore(storeId, memberId);

    if (payoutAccountRepository.existsByStoreId(storeId)) {
      throw new BusinessException(CommonErrorCode.INVALID_INPUT, "이미 등록된 정산 계좌가 있습니다.");
    }

    StorePayoutAccount payoutAccount =
        new StorePayoutAccount(storeId, accountNumber, accountHolder);

    StorePayoutAccount savedAccount = payoutAccountRepository.save(payoutAccount);

    return PayoutAccountResult.from(savedAccount);
  }

  @Transactional
  public PayoutAccountResult updatePayoutAccount(
      Long storeId, Long memberId, String accountNumber, String accountHolder) {
    getOwnedStore(storeId, memberId);

    StorePayoutAccount payoutAccount =
        payoutAccountRepository
            .findByStoreId(storeId)
            .orElseThrow(
                () ->
                    new BusinessException(
                        CommonErrorCode.ENTITY_NOT_FOUND, "등록된 정산 계좌를 찾을 수 없습니다."));

    payoutAccount.update(accountNumber, accountHolder);

    return PayoutAccountResult.from(payoutAccount);
  }

  // StoreFacade 검증 메서드
  public void validateSeller(Long storeId, Long memberId) {
    Store store =
        storeRepository
            .findById(storeId)
            .orElseThrow(
                () -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "매장을 찾을 수 없습니다."));

    if (!store.isOwnedBy(memberId)) {
      throw new BusinessException(ErrorCode.ORDER_NOT_SELLER);
    }
  }

  @Transactional(readOnly = true)
  public List<Long> findSettlementTargetStoreIds() {
    return storeRepository.findAllActiveStoreIds();
  }
}
