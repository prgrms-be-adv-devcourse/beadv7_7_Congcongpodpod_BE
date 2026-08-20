package kr.lastdish.core.favorite.domain;

import java.util.List;
import java.util.Optional;

public interface StoreFavoriteRepository {

  void createIfAbsent(Long memberId, Long storeId);

  Optional<StoreFavorite> findByMemberIdAndStoreId(Long memberId, Long storeId);

  List<StoreFavorite> findAllByMemberIdOrderByCreatedAtDesc(Long memberId);

  void deleteByMemberIdAndStoreId(Long memberId, Long storeId);
}
