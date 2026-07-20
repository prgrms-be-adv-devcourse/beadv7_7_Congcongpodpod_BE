package kr.lastdish.core.store.domain;

import java.util.Optional;

public interface StorePayoutAccountRepository {

    StorePayoutAccount save(StorePayoutAccount payoutAccount);

    Optional<StorePayoutAccount> findByStoreId(Long storeId);

    boolean existsByStoreId(Long storeId);
}
