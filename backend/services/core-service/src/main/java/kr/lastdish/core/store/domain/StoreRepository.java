package kr.lastdish.core.store.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface StoreRepository {

  Store save(Store store);

  Optional<Store> findById(Long storeId);

  boolean existsByMemberId(Long memberId);

  boolean existsByBusinessNumber(String businessNumber);

  List<Store> findOpenStoresByLocationRange(
      BigDecimal minLatitude,
      BigDecimal maxLatitude,
      BigDecimal minLongitude,
      BigDecimal maxLongitude,
      int page,
      int size);

  long countByLocationRange(
      BigDecimal minLatitude,
      BigDecimal maxLatitude,
      BigDecimal minLongitude,
      BigDecimal maxLongitude);
}
