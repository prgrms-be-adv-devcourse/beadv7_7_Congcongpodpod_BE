package kr.lastdish.core.favorite.infrastructure;

import java.util.List;
import java.util.Optional;
import kr.lastdish.core.favorite.domain.StoreFavorite;
import kr.lastdish.core.favorite.domain.StoreFavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StoreFavoriteRepositoryAdapter implements StoreFavoriteRepository {

  private final StoreFavoriteJpaRepository storeFavoriteJpaRepository;

  @Override
  public void createIfAbsent(Long memberId, Long storeId) {
    storeFavoriteJpaRepository.createIfAbsent(memberId, storeId);
  }

  @Override
  public Optional<StoreFavorite> findByMemberIdAndStoreId(Long memberId, Long storeId) {
    return storeFavoriteJpaRepository.findByMemberIdAndStoreId(memberId, storeId);
  }

  @Override
  public List<StoreFavorite> findAllByMemberIdOrderByCreatedAtDesc(Long memberId) {
    return storeFavoriteJpaRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId);
  }

  @Override
  public void deleteByMemberIdAndStoreId(Long memberId, Long storeId) {
    storeFavoriteJpaRepository.deleteByMemberIdAndStoreId(memberId, storeId);
  }
}
