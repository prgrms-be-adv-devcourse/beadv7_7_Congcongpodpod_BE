package kr.lastdish.ai.domain.model;

import java.math.BigDecimal;
import java.time.LocalTime;
import lombok.Builder;

@Builder
public record ParsedSearchCondition(
    BigDecimal maxPrice, // 예산 상한선
    Double maxDistanceKm, // 검색 반경
    LocalTime pickupDeadline, // 희망 픽업 시각
    String category, // 카테고리
    String rawIntent // kNN/Match 검색에 사용할 키워드/의도 문장 (예: "든든한 저녁")
    ) {}
