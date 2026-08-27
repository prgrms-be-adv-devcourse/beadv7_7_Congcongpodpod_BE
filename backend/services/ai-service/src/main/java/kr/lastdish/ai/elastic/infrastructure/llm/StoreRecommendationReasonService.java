package kr.lastdish.ai.elastic.infrastructure.llm;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import kr.lastdish.ai.elastic.infrastructure.llm.dto.RecommendationReasonResponse;
import kr.lastdish.ai.elastic.infrastructure.llm.dto.StoreReasonPromptItem;
import kr.lastdish.ai.elastic.presentation.dto.StoreResponse;
import kr.lastdish.ai.elastic.presentation.dto.StoreSearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreRecommendationReasonService {

  private static final long TIMEOUT_MS = 3000;

  // 폴백 문장에서 배지 하나당 붙일 짧은 근거 조각. 여러 배지가 있으면 자연스럽게 이어붙인다.
  private static final Map<String, String> BADGE_PHRASES =
      Map.of(
          "가장 가까움", "가장 가까운 위치",
          "최저가", "가장 저렴한 가격",
          "마감임박", "말씀하신 시간 안에 마감 임박",
          "가게명 일치도 높음", "찾으시는 가게 이름과 잘 어울림",
          "찾으시는 메뉴와 유사해요", "찾으시는 메뉴와 유사한 메뉴",
          "설명이 잘 맞아요", "메뉴 설명이 취향과 잘 맞음");

  private final ChatModel chatModel;
  private final ExecutorService recommendationReasonExecutor;

  /** 상위 랭킹 결과에 대해 RAG 기반 추천 이유를 생성해 채워 넣는다. LLM 실패/타임아웃 시 배지 기반 기본 문장으로 대체한다 (검색 자체는 절대 막지 않음). */
  public void assignReasons(List<StoreSearchResult> topResults, String rawIntent) {
    if (topResults == null || topResults.isEmpty()) {
      return;
    }

    Map<Long, String> generatedReasons = generateWithTimeout(topResults, rawIntent);

    for (StoreSearchResult result : topResults) {
      Long storeId = result.getStore().storeId();
      String reason = generatedReasons.get(storeId);
      result.setReason(reason != null ? reason : buildDefaultReason(result));
    }
  }

  private Map<Long, String> generateWithTimeout(
      List<StoreSearchResult> topResults, String rawIntent) {
    CompletableFuture<Map<Long, String>> future =
        CompletableFuture.supplyAsync(
            () -> generateReasonsViaLlm(topResults, rawIntent), recommendationReasonExecutor);

    try {
      return future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      log.warn("추천 이유 생성 타임아웃({}ms) - 배지 기반 기본값으로 대체", TIMEOUT_MS);
      future.cancel(true); // 응답 대기를 포기하고 스레드 정리, 완전한 kill은 아니지만 리소스 반환 시도
      return Map.of();
    } catch (Exception e) {
      log.error("추천 이유 생성 실패 - 배지 기반 기본값으로 대체", e);
      return Map.of();
    }
  }

  private Map<Long, String> generateReasonsViaLlm(
      List<StoreSearchResult> topResults, String rawIntent) {
    BeanOutputConverter<RecommendationReasonResponse> converter =
        new BeanOutputConverter<>(RecommendationReasonResponse.class);

    List<StoreReasonPromptItem> items = topResults.stream().map(this::toPromptItem).toList();

    Prompt prompt = createPrompt(rawIntent, items, converter);
    String responseText = chatModel.call(prompt).getResult().getOutput().getText();
    RecommendationReasonResponse parsed = converter.convert(responseText);

    if (parsed == null || parsed.reasons() == null) {
      return Map.of();
    }

    return parsed.reasons().stream()
        .collect(
            Collectors.toMap(
                RecommendationReasonResponse.StoreReason::storeId,
                RecommendationReasonResponse.StoreReason::reason,
                (a, b) -> a));
  }

  private StoreReasonPromptItem toPromptItem(StoreSearchResult result) {
    var store = result.getStore();
    var cheapestDish = store.cheapestDish();
    String dishName = cheapestDish != null ? cheapestDish.dishName() : null;
    var breakdown = result.getScoreBreakdown();

    return new StoreReasonPromptItem(
        store.storeId(),
        store.storeName(),
        dishName,
        breakdown != null ? breakdown.getRawDistanceKm() : null,
        calculateDiscountRate(cheapestDish),
        breakdown != null ? breakdown.getStoreNameSimScore() : null,
        breakdown != null ? breakdown.getDishNameSimScore() : null,
        breakdown != null ? breakdown.getDescriptionSimScore() : null,
        result.getBadges());
  }

  /** 정가 대비 할인율(%)을 계산한다. 가격 정보가 없거나 정가가 0이면 null. */
  private Double calculateDiscountRate(StoreResponse.CheapestDishResponse dish) {
    if (dish == null || dish.dishPrice() == null || dish.discountPrice() == null) {
      return null;
    }
    BigDecimal dishPrice = dish.dishPrice();
    if (dishPrice.compareTo(BigDecimal.ZERO) <= 0) {
      return null;
    }
    return dishPrice
        .subtract(dish.discountPrice())
        .divide(dishPrice, 4, RoundingMode.HALF_UP)
        .multiply(BigDecimal.valueOf(100))
        .doubleValue();
  }

  private Prompt createPrompt(
      String rawIntent,
      List<StoreReasonPromptItem> items,
      BeanOutputConverter<RecommendationReasonResponse> converter) {
    // 프롬프트는 짧게 유지하되, "왜 이 매장인지"가 각 아이템의 구체적인 수치(거리 km, 할인율 %,
    // 필드별 유사도)에 근거하도록만 핵심 규칙 몇 줄로 압축했다.
    String templateText =
        """
                            당신은 음식 배달 앱의 추천 이유 작성 도우미입니다.
                            사용자가 "{rawIntent}"라는 의도로 검색했고, 아래는 순위가 정해진 상위 매장 목록입니다.

                            각 매장마다, 이 매장이 왜 이 순위/추천 목록에 올랐는지 한국어 한 문장으로 설명하세요.

                            규칙:
                            1. distanceKm(실제 거리), discountRate(실제 할인율%), storeNameSimScore/
                               dishNameSimScore/descriptionSimScore(0~1, 0.7 이상이면 그 항목이 검색어와
                               잘 맞음), badges 중 실제로 값이 있는 근거만 골라 씁니다. 없는 사실은 지어내지 마세요.
                            2. "왜 이 순위인지"가 드러나도록 그 매장에서 가장 두드러진 근거 1~2개를 구체적
                               수치와 함께 짧게 씁니다(예: "1.2km로 가장 가까워요", "설명이 취향과 잘 맞아요").
                            3. storeId는 입력값을 그대로 사용합니다.

                            매장 목록: {items}

                            {format}
                            """;

    PromptTemplate promptTemplate = new PromptTemplate(templateText);
    return promptTemplate.create(
        java.util.Map.of(
            "rawIntent", rawIntent == null ? "" : rawIntent,
            "items", items.toString(),
            "format", converter.getFormat()));
  }

  /** LLM 실패/타임아웃 시 배지 기반으로 문장을 조립한다. 배지가 여러 개면 전부 이어붙인다. */
  private String buildDefaultReason(StoreSearchResult result) {
    List<String> badges = result.getBadges();
    if (badges == null || badges.isEmpty()) {
      return "조건에 맞는 매장이에요.";
    }

    List<String> phrases = new ArrayList<>();
    for (String badge : badges) {
      String phrase = BADGE_PHRASES.get(badge);
      if (phrase != null) {
        phrases.add(phrase);
      }
    }

    if (phrases.isEmpty()) {
      return "조건에 맞는 매장이에요.";
    }
    if (phrases.size() == 1) {
      return phrases.get(0) + " 매장이에요.";
    }

    String joined =
        String.join(", ", phrases.subList(0, phrases.size() - 1))
            + "에 "
            + phrases.get(phrases.size() - 1);
    return joined + "까지 갖춘 매장이에요.";
  }
}
