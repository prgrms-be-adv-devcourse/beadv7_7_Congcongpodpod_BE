package kr.lastdish.core.store.application.port.in;

import java.util.List;
import kr.lastdish.core.store.application.dto.StoreQuerySnapshot;

public interface StoreQueryUseCase {

  boolean existsActiveStore(Long storeId);

  List<StoreQuerySnapshot> findActiveStores(List<Long> storeIds);
}
