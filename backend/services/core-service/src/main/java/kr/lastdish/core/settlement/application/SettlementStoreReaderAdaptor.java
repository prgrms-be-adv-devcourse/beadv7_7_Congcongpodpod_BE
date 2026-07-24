package kr.lastdish.core.settlement.application;

import java.util.List;
import kr.lastdish.core.store.application.StoreFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SettlementStoreReaderAdaptor implements SettlementStoreReader {
  private final StoreFacade storeFacade;

  @Override
  public List<Long> readSettlementTargetStoreIds() {
    return storeFacade.findSettlementTargetStoreIds();
  }
}
