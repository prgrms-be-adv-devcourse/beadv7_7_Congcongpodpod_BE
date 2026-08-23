package kr.lastdish.core.settlement.infrastructure;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import kr.lastdish.core.settlement.domain.SettlementDetail;
import kr.lastdish.core.settlement.domain.SettlementDetailRepository;
import kr.lastdish.core.settlement.domain.SettlementDetailSummary;
import kr.lastdish.core.settlement.domain.SettlementDetailSummaryProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SettlementDetailRepositoryAdaptor implements SettlementDetailRepository {
  private final JpaSettlementDetailRepository jpaSettlementDetailRepository;
  private final JdbcSettlementDetailRepository jdbcSettlementDetailRepository;

  @Override
  public SettlementDetail save(SettlementDetail settlementDetail) {
    return jpaSettlementDetailRepository.save(settlementDetail);
  }

  @Override
  public List<SettlementDetail> saveAll(List<SettlementDetail> settlementDetails) {
    return jpaSettlementDetailRepository.saveAll(settlementDetails);
  }

  @Override
  public void bulkInsert(List<SettlementDetail> settlementDetails) {
    jdbcSettlementDetailRepository.bulkInsert(settlementDetails);
  }

  @Override
  public boolean existsByOrderId(Long orderId) {
    return jpaSettlementDetailRepository.existsByOrderId(orderId);
  }

  @Override
  public Set<Long> findSettledOrderIds(Collection<Long> orderIds) {
    if (orderIds == null || orderIds.isEmpty()) {
      return Set.of();
    }

    return new HashSet<>(jpaSettlementDetailRepository.findSettledOrderIds(orderIds));
  }

  @Override
  public List<SettlementDetail> findAllBySettlementId(Long settlementId) {
    return jpaSettlementDetailRepository.findAllBySettlementIdOrderByIdAsc(settlementId);
  }

  @Override
  public SettlementDetailSummary summarizeBySettlementId(Long settlementId) {
    SettlementDetailSummaryProjection result =
        jpaSettlementDetailRepository.summarizeBySettlementId(settlementId);

    return new SettlementDetailSummary(
        result.getTotalOrderCount(),
        result.getGrossAmount(),
        result.getFeeAmount(),
        result.getSettlementAmount());
  }
}
