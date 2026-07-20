package kr.lastdish.core.store.infrastructure;

import kr.lastdish.core.store.domain.Store;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreJpaRepository extends JpaRepository<Store, Long> {

  boolean existsByMemberId(Long memberId);

  boolean existsByBusinessNumber(String businessNumber);
}
