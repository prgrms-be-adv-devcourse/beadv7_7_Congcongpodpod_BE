package kr.lastdish.core.settlement.application;

import java.util.List;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.api.exception.CommonErrorCode;
import kr.lastdish.core.settlement.application.dto.*;
import kr.lastdish.core.settlement.domain.*;
import kr.lastdish.core.settlement.presentation.dto.SettlementDetailResponse;
import kr.lastdish.core.settlement.presentation.dto.SettlementResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {
  private final SettlementCalculator settlementCalculator;
  private final SettlementRepository settlementRepository;
  private final SettlementDetailRepository settlementDetailRepository;
  private final SettlementStoreReader settlementStoreReader;

  @Transactional(readOnly = true)
  public Page<SettlementResponse> getSettlements(Long memberId, String role, Pageable pageable) {
    if (!role.equals("SELLER")) {
      throw new BusinessException(CommonErrorCode.INVALID_INPUT, "판매자만 정산 내역을 조회할 수 있습니다.");
    }
    Long storeId = settlementStoreReader.readStoreIdByMemberId(memberId);

    return settlementRepository.findAllByStoreId(storeId, pageable).map(SettlementResponse::from);
  }

  @Transactional(readOnly = true)
  public SettlementDetailResponse getSettlement(Long memberId, String role, Long settlementId) {
    if (!role.equals("SELLER")) {
      throw new BusinessException(CommonErrorCode.INVALID_INPUT, "판매자만 정산 내역을 조회할 수 있습니다.");
    }
    Long storeId = settlementStoreReader.readStoreIdByMemberId(memberId);

    Settlement settlement =
        settlementRepository
            .findByIdAndStoreId(settlementId, storeId)
            .orElseThrow(
                () -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "정산 정보를 찾을 수 없습니다."));

    List<SettlementDetail> details = settlementDetailRepository.findAllBySettlementId(settlementId);

    return SettlementDetailResponse.of(settlement, details);
  }
}
