package kr.lastdish.core.order.application;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

// 6자리 숫자 픽업 코드 생성

@Component
public class PickupCodeGenerator {

  private static final SecureRandom RANDOM = new SecureRandom();

  public String generate() {
    return String.valueOf(100000 + RANDOM.nextInt(900000));
  }
}
