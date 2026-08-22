package kr.lastdish.core.dish.application.port.in;

import java.util.List;
import java.util.Map;
import kr.lastdish.core.dish.application.dto.DishQuerySnapshot;

public interface DishQueryUseCase {

  Map<Long, List<DishQuerySnapshot>> findOnSaleByStoreIds(List<Long> storeIds);
}
