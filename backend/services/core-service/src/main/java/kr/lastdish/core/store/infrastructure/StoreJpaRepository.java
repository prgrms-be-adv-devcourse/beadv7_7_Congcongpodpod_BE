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
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreJpaRepository extends JpaRepository<Store, Long> {
  Optional<Store> findByIdAndDeletedFalse(Long storeId);

  // holidays를 함께 가져온다. 호출부(StoreQueryService.toSnapshot)가 매장마다 이 컬렉션을 읽는데,
  // 지연 로딩이면 매장 수만큼 SELECT가 더 나간다. 주문 목록 조회에서 매장 31개에 34쿼리가
  // 나가던 원인이다(실측 2026-08-28). 목록 조회라 페이징이 없어 컬렉션 fetch join이 안전하다.
  @EntityGraph(attributePaths = "holidays")
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

  @Query(
      value =
          """
          SELECT DISTINCT s.*
          FROM stores s
          LEFT JOIN dishes d
            ON d.store_id = s.store_id
          WHERE (s.updated_at >= :from AND s.updated_at < :to)
             OR (d.updated_at >= :from AND d.updated_at < :to)
          ORDER BY s.store_id
          """,
      nativeQuery = true)
  List<Store> findRenewalTargets(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
