package kr.lastdish.core.store.infrastructure;

import kr.lastdish.core.store.domain.StorePayoutAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StorePayoutAccountJpaRepository extends JpaRepository<StorePayoutAccount, Long> {
    Optional<StorePayoutAccount> findByStoreIdAndDeletedFalse(Long storeId);

    boolean existsByStoreIdAndDeletedFalse(Long storeId);
}
