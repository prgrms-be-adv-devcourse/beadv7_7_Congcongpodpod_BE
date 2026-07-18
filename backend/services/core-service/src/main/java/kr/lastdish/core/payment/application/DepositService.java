package kr.lastdish.core.payment.application;

import jakarta.transaction.Transactional;
import kr.lastdish.core.payment.domain.deposit.Deposit;
import kr.lastdish.core.payment.infrastructure.DepositRepository;
import kr.lastdish.core.payment.application.dto.DepositBalanceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DepositService {

    private final DepositRepository depositRepository;

    // 회원의 현재 예치금 잔액 조회 로직
    @Transactional
    public DepositBalanceResponse getDepositBalance(Long memberId) {
        return DepositBalanceResponse.from(getOrCreateDeposit(memberId));
    }

    /**
     * 회원의 예치금 지갑을 조회
     * 아직 예치금 지갑이 생성되지 않은 회원인 경우,
     * NPE를 방지하기 위해 잔액이 0원인 지갑을 신규 생성하여 반환
     */
    @Transactional
    public Deposit getOrCreateDeposit(Long memberId) {
        return depositRepository.findByMemberId(memberId)
                .orElseGet(() -> depositRepository.save(Deposit.createDefault(memberId)));
    }
}
