package kr.lastdish.core.store.domain;

public interface StoreRepository {

  Store save(Store store);

  boolean existsByMemberId(Long memberId);

  boolean existsByBusinessNumber(String businessNumber);
}
