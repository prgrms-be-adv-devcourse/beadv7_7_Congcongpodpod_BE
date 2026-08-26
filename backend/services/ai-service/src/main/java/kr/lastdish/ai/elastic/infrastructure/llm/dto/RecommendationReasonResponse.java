package kr.lastdish.ai.elastic.infrastructure.llm.dto;

import java.util.List;

public record RecommendationReasonResponse(List<StoreReason> reasons) {
  public record StoreReason(Long storeId, String reason) {}
}
