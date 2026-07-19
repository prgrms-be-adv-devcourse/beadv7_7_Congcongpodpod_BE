package kr.lastdish.core.payment.application;

import kr.lastdish.core.payment.application.dto.DepositHistoryResponse;
import kr.lastdish.core.payment.infrastructure.DepositHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DepositHistoryService {

    private final DepositHistoryRepository depositHistoryRepository;

    @Transactional(readOnly = true)
    public Page<DepositHistoryResponse> getHistory(Long memberId, Pageable pageable) {
        return depositHistoryRepository.findByMemberId(memberId, pageable)
                .map(DepositHistoryResponse::from);
    }
}