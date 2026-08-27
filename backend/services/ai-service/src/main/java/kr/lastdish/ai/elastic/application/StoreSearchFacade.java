package kr.lastdish.ai.elastic.application;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
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

  // ===== 투-패스(Two-Pass) 폴백 판단 기준 =====
  // BM25 Fast-Pass 결과가 아래 조건을 만족하지 못하면 LLM 파싱 + kNN 벡터 검색으로 폴백한다.
  // ES score는 필드 부스트(storeName^1.5, dishName^2.0)와 nested max score mode의 영향을 받으므로
  // 절대값 기준은 운영 로그를 보며 튜닝이 필요하다. (아래 값은 초기 기준치)
  private static final int FAST_PASS_MIN_RESULT_COUNT = 5;
  private static final double FAST_PASS_MIN_TOP_SCORE = 3.0;

  // 단순 키워드 판별: 공백 기준 2단어 이하 & 가격/거리 등 필터성 표현이 없는 경우
  private static final Pattern FILTER_KEYWORD_PATTERN =
      Pattern.compile(".*(\\d+원|이하|이상|근처|주변|할인).*");

  private final LlmParsingService llmParsingService;
  private final EmbeddingService embeddingService;
  private final SearchService searchService;
  private final StoreRankingService storeRankingService;
  private final StoreRecommendationReasonService storeRecommendationReasonService;

  /** 사용자 자연어 쿼리를 해석하고 하이브리드 검색을 실행합니다. Spring MVC 동기 처리 */
  public List<StoreSearchResult> search(StoreSearchRequest request) {
    StopWatch stopWatch = new StopWatch();
    String rawQuery = request.getQuery() == null ? "" : request.getQuery().trim();
    GeoPoint userLocation = buildUserLocation(request);

    // 0. 단순 키워드 입력 -> LLM 파싱/임베딩을 전면 스킵하고 BM25 키워드 검색만 즉시 수행
    if (isSimpleKeyword(rawQuery)) {
      log.info("단순 키워드 판별(query=\"{}\") - LLM/임베딩 스킵, BM25 전용 검색 수행", rawQuery);
      return searchByKeywordOnly(rawQuery, request, userLocation, stopWatch);
    }

    // 1. BM25 Fast-Pass: 원문 그대로 키워드/멀티매치 검색만 먼저 수행 (LLM/벡터 미사용)
    stopWatch.start("1. BM25 Fast-Pass");
    ParsedSearchCondition fastCond =
        ParsedSearchCondition.builder()
            .maxDistanceKm(request.getRadiusKm())
            .rawIntent(rawQuery)
            .build();
    List<SearchHit<StoreDocument>> fastHits =
        searchService.searchStoresAndDishes(fastCond, userLocation, null);
    stopWatch.stop();

    // 2. 품질 검사: 결과 수/최고 점수가 기준을 만족하면 LLM 폴백 없이 즉시 반환
    if (isQualitySufficient(fastHits)) {
      log.info(
          "BM25 Fast-Pass 품질 충족(count={}, topScore={}) - LLM 폴백 없이 반환",
          fastHits.size(),
          fastHits.isEmpty() ? 0.0 : fastHits.get(0).getScore());
      return finalizeResults(fastHits, userLocation, fastCond.rawIntent(), stopWatch, true);
    }

    log.info("BM25 Fast-Pass 품질 미달(count={}) - LLM 파싱 + 벡터 검색으로 폴백", fastHits.size());

    // 3. Fallback: 기존 LLM 파싱 + 하이브리드(BM25+kNN) 검색 수행
    return executeFullLlmSearch(request, userLocation, stopWatch);
  }

  /** 단순 키워드(2단어 이하, 필터 표현 없음) 전용 경로: BM25 멀티매치 검색만 수행하고 즉시 반환한다. */
  private List<StoreSearchResult> searchByKeywordOnly(
      String rawQuery, StoreSearchRequest request, GeoPoint userLocation, StopWatch stopWatch) {

    stopWatch.start("0. 단순 키워드 BM25 전용 검색");
    ParsedSearchCondition simpleCond =
        ParsedSearchCondition.builder()
            .maxDistanceKm(request.getRadiusKm())
            .rawIntent(rawQuery)
            .build();
    List<SearchHit<StoreDocument>> hits =
        searchService.searchStoresAndDishes(simpleCond, userLocation, null);
    stopWatch.stop();

    // 단순 키워드 경로는 지연시간 최소화가 목적이므로 RAG 추천 이유 생성(LLM 호출)은 생략한다.
    return finalizeResults(hits, userLocation, simpleCond.rawIntent(), stopWatch, false);
  }

  /** 기존 전체 파이프라인: LLM 조건 파싱 -> 임베딩 -> 하이브리드(BM25+kNN) 검색 -> 랭킹 -> RAG 추천 이유. */
  private List<StoreSearchResult> executeFullLlmSearch(
      StoreSearchRequest request, GeoPoint userLocation, StopWatch stopWatch) {

    // 1. LLM 조건 파싱
    stopWatch.start("2. LLM 파싱");
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

    // 3. 키워드 임베딩 벡터화
    stopWatch.start("3. 임베딩");
    List<Float> queryVector = embeddingService.getEmbeddingList(finalCond.rawIntent());
    stopWatch.stop();

    // 4. 검색 실행 (BM25 + kNN 하이브리드)
    stopWatch.start("4. ES 검색");
    List<SearchHit<StoreDocument>> hits =
        searchService.searchStoresAndDishes(finalCond, userLocation, queryVector);
    stopWatch.stop();

    return finalizeResults(hits, userLocation, finalCond.rawIntent(), stopWatch, true);
  }

  /** 랭킹/배지 부여 후, 필요 시 상위 N개에 대해 RAG 추천 이유를 채워 반환한다. */
  private List<StoreSearchResult> finalizeResults(
      List<SearchHit<StoreDocument>> hits,
      GeoPoint userLocation,
      String rawIntent,
      StopWatch stopWatch,
      boolean generateReasons) {

    // TODO: 실제 픽업 완료 이력 조회로 교체 필요 (개인화 배지 계산용)
    Set<Long> completedPickupStoreIds = Collections.emptySet();

    stopWatch.start("랭킹/배지");
    List<StoreSearchResult> ranked =
        storeRankingService.rankAndAssignBadges(hits, userLocation, completedPickupStoreIds);
    List<StoreSearchResult> displayResults = ranked.stream().limit(DISPLAY_TOP_N).toList();
    stopWatch.stop();

    if (generateReasons) {
      // 상위 REASON_TOP_N개만 이유 생성 대상으로 넘김. 나머지는 reason=null로 그대로 노출됨
      stopWatch.start("RAG 추천 이유 생성");
      List<StoreSearchResult> reasonTargets = displayResults.stream().limit(REASON_TOP_N).toList();
      storeRecommendationReasonService.assignReasons(reasonTargets, rawIntent);
      stopWatch.stop();
    }

    log.info("검색 구간별 소요시간\n{}", stopWatch.prettyPrint());
    return displayResults;
  }

  private GeoPoint buildUserLocation(StoreSearchRequest request) {
    if (request.getLatitude() != null && request.getLongitude() != null) {
      return new GeoPoint(request.getLatitude(), request.getLongitude());
    }
    return null;
  }

  /** 결과 수와 ES 최고 점수가 모두 기준을 만족해야 LLM 폴백 없이 BM25 결과를 그대로 사용한다. */
  private boolean isQualitySufficient(List<SearchHit<StoreDocument>> hits) {
    if (hits == null || hits.size() < FAST_PASS_MIN_RESULT_COUNT) {
      return false;
    }
    double topScore = hits.get(0).getScore();
    return topScore >= FAST_PASS_MIN_TOP_SCORE;
  }

  /** 공백 기준 2단어 이하이며 가격/거리 등 필터성 표현이 없는 단순 키워드 쿼리인지 판별한다. */
  private boolean isSimpleKeyword(String query) {
    if (query.isBlank()) {
      return false;
    }
    String[] tokens = query.split("\\s+");
    return tokens.length <= 2 && !FILTER_KEYWORD_PATTERN.matcher(query).matches();
  }
}
