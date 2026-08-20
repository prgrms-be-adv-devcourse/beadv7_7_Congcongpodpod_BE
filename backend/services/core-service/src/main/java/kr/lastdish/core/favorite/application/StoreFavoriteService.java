package kr.lastdish.core.favorite.application;

import java.util.List;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.api.exception.CommonErrorCode;
import kr.lastdish.core.favorite.application.dto.FavoriteStoreResult;
import kr.lastdish.core.favorite.application.port.out.FavoriteStoreQueryPort;
import kr.lastdish.core.favorite.domain.StoreFavorite;
import kr.lastdish.core.favorite.domain.StoreFavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreFavoriteService {

  private final StoreFavoriteRepository storeFavoriteRepository;
  private final FavoriteStoreQueryPort favoriteStoreQueryPort;

  // 매장을 찜한다. 이미 찜한 매장이면 아무것도 하지 않는다(멱등).
  @Transactional
  public void addFavorite(Long memberId, Long storeId) {
    if (!favoriteStoreQueryPort.existsById(storeId)) {
      throw new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "매장을 찾을 수 없습니다.");
    }

    storeFavoriteRepository.createIfAbsent(memberId, storeId);
  }

  // 찜한 매장 목록을 최근 추가순으로 조회한다. 삭제된 매장은 건너뛴다.
  public List<FavoriteStoreResult> getFavorites(Long memberId) {
    List<Long> storeIds =
        storeFavoriteRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId).stream()
            .map(StoreFavorite::getStoreId)
            .toList();

    return favoriteStoreQueryPort.findByIds(storeIds);
  }

  // 매장 찜을 해제한다. 찜하지 않은 매장이면 아무것도 하지 않는다(멱등).
  @Transactional
  public void removeFavorite(Long memberId, Long storeId) {
    storeFavoriteRepository.deleteByMemberIdAndStoreId(memberId, storeId);
  }

  public boolean isFavorite(Long memberId, Long storeId) {
    return storeFavoriteRepository.findByMemberIdAndStoreId(memberId, storeId).isPresent();
  }
}
