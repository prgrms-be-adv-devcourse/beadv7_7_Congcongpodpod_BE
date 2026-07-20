package kr.lastdish.core.store.infrastructure;

import java.util.Optional;
import kr.lastdish.core.store.domain.Store;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreJpaRepository extends JpaRepository<Store, Long> {
  Optional<Store> findByIdAndDeletedFalse(Long storeId);

  boolean existsByMemberId(Long memberId);

  boolean existsByBusinessNumber(String businessNumber);
}
