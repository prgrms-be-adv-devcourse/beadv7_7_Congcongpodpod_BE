package kr.lastdish.core.favorite.application.port.out;

import java.util.List;
import kr.lastdish.core.favorite.application.dto.FavoriteStoreResult;

public interface FavoriteStoreQueryPort {

  boolean existsById(Long storeId);

  List<FavoriteStoreResult> findByIds(List<Long> storeIds);
}
