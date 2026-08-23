package kr.lastdish.ai.application;

import java.util.List;
import kr.lastdish.ai.domain.document.StoreDocument;
import kr.lastdish.ai.domain.model.ParsedSearchCondition;
import kr.lastdish.ai.infrastructure.embedding.EmbeddingService;
import kr.lastdish.ai.infrastructure.llm.LlmParsingService;
import kr.lastdish.ai.presentation.dto.StoreSearchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StoreSearchFacade {

  private final LlmParsingService llmParsingService;
  private final EmbeddingService embeddingService;
  private final SearchService searchService;

  /** 사용자 자연어 쿼리를 해석하고 하이브리드 검색을 실행합니다. Spring MVC 동기 처리 */
  public List<SearchHit<StoreDocument>> search(StoreSearchRequest request) {
    // 1. LLM 조건 파싱
    ParsedSearchCondition cond = llmParsingService.parseUserQuery(request.getQuery());

    // 2. 거리 반경 Fallback 처리
    // LLM이 쿼리에서 거리를 추출하지 못했으면(null), 프론트에서 받은 radiusKm 사용
    Double effectiveDistance =
        (cond.maxDistanceKm() != null) ? cond.maxDistanceKm() : request.getRadiusKm();

    // 반경 정보가 보완된 새로운 ParsedSearchCondition 구성
    ParsedSearchCondition finalCond =
        new ParsedSearchCondition(
            cond.maxPrice(),
            effectiveDistance,
            cond.pickupDeadline(),
            cond.category(),
            cond.rawIntent());

    // 3. 위치 객체 생성
    GeoPoint userLocation = null;
    if (request.getLatitude() != null && request.getLongitude() != null) {
      userLocation = new GeoPoint(request.getLatitude(), request.getLongitude());
    }

    // 4. 키워드 임베딩 벡터화
    List<Float> queryVector = embeddingService.getEmbeddingList(finalCond.rawIntent());

    // 5. 검색 실행
    return searchService.searchStoresAndDishes(finalCond, userLocation, queryVector);
  }
}
