package kr.lastdish.ai.elastic.infrastructure.llm;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import kr.lastdish.ai.elastic.infrastructure.llm.dto.RecommendationReasonResponse;
import kr.lastdish.ai.elastic.infrastructure.llm.dto.StoreReasonPromptItem;
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
    String dishName = store.cheapestDish() != null ? store.cheapestDish().dishName() : null;

    return new StoreReasonPromptItem(
        store.storeId(),
        store.storeName(),
        dishName,
        result.getScoreBreakdown() != null ? result.getScoreBreakdown().getDistanceScore() : null,
        result.getScoreBreakdown() != null
            ? result.getScoreBreakdown().getDiscountRateScore()
            : null,
        result.getBadges());
  }

  private Prompt createPrompt(
      String rawIntent,
      List<StoreReasonPromptItem> items,
      BeanOutputConverter<RecommendationReasonResponse> converter) {
    String templateText =
        """
                    당신은 음식 배달 앱의 추천 이유 작성 도우미입니다.
                    사용자가 "{rawIntent}"라는 의도로 검색했고, 아래는 이미 순위가 정해진 상위 매장 목록입니다.

                    각 매장에 대해, 왜 이 매장이 추천 목록에 올랐는지 한국어로 한 문장씩 자연스럽게 설명해주세요.

                    규칙:
                    1. 반드시 주어진 정보(배지, 거리, 할인율)에 근거해서만 작성합니다. 없는 사실을 지어내지 마세요.
                    2. 각 문장은 20자 내외로 짧고 친근하게 작성합니다.
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

  private String buildDefaultReason(StoreSearchResult result) {
    List<String> badges = result.getBadges();
    if (badges == null || badges.isEmpty()) {
      return "조건에 맞는 매장이에요.";
    }
    return switch (badges.get(0)) {
      case "가장 가까움" -> "가장 가까운 위치에 있는 매장이에요.";
      case "할인율 최고" -> "할인율이 가장 높은 매장이에요.";
      case "취향 맞음" -> "이전에 이용하신 취향과 잘 맞아요.";
      case "뜻으로 찾음" -> "검색하신 의도와 의미적으로 잘 맞아요.";
      default -> "조건에 맞는 매장이에요.";
    };
  }
}
