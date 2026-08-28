package kr.lastdish.ai.elastic.application;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
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
  private static final double MIN_DISPLAY_SCORE = 0.24;
  private static final int REASON_TOP_N = 5; // RAG 추천수

  // ===== 투-패스 폴백 판단 기준 =====
  // BM25 Fast-Pass 결과가 아래 조건을 만족하지 못하면 LLM 파싱 + kNN 벡터 검색으로 폴백
  private static final int FAST_PASS_MIN_RESULT_COUNT = 5;
  private static final double FAST_PASS_MIN_TOP_SCORE = 2.0;

  // 단순 키워드 판별
  private static final Pattern FILTER_KEYWORD_PATTERN =
      Pattern.compile(".*(\\d+원|이하|이상|근처|주변|할인).*");
  private static final Pattern WALLET_BALANCE_PATTERN =
      Pattern.compile("(내\\s*잔액|잔액으로|잔액\\s*내|예치금|포인트|지갑|가진\\s*돈|남은\\s*돈|보유\\s*금액|가진\\s*포인트)");
  private static final Pattern PICKUP_DEADLINE_PATTERN =
      Pattern.compile("(오전|오후|아침|저녁|밤)?\\s*(\\d{1,2})시\\s*(\\d{1,2}\\s*분)?\\s*(까지|전에|이내)");

  // Fast-Pass/단순 키워드 경로는 LLM을 타지 않으므로, LLM 없이도 판별 가능한 카테고리·픽업시간만
  private static final List<Map.Entry<String, String>> CATEGORY_ALIASES =
      Stream.of(
              Map.entry("디저트 빵", "디저트 빵"),
              Map.entry("식사빵", "식사빵"),
              Map.entry("케이크", "케이크"),
              Map.entry("디저트", "디저트"),
              Map.entry("샐러드", "샐러드"),
              Map.entry("샌드위치", "샌드위치"),
              Map.entry("밥류", "밥류"),
              Map.entry("카페", "음료 / 카페"),
              Map.entry("음료", "음료 / 카페"),
              Map.entry("과일류", "과일류"),
              Map.entry("유제품", "유제품"))
          .sorted(
              Comparator.comparingInt((Map.Entry<String, String> e) -> e.getKey().length())
                  .reversed())
          .toList();

  private final LlmParsingService llmParsingService;
  private final EmbeddingService embeddingService;
  private final SearchService searchService;
  private final StoreRankingService storeRankingService;
  private final StoreRecommendationReasonService storeRecommendationReasonService;

  /** 사용자 자연어 쿼리를 해석하고 하이브리드 검색을 실행 */
  public List<StoreSearchResult> search(StoreSearchRequest request) {
    StopWatch stopWatch = new StopWatch();
    String rawQuery = request.getQuery() == null ? "" : request.getQuery().trim();
    GeoPoint userLocation = buildUserLocation(request);

    // 0. 단순 키워드 입력 -> LLM 파싱/임베딩을 전면 스킵하고 BM25 키워드 검색만 즉시 수행
    if (isSimpleKeyword(rawQuery)) {
      log.info("단순 키워드 판별(query=\"{}\") - LLM/임베딩 스킵, BM25 전용 검색 수행", rawQuery);
      return searchByKeywordOnly(rawQuery, request, userLocation, stopWatch);
    }

    // 1. BM25 Fast-Pass: 원문 그대로 키워드/멀티매치 검색만 먼저 수행
    //    카테고리/픽업시간은 LLM 없이 로컬 추출로 채워서 필터에 반영
    stopWatch.start("1. BM25 Fast-Pass");
    ParsedSearchCondition fastCond =
        applyWalletBalanceIfRequested(
            buildLocalCondition(rawQuery, request.getRadiusKm()),
            rawQuery,
            request.getWalletBalance());
    List<SearchHit<StoreDocument>> fastHits =
        searchService.searchStoresAndDishes(fastCond, userLocation, null);
    stopWatch.stop();

    // 2. 품질 검사: 결과 수/최고 점수가 기준을 만족하면 LLM 폴백 없이 즉시 반환
    if (isQualitySufficient(fastHits)) {
      log.info(
          "BM25 Fast-Pass 품질 충족(count={}, topScore={}) - LLM 폴백 없이 반환",
          fastHits.size(),
          fastHits.isEmpty() ? 0.0 : fastHits.get(0).getScore());
      return finalizeResults(
          fastHits, userLocation, fastCond, stopWatch, true, null, request.getWalletBalance());
    }

    log.info("BM25 Fast-Pass 품질 미달(count={}) - LLM 파싱 + 벡터 검색으로 폴백", fastHits.size());

    // 3. Fallback: 기존 LLM 파싱 + 하이브리드(BM25+kNN) 검색 수행
    return executeFullLlmSearch(request, userLocation, stopWatch);
  }

  /** 단순 키워드(2단어 이하, 필터 표현 없음) 전용 경로: BM25 멀티매치 검색만 수행하고 즉시 반환 */
  private List<StoreSearchResult> searchByKeywordOnly(
      String rawQuery, StoreSearchRequest request, GeoPoint userLocation, StopWatch stopWatch) {

    stopWatch.start("0. 단순 키워드 BM25 전용 검색");
    ParsedSearchCondition simpleCond =
        applyWalletBalanceIfRequested(
            buildLocalCondition(rawQuery, request.getRadiusKm()),
            rawQuery,
            request.getWalletBalance());
    List<SearchHit<StoreDocument>> hits =
        searchService.searchStoresAndDishes(simpleCond, userLocation, null);
    stopWatch.stop();

    // 단순 키워드 경로는 지연시간 최소화가 목적이므로 RAG 추천 이유 생성은 생략
    return finalizeResults(
        hits, userLocation, simpleCond, stopWatch, false, null, request.getWalletBalance());
  }

  /** LLM 없이 카테고리/픽업시간을 로컬로 추출해서 채운 검색 조건을 생성 */
  private ParsedSearchCondition buildLocalCondition(String rawQuery, Double radiusKm) {
    return ParsedSearchCondition.builder()
        .maxDistanceKm(radiusKm)
        .rawIntent(rawQuery)
        .category(detectCategory(rawQuery))
        .pickupDeadline(detectPickupDeadline(rawQuery))
        .build();
  }

  /** 알려진 카테고리 별칭이 쿼리에 포함돼 있으면 색인된 카테고리 값을 반환 길이가 긴(구체적인) 별칭을 먼저 검사한다. */
  private String detectCategory(String query) {
    if (query == null || query.isBlank()) {
      return null;
    }
    for (Map.Entry<String, String> alias : CATEGORY_ALIASES) {
      if (query.contains(alias.getKey())) {
        return alias.getValue();
      }
    }
    return null;
  }

  /** "N시까지/전에/이내" 형태의 마지노선 픽업 시각을 추출한다. 오전/오후 표기가 없으면 픽업 특성상 오후로 간주 */
  private LocalTime detectPickupDeadline(String query) {
    if (query == null || query.isBlank()) {
      return null;
    }

    Matcher matcher = PICKUP_DEADLINE_PATTERN.matcher(query);
    if (!matcher.find()) {
      return null;
    }

    String meridiem = matcher.group(1);
    int hour = Integer.parseInt(matcher.group(2));
    String minuteGroup = matcher.group(3);
    int minute = minuteGroup != null ? Integer.parseInt(minuteGroup.replaceAll("\\D", "")) : 0;

    if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
      return null;
    }

    if (hour >= 1 && hour <= 12) {
      boolean isMorning = "오전".equals(meridiem) || "아침".equals(meridiem);
      if (hour == 12) {
        hour = isMorning ? 0 : 12;
      } else if (!isMorning) {
        // 오전 표기가 명시적으로 없으면 오후로 간주
        hour += 12;
      }
    }

    try {
      return LocalTime.of(hour, minute);
    } catch (DateTimeException e) {
      return null;
    }
  }

  /** 쿼리에 잔액/예치금을 언급했고, 이미 명시적 금액을 말하지 않았다면 현재 잔액을 예산 상한선으로 채움 */
  private ParsedSearchCondition applyWalletBalanceIfRequested(
      ParsedSearchCondition cond, String rawQuery, java.math.BigDecimal walletBalance) {
    if (cond.maxPrice() != null || walletBalance == null) {
      return cond;
    }
    if (rawQuery == null || !WALLET_BALANCE_PATTERN.matcher(rawQuery).find()) {
      return cond;
    }
    return new ParsedSearchCondition(
        walletBalance,
        cond.maxDistanceKm(),
        cond.pickupDeadline(),
        cond.category(),
        cond.rawIntent(),
        cond.isFoodRelated());
  }

  /** 전체 파이프라인: LLM 조건 파싱 -> 임베딩 -> 하이브리드(BM25+kNN) 검색 -> 랭킹 -> RAG 추천 이유. */
  private List<StoreSearchResult> executeFullLlmSearch(
      StoreSearchRequest request, GeoPoint userLocation, StopWatch stopWatch) {

    // 1. LLM 조건 파싱
    stopWatch.start("2. LLM 파싱");
    ParsedSearchCondition cond = llmParsingService.parseUserQuery(request.getQuery());
    stopWatch.stop();

    if (Boolean.FALSE.equals(cond.isFoodRelated())) {
      log.info("LLM이 음식/매장 검색과 무관하다고 판단(query=\"{}\") - 빈 결과 반환", request.getQuery());
      return List.of();
    }

    // 2. 거리 반경 Fallback 처리
    Double effectiveDistance =
        (cond.maxDistanceKm() != null) ? cond.maxDistanceKm() : request.getRadiusKm();

    ParsedSearchCondition finalCond =
        applyWalletBalanceIfRequested(
            new ParsedSearchCondition(
                cond.maxPrice(),
                effectiveDistance,
                cond.pickupDeadline(),
                cond.category(),
                cond.rawIntent(),
                cond.isFoodRelated()),
            request.getQuery(),
            request.getWalletBalance());

    // 3. 키워드 임베딩 벡터화
    stopWatch.start("3. 임베딩");
    List<Float> queryVector = embeddingService.getEmbeddingList(finalCond.rawIntent());
    stopWatch.stop();

    // 4. 하이브리드 검색 실행
    stopWatch.start("4. ES 검색");
    List<SearchHit<StoreDocument>> hits =
        searchService.searchStoresAndDishes(finalCond, userLocation, queryVector);
    stopWatch.stop();
    return finalizeResults(
        hits, userLocation, finalCond, stopWatch, true, queryVector, request.getWalletBalance());
  }

  /** 랭킹/배지 부여 후, 필요 시 상위 N개에 대해 RAG 추천 이유를 채워 반환 */
  private List<StoreSearchResult> finalizeResults(
      List<SearchHit<StoreDocument>> hits,
      GeoPoint userLocation,
      ParsedSearchCondition cond,
      StopWatch stopWatch,
      boolean generateReasons,
      List<Float> queryVector,
      BigDecimal walletBalance) {

    boolean deadlineRequested = cond.pickupDeadline() != null;

    stopWatch.start("랭킹/배지");
    List<StoreSearchResult> ranked =
        storeRankingService.rankAndAssignBadges(
            hits, userLocation, deadlineRequested, queryVector, walletBalance);
    List<StoreSearchResult> displayResults =
        ranked.stream()
            .filter(r -> r.getTotalScore() >= MIN_DISPLAY_SCORE)
            .limit(DISPLAY_TOP_N)
            .toList();
    stopWatch.stop();

    if (generateReasons) {
      stopWatch.start("RAG 추천 이유 생성");
      List<StoreSearchResult> reasonTargets = displayResults.stream().limit(REASON_TOP_N).toList();
      storeRecommendationReasonService.assignReasons(reasonTargets, cond.rawIntent());
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

  /** 결과 수와 ES 최고 점수가 모두 기준을 만족해야 LLM 폴백 없이 BM25 결과를 그대로 사용 */
  private boolean isQualitySufficient(List<SearchHit<StoreDocument>> hits) {
    if (hits == null || hits.size() < FAST_PASS_MIN_RESULT_COUNT) {
      return false;
    }
    double topScore = hits.get(0).getScore();
    return topScore >= FAST_PASS_MIN_TOP_SCORE;
  }

  /** 공백 기준 2단어 이하이며 가격/거리 등 필터성 표현이 없는 단순 키워드 쿼리인지 판별 */
  private boolean isSimpleKeyword(String query) {
    if (query.isBlank()) {
      return false;
    }
    String[] tokens = query.split("\\s+");
    return tokens.length <= 2 && !FILTER_KEYWORD_PATTERN.matcher(query).matches();
  }
}
