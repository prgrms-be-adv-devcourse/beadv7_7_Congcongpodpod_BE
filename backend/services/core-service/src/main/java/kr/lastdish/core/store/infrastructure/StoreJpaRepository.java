package kr.lastdish.core.store.infrastructure;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import kr.lastdish.core.store.domain.Category;
import kr.lastdish.core.store.domain.Store;
import kr.lastdish.core.store.domain.StoreStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreJpaRepository extends JpaRepository<Store, Long> {
  Optional<Store> findByIdAndDeletedFalse(Long storeId);

  List<Store> findAllByIdInAndDeletedFalse(List<Long> storeIds);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<Store> findWithLockByIdAndDeletedFalse(Long storeId);

  boolean existsByMemberId(Long memberId);

  Optional<Store> findByMemberIdAndDeletedFalse(Long memberId);

  Optional<Store> findByMemberId(Long memberId);

  boolean existsByBusinessNumber(String businessNumber);

  @Query(
      """
      SELECT s FROM Store s
      WHERE s.latitude BETWEEN :minLatitude AND :maxLatitude
        AND s.longitude BETWEEN :minLongitude AND :maxLongitude
        AND s.status = :status
        AND s.deleted = false
        AND (:category IS NULL OR s.category = :category)
      """)
  Page<Store> findOpenStoresByLocationRange(
      @Param("minLatitude") BigDecimal minLatitude,
      @Param("maxLatitude") BigDecimal maxLatitude,
      @Param("minLongitude") BigDecimal minLongitude,
      @Param("maxLongitude") BigDecimal maxLongitude,
      @Param("status") StoreStatus status,
      @Param("category") Category category,
      Pageable pageable);

  @Query(
      """
          SELECT COUNT(s) FROM Store s
          WHERE s.latitude BETWEEN :minLatitude AND :maxLatitude
            AND s.longitude BETWEEN :minLongitude AND :maxLongitude
            AND s.status = :status
            AND s.deleted = false
            AND (:category IS NULL OR s.category = :category)
          """)
  long countOpenStoresByLocationRange(
      @Param("minLatitude") BigDecimal minLatitude,
      @Param("maxLatitude") BigDecimal maxLatitude,
      @Param("minLongitude") BigDecimal minLongitude,
      @Param("maxLongitude") BigDecimal maxLongitude,
      @Param("status") StoreStatus status,
      @Param("category") Category category);

  @Query(
      """
        SELECT store.id
        FROM Store store
        WHERE store.deleted IS false
        """)
  List<Long> findAllActiveStoreIds();

  // 마감 대상 매장을 매장별 트랜잭션에서 처리할 수 있도록 ID만 조회한다.
  @Query(
      """
      SELECT store.id
      FROM Store store
      WHERE store.deleted IS false
        AND store.status = "OPEN"
        AND store.nextClosingAt <= :now
      ORDER BY store.id
      """)
  List<Long> findStoreIdsReadyToClose(@Param("now") LocalDateTime now);
}
