package kr.lastdish.core.store.infrastructure;

import kr.lastdish.core.store.domain.Store;
import kr.lastdish.core.store.domain.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StoreRespositoryAdaptor implements StoreRepository {
    private final StoreJpaRepository storeJpaRepository;

    @Override
    public Store save(Store store) {
        return storeJpaRepository.save(store);
    }

    @Override
    public Optional<Store> findById(Long storeId) {
        return storeJpaRepository.findById(storeId).filter(store -> !store.isDeleted());
    }

    @Override
    public boolean existsByMemberId(Long memberId) {
        return storeJpaRepository.existsByMemberId(
                memberId
        );
    }

    @Override
    public boolean existsByBusinessNumber(
            String businessNumber
    ) {
        return storeJpaRepository
                .existsByBusinessNumber(businessNumber);
    }
}
