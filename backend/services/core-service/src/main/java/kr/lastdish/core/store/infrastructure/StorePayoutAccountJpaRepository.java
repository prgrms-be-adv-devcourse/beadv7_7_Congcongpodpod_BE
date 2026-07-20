package kr.lastdish.core.store.infrastructure;

import java.util.Optional;
import kr.lastdish.core.store.domain.StorePayoutAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StorePayoutAccountJpaRepository extends JpaRepository<StorePayoutAccount, Long> {
  Optional<StorePayoutAccount> findByStoreIdAndDeletedFalse(Long storeId);

  boolean existsByStoreIdAndDeletedFalse(Long storeId);
}
