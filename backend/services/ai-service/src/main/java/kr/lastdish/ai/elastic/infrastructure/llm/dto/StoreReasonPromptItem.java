package kr.lastdish.ai.elastic.infrastructure.llm.dto;

import java.util.List;

public record StoreReasonPromptItem(
    Long storeId,
    String storeName,
    String dishName,
    Double distanceKm,
    Double discountRate,
    List<String> badges) {}
