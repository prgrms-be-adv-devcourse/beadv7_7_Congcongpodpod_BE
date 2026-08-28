package kr.lastdish.ai.elastic.infrastructure.llm;

import java.util.Map;
import kr.lastdish.ai.elastic.domain.model.DishCategories;
import kr.lastdish.ai.elastic.domain.model.ParsedSearchCondition;
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
public class LlmParsingService {

  private final ChatModel chatModel;

  public ParsedSearchCondition parseUserQuery(String userQuery) {
    if (userQuery == null || userQuery.isBlank()) {
      return ParsedSearchCondition.builder().rawIntent(userQuery).build();
    }

    // LLM의 텍스트 응답(JSON)을 Java 객체로 자동으로 매핑
    BeanOutputConverter<ParsedSearchCondition> converter =
        new BeanOutputConverter<>(ParsedSearchCondition.class);

    Prompt prompt = createPrompt(userQuery, converter);

    try {
      long callStart = System.currentTimeMillis();
      String responseText = chatModel.call(prompt).getResult().getOutput().getText();
      long callEnd = System.currentTimeMillis();
      log.info("=== OpenAI 실제 호출 시간: {} ms ===", callEnd - callStart);

      long convertStart = System.currentTimeMillis();
      ParsedSearchCondition result = converter.convert(responseText);
      long convertEnd = System.currentTimeMillis();
      log.info("=== JSON 컨버팅 시간: {} ms ===", convertEnd - convertStart);

      return result;
    } catch (Exception e) {
      log.error("사용자 쿼리 파싱 중 오류 발생: {}", userQuery, e);
      // 예외 발생 시 검색 기능 자체가 마비되지 않도록 fallback 처리
      return ParsedSearchCondition.builder().rawIntent(userQuery).build();
    }
  }

  /** PromptTemplate을 활용하여 안전하게 프롬프트를 생성하는 헬퍼 메서드 */
  private Prompt createPrompt(
      String userQuery, BeanOutputConverter<ParsedSearchCondition> converter) {
    String promptTemplateText =
        """
                            당신은 음식 검색 쿼리 해석기입니다.
                            사용자의 자연어 질문에서 검색 조건을 분석하여 추출해주세요.

                            규칙:
                            1. 명시되지 않은 조건은 null로 비워둡니다.
                            2. 가격(예산 상한선)은 숫자(원 단위)로 추출합니다. (예: "1만원 이하" -> 10000)
                            3. 거리는 km 단위의 숫자로 추출합니다. (예: "2km 근처" -> 2.0)
                               "도보 N분", "N분 거리"처럼 시간으로 표현된 경우에는 도보 속도를 시속 4km로
                               가정해 N * (4.0 / 60)으로 환산합니다. (예: "3분 거리" -> 0.2, "도보 10분" -> 0.67)
                               자전거/차량 등 다른 이동수단이 명시되면 이 가정을 적용하지 말고 null로 둡니다.
                            4. rawIntent에는 추출된 숫자나 단위를 제외한 순수 음식/메뉴/카테고리 관련 검색 키워드만 남깁니다.
                            5. pickupDeadline은 "몇 시까지 픽업하고 싶다/가능한 것"처럼 사용자가 픽업을
                               "완료하고 싶은 마지노선 시각"을 언급한 경우에만 HH:mm:ss 형식으로 추출합니다.
                               (예: "8시까지 픽업 가능한 거" -> "20:00:00", "저녁 7시 전에 찾으러 갈 수 있는 거" -> "19:00:00")
                               특정 픽업 시각을 명시하지 않았다면 null로 둡니다.
                            6. category는 아래 목록에 있는 값과 의미가 명확히 일치할 때만, 목록에 적힌 문자열 그대로
                               추출합니다. 목록에 없거나 어떤 카테고리인지 애매하면(예: 그냥 "빵") null로 둡니다.
                               목록: {categoryList}
                            7. isFoodRelated는 사용자 입력이 음식/메뉴/매장 검색과 조금이라도 관련 있으면
                            true, 날씨·잡담·감상 등 음식/매장 검색과 전혀 무관한 문장이면 false로 판단합니다.
                               가격/거리/픽업시각/카테고리 조건만 있고 구체적 메뉴명이 없어도, 그 조건 자체가
                               음식 주문 맥락이면 true입니다. 애매하면 true로 판단합니다(관대하게 판단).


                            사용자 입력: {userQuery}

                            {format}
                            """;

    PromptTemplate promptTemplate = new PromptTemplate(promptTemplateText);
    Prompt prompt =
        promptTemplate.create(
            Map.of(
                "userQuery", userQuery,
                "categoryList", String.join(", ", DishCategories.ALL),
                "format", converter.getFormat()));

    return prompt;
  }
}
