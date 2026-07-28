package kr.lastdish.core.store.infrastructure;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import kr.lastdish.core.store.domain.Category;
import kr.lastdish.core.store.domain.Store;
import kr.lastdish.core.store.domain.StoreStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreJpaRepository extends JpaRepository<Store, Long> {
  Optional<Store> findByIdAndDeletedFalse(Long storeId);

  boolean existsByMemberId(Long memberId);

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

  Optional<Store> findByMemberId(Long memberId);
}
