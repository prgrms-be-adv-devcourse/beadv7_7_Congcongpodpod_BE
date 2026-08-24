package kr.lastdish.core.store.application;

import java.util.List;
import kr.lastdish.core.store.application.dto.StoreQuerySnapshot;
import kr.lastdish.core.store.application.port.in.StoreQueryUseCase;
import kr.lastdish.core.store.domain.Store;
import kr.lastdish.core.store.domain.StoreHoliday;
import kr.lastdish.core.store.domain.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreQueryService implements StoreQueryUseCase {

  private final StoreRepository storeRepository;

  @Override
  public boolean existsActiveStore(Long storeId) {
    return storeRepository.findById(storeId).isPresent();
  }

  @Override
  public List<StoreQuerySnapshot> findActiveStores(List<Long> storeIds) {
    if (storeIds.isEmpty()) {
      return List.of();
    }
    return storeRepository.findAllByIdIn(storeIds).stream().map(this::toSnapshot).toList();
  }

  private StoreQuerySnapshot toSnapshot(Store store) {
    return new StoreQuerySnapshot(
        store.getId(),
        store.getMemberId(),
        store.getStoreName(),
        store.getBusinessNumber(),
        store.getStoreAddress(),
        store.getStorePhone(),
        store.getOpenTime(),
        store.getCloseTime(),
        store.getStatus().name(),
        store.getLatitude(),
        store.getLongitude(),
        store.getCategory().name(),
        store.getHolidays().stream().map(StoreHoliday::getDayOfWeek).toList());
  }
}
