package kr.lastdish.core.store.infrastructure;

import java.math.BigDecimal;
import java.util.Optional;
import kr.lastdish.core.store.domain.Store;
import kr.lastdish.core.store.domain.StoreStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreJpaRepository extends JpaRepository<Store, Long> {
  Optional<Store> findByIdAndDeletedFalse(Long storeId);

  boolean existsByMemberId(Long memberId);

  boolean existsByBusinessNumber(String businessNumber);

  Page<Store> findAllByLatitudeBetweenAndLongitudeBetweenAndStatusAndDeletedFalse(
      BigDecimal minLatitude,
      BigDecimal maxLatitude,
      BigDecimal minLongitude,
      BigDecimal maxLongitude,
      StoreStatus status,
      Pageable pageable);

  long countByLatitudeBetweenAndLongitudeBetweenAndStatusAndDeletedFalse(
      BigDecimal minLatitude,
      BigDecimal maxLatitude,
      BigDecimal minLongitude,
      BigDecimal maxLongitude,
      StoreStatus status);
}
