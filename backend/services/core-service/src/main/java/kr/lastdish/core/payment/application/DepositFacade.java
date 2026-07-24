package kr.lastdish.core.payment.application;

import java.math.BigDecimal;
import kr.lastdish.core.payment.application.dto.DepositTransactionResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DepositFacade {
  private final DepositService depositService;

  public DepositTransactionResult use(Long memberId, Long orderId, BigDecimal amount) {
    return depositService.use(memberId, orderId, amount);
  }

  public DepositTransactionResult refund(Long memberId, Long orderId, BigDecimal amount) {
    return depositService.refund(memberId, orderId, amount);
  }

  public DepositTransactionResult charge(Long memberId, Long paymentId, BigDecimal amount) {
    return depositService.charge(memberId, paymentId, amount);
  }
}
