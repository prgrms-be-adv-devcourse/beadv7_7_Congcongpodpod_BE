package kr.lastdish.ai.application;

import java.util.List;
import kr.lastdish.ai.domain.document.StoreDocument;
import kr.lastdish.ai.domain.model.ParsedSearchCondition;
import kr.lastdish.ai.infrastructure.embedding.EmbeddingService;
import kr.lastdish.ai.infrastructure.llm.LlmParsingService;
import kr.lastdish.ai.presentation.dto.StoreSearchRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreSearchFacade {

  private final LlmParsingService llmParsingService;
  private final EmbeddingService embeddingService;
  private final SearchService searchService;

  /** 사용자 자연어 쿼리를 해석하고 하이브리드 검색을 실행합니다. Spring MVC 동기 처리 */
  public List<SearchHit<StoreDocument>> search(StoreSearchRequest request) {
    StopWatch stopWatch = new StopWatch();

    // 1. LLM 조건 파싱
    stopWatch.start("1. LLM 파싱");
    ParsedSearchCondition cond = llmParsingService.parseUserQuery(request.getQuery());
    stopWatch.stop();

    // 2. 거리 반경 Fallback 처리
    Double effectiveDistance =
        (cond.maxDistanceKm() != null) ? cond.maxDistanceKm() : request.getRadiusKm();

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
    stopWatch.start("2. 임베딩");
    List<Float> queryVector = embeddingService.getEmbeddingList(finalCond.rawIntent());
    stopWatch.stop();

    // 5. 검색 실행
    stopWatch.start("3. ES 검색");
    List<SearchHit<StoreDocument>> result =
        searchService.searchStoresAndDishes(finalCond, userLocation, queryVector);
    stopWatch.stop();

    log.info("검색 구간별 소요시간\n{}", stopWatch.prettyPrint());

    return result;
  }
}
