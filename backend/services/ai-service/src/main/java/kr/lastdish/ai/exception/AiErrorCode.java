package kr.lastdish.ai.exception;

import kr.lastdish.common.api.exception.ErrorCodeSpec;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AiErrorCode implements ErrorCodeSpec {
  NOT_FOOD(HttpStatus.BAD_REQUEST, "AI001", "음식 이미지가 아니거나 인식 신뢰도가 낮습니다."),
  TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "AI002", "분당 최대 요청 횟수를 초과했습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
