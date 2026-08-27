package kr.lastdish.ai.foodclassify.exception;

import kr.lastdish.common.api.exception.ErrorCodeSpec;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AiErrorCode implements ErrorCodeSpec {
  NOT_FOOD(HttpStatus.BAD_REQUEST, "AI001", "음식 이미지가 아니거나 인식 신뢰도가 낮습니다."),
  TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "AI002", "분당 최대 요청 횟수를 초과했습니다."),
  TIMEOUT_ERROR(HttpStatus.GATEWAY_TIMEOUT, "AI003", "AI 서버 응답 시간 초과로 분석에 실패했습니다."),
  AI_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AI004", "AI 서버 연동 중 오류가 발생했습니다."),
  INVALID_IMAGE_FILE(HttpStatus.BAD_REQUEST, "AI005", "이미지 파일을 읽는 중 오류가 발생했습니다."),
  CORE_API_COMMUNICATION_ERROR(
      HttpStatus.INTERNAL_SERVER_ERROR, "AI006", "Core 서비스 연동 중 오류가 발생했습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
