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
