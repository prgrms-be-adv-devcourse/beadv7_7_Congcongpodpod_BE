package kr.lastdish.ai.elastic.application;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import kr.lastdish.ai.elastic.domain.document.StoreDocument;
import kr.lastdish.ai.elastic.domain.model.ParsedSearchCondition;
import kr.lastdish.ai.elastic.infrastructure.embedding.EmbeddingService;
import kr.lastdish.ai.elastic.infrastructure.llm.LlmParsingService;
import kr.lastdish.ai.elastic.infrastructure.llm.StoreRecommendationReasonService;
import kr.lastdish.ai.elastic.presentation.dto.StoreSearchRequest;
import kr.lastdish.ai.elastic.presentation.dto.StoreSearchResult;
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

  private static final int DISPLAY_TOP_N = 100; // 화면에 노출수
  private static final int REASON_TOP_N = 5; // RAG 추천수

  private final LlmParsingService llmParsingService;
  private final EmbeddingService embeddingService;
  private final SearchService searchService;
  private final StoreRankingService storeRankingService;
  private final StoreRecommendationReasonService storeRecommendationReasonService;

  /** 사용자 자연어 쿼리를 해석하고 하이브리드 검색을 실행합니다. Spring MVC 동기 처리 */
  public List<StoreSearchResult> search(StoreSearchRequest request) {
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
    List<SearchHit<StoreDocument>> hits =
        searchService.searchStoresAndDishes(finalCond, userLocation, queryVector);
    stopWatch.stop();

    // TODO: 실제 픽업 완료 이력 조회로 교체 필요 (개인화 배지 계산용)
    Set<Long> completedPickupStoreIds = Collections.emptySet();

    stopWatch.start("4. 랭킹/배지");
    List<StoreSearchResult> ranked =
        storeRankingService.rankAndAssignBadges(hits, userLocation, completedPickupStoreIds);
    List<StoreSearchResult> displayResults = ranked.stream().limit(DISPLAY_TOP_N).toList();
    stopWatch.stop();

    // 상위 REASON_TOP_N개만 이유 생성 대상으로 넘김. 나머지는 reason=null로 그대로 노출됨
    stopWatch.start("5. RAG 추천 이유 생성");
    List<StoreSearchResult> reasonTargets = displayResults.stream().limit(REASON_TOP_N).toList();
    storeRecommendationReasonService.assignReasons(reasonTargets, finalCond.rawIntent());
    stopWatch.stop();

    log.info("검색 구간별 소요시간\n{}", stopWatch.prettyPrint());

    return displayResults;
  }
}
