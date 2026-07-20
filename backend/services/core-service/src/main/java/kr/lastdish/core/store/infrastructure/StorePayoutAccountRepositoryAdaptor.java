package kr.lastdish.core.store.infrastructure;

import kr.lastdish.core.store.domain.StorePayoutAccount;
import kr.lastdish.core.store.domain.StorePayoutAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StorePayoutAccountRepositoryAdaptor implements StorePayoutAccountRepository {
    private final StorePayoutAccountJpaRepository payoutAccountJpaRepository;

    @Override
    public StorePayoutAccount save(StorePayoutAccount payoutAccount) {
        return payoutAccountJpaRepository.save(payoutAccount);
    }

    @Override
    public Optional<StorePayoutAccount> findByStoreId(Long storeId) {
        return payoutAccountJpaRepository.findByStoreIdAndDeletedFalse(storeId);
    }

    @Override
    public boolean existsByStoreId(Long storeId) {
        return payoutAccountJpaRepository.existsByStoreIdAndDeletedFalse(storeId);
    }
}
