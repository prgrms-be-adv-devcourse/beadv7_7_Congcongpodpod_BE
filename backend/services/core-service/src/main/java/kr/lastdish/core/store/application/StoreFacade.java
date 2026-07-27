package kr.lastdish.core.store.application;

import kr.lastdish.core.store.application.dto.RegisterStoreCommand;
import kr.lastdish.core.store.application.dto.StoreResult;
import kr.lastdish.core.store.application.port.out.SellerRoleGrantPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StoreFacade {
  private final StoreService storeService;
  private final SellerRoleGrantPort sellerRoleGrantPort;

  public StoreResult register(RegisterStoreCommand command) {
    StoreResult result = storeService.register(command);

    // TODO: 역할 부여 호출 실패 시 재시도와 멱등성을 보장해 매장과 회원 권한의 불일치를 복구한다.
    sellerRoleGrantPort.grantSellerRole(command.memberId());

    return result;
  }

  public void validateStoreOwner(Long storeId, Long memberId) {
    storeService.validateSeller(storeId, memberId);
  }
}
