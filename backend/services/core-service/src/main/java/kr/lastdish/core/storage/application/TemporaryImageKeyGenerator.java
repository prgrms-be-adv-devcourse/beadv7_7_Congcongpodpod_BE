package kr.lastdish.core.storage.application;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TemporaryImageKeyGenerator {

  public String generateDishKey(Long storeId, ImageContentType contentType) {
    return "tmp/dish/%d/%s.%s".formatted(storeId, UUID.randomUUID(), contentType.extension());
  }
}
