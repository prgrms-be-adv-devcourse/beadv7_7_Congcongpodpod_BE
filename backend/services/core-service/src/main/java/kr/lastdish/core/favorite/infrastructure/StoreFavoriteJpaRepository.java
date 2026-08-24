package kr.lastdish.core.favorite.infrastructure;

import java.util.List;
import java.util.Optional;
import kr.lastdish.core.favorite.domain.StoreFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreFavoriteJpaRepository extends JpaRepository<StoreFavorite, Long> {

  @Modifying(flushAutomatically = true)
  @Query(
      value =
          """
          INSERT INTO store_favorites (member_id, store_id, created_at)
          VALUES (:memberId, :storeId, NOW())
          ON CONFLICT (member_id, store_id) DO NOTHING
          """,
      nativeQuery = true)
  void createIfAbsent(@Param("memberId") Long memberId, @Param("storeId") Long storeId);

  Optional<StoreFavorite> findByMemberIdAndStoreId(Long memberId, Long storeId);

  List<StoreFavorite> findAllByMemberIdOrderByCreatedAtDesc(Long memberId);

  @Modifying(flushAutomatically = true)
  @Query(
      """
      DELETE FROM StoreFavorite favorite
      WHERE favorite.memberId = :memberId
        AND favorite.storeId = :storeId
      """)
  void deleteByMemberIdAndStoreId(@Param("memberId") Long memberId, @Param("storeId") Long storeId);
}
