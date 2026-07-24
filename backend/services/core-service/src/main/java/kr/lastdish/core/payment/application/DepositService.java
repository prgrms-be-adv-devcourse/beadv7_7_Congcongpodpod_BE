package kr.lastdish.core.payment.application;

import java.math.BigDecimal;
import kr.lastdish.core.payment.application.dto.DepositBalanceResponse;
import kr.lastdish.core.payment.application.dto.DepositTransactionResult;
import kr.lastdish.core.payment.domain.deposit.Deposit;
import kr.lastdish.core.payment.domain.deposit.DepositHistory;
import kr.lastdish.core.payment.domain.deposit.DepositNotFoundException;
import kr.lastdish.core.payment.infrastructure.DepositHistoryRepository;
import kr.lastdish.core.payment.infrastructure.DepositRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DepositService {

  private final DepositRepository depositRepository;
  private final DepositHistoryRepository depositHistoryRepository;

  // 회원의 현재 예치금 잔액 조회 로직
  @Transactional
  public DepositBalanceResponse getDepositBalance(Long memberId) {
    return DepositBalanceResponse.from(getOrCreateDeposit(memberId));
  }

  // 회원의 예치금 조회 시점에 예치금 정보가 생성되지 않은 회원인 경우, NPE를 방지하기 위해 잔액이 0원인 지갑을 신규 생성하여 반환
  @Transactional
  public Deposit getOrCreateDeposit(Long memberId) {
    return depositRepository
        .findByMemberId(memberId)
        .orElseGet(() -> depositRepository.save(Deposit.createDefault(memberId)));
  }

  // 회원 예치금 사용 시 차감 후 기록
  @Transactional
  public DepositTransactionResult use(Long memberId, Long orderId, BigDecimal amount) {
    Deposit deposit =
        depositRepository
            .findWithLockByMemberId(memberId)
            .orElseThrow(() -> new DepositNotFoundException(memberId));

    deposit.use(amount);

    DepositHistory history =
        depositHistoryRepository.save(
            DepositHistory.recordUse(memberId, orderId, amount, deposit.getBalance()));

    return DepositTransactionResult.from(history);
  }

  // 주문 취소 시 예치금 환불 후 기록
  @Transactional
  public DepositTransactionResult refund(Long memberId, Long orderId, BigDecimal amount) {
    Deposit deposit =
        depositRepository
            .findWithLockByMemberId(memberId)
            .orElseThrow(() -> new DepositNotFoundException(memberId));

    deposit.refund(amount);

    DepositHistory history =
        depositHistoryRepository.save(
            DepositHistory.recordRefund(memberId, orderId, amount, deposit.getBalance()));

    return DepositTransactionResult.from(history);
  }

  // 예치금 충전. 결제 승인 성공 시 잔액을 늘리고 기록
  @Transactional
  public DepositTransactionResult charge(Long memberId, Long paymentId, BigDecimal amount) {
    Deposit deposit =
        depositRepository
            .findWithLockByMemberId(memberId)
            .orElseGet(() -> depositRepository.save(Deposit.createDefault(memberId)));

    deposit.charge(amount);

    DepositHistory history =
        depositHistoryRepository.save(
            DepositHistory.recordCharge(memberId, paymentId, amount, deposit.getBalance()));

    return DepositTransactionResult.from(history);
  }
}
