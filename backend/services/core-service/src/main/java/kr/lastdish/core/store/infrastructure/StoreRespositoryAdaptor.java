package kr.lastdish.core.store.infrastructure;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import kr.lastdish.core.store.domain.Category;
import kr.lastdish.core.store.domain.Store;
import kr.lastdish.core.store.domain.StoreRepository;
import kr.lastdish.core.store.domain.StoreStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StoreRespositoryAdaptor implements StoreRepository {
  private final StoreJpaRepository storeJpaRepository;

  @Override
  public Store save(Store store) {
    return storeJpaRepository.save(store);
  }

  @Override
  public Optional<Store> findById(Long storeId) {
    return storeJpaRepository.findByIdAndDeletedFalse(storeId);
  }

  @Override
  public Optional<Long> findOwnerMemberId(Long storeId) {
    /*
     * 파생 쿼리(findByIdAndDeletedFalse)나 JPQL이 아니라 PK 조회를 쓴다.
     * PK 조회만 1차 캐시를 타므로, 같은 트랜잭션에서 이미 읽은 매장이면 SQL이 아예 나가지 않는다.
     * 주문 생성은 앞서 영업 여부를 확인하며 매장을 이미 읽어 둔다(2026-08-28 실측: 여기서 1쿼리 절약).
     * 캐시에 없으면 평소처럼 한 번 조회하므로 어느 경우에도 손해가 없다.
     *
     * PK 조회는 삭제 여부를 걸러 주지 않으므로 직접 확인한다.
     */
    return storeJpaRepository
        .findById(storeId)
        .filter(store -> !store.isDeleted())
        .map(Store::getMemberId);
  }

  @Override
  public List<Store> findAllByIdIn(List<Long> storeIds) {
    return storeJpaRepository.findAllByIdInAndDeletedFalse(storeIds);
  }

  @Override
  public Optional<Store> findWithLockById(Long storeId) {
    return storeJpaRepository.findWithLockByIdAndDeletedFalse(storeId);
  }

  @Override
  public boolean existsByMemberId(Long memberId) {
    return storeJpaRepository.existsByMemberId(memberId);
  }

  @Override
  public Optional<Store> findByMemberId(Long memberId) {
    return storeJpaRepository.findByMemberIdAndDeletedFalse(memberId);
  }

  @Override
  public Optional<Store> findByMemberIdForSettlement(Long memberId) {
    return storeJpaRepository.findByMemberId(memberId);
  }

  @Override
  public boolean existsByBusinessNumber(String businessNumber) {
    return storeJpaRepository.existsByBusinessNumber(businessNumber);
  }

  @Override
  public List<Store> findOpenStoresByLocationRange(
      BigDecimal minLatitude,
      BigDecimal maxLatitude,
      BigDecimal minLongitude,
      BigDecimal maxLongitude,
      Category category,
      int page,
      int size) {
    PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));

    return storeJpaRepository
        .findOpenStoresByLocationRange(
            minLatitude,
            maxLatitude,
            minLongitude,
            maxLongitude,
            StoreStatus.OPEN,
            category,
            pageable)
        .getContent();
  }

  @Override
  public long countByLocationRange(
      BigDecimal minLatitude,
      BigDecimal maxLatitude,
      BigDecimal minLongitude,
      BigDecimal maxLongitude,
      Category category) {
    return storeJpaRepository.countOpenStoresByLocationRange(
        minLatitude, maxLatitude, minLongitude, maxLongitude, StoreStatus.OPEN, category);
  }

  @Override
  public List<Long> findAllActiveStoreIds() {
    return storeJpaRepository.findAllActiveStoreIds();
  }

  @Override
  public List<Long> findStoreIdsReadyToClose(LocalDateTime now) {
    return storeJpaRepository.findStoreIdsReadyToClose(now);
  }

  @Override
  public List<Store> findRenewalTargets(LocalDateTime from, LocalDateTime to) {
    return storeJpaRepository.findRenewalTargets(from, to);
  }
}
