package kr.lastdish.core.store.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StoreFacade {
  private final StoreService storeService;

  public void validateStoreOwner(Long storeId, Long memberId) {
    storeService.validateSeller(storeId, memberId);
  }
}
