package kr.lastdish.ai.elastic.infrastructure.llm;

import java.util.Map;
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
                    4. rawIntent에는 추출된 숫자나 단위를 제외한 순수 음식/메뉴/카테고리 관련 검색 키워드만 남깁니다.

                    사용자 입력: {userQuery}

                    {format}
                    """;

    PromptTemplate promptTemplate = new PromptTemplate(promptTemplateText);
    Prompt prompt =
        promptTemplate.create(Map.of("userQuery", userQuery, "format", converter.getFormat()));

    // 임시 디버깅 로그 - 확인 후 삭제
    String fullText = prompt.getContents();
    log.info("=== LLM 프롬프트 길이: {} chars ===\n{}\n=== 프롬프트 끝 ===", fullText.length(), fullText);

    return prompt;
  }
}
