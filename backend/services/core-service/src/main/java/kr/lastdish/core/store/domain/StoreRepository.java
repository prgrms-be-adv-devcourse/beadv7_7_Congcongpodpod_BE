package kr.lastdish.core.store.domain;

import java.util.Optional;

public interface StoreRepository {

  Store save(Store store);

  Optional<Store> findById(Long storeId);

  boolean existsByMemberId(Long memberId);

  boolean existsByBusinessNumber(String businessNumber);
}
