package kr.lastdish.ai.elastic.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.lastdish.ai.elastic.application.StoreIndexerService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Profile("local")
@Tag(name = "AI Test Index API", description = "core-service 없이 ES에 테스트 데이터 색인")
@RestController
@RequestMapping("/api/v1/ai/index")
@RequiredArgsConstructor
public class TestIndexController {

  private final StoreIndexerService storeIndexerService;

  @Operation(summary = "지정 기간 동안 변경된 매장을 Core에서 조회해 색인 (운영 경로 재사용, 테스트용)")
  @PostMapping("/sync")
  public ResponseEntity<String> syncByRange(
      @RequestParam java.time.Instant from, @RequestParam java.time.Instant to) { // limit 추가

    storeIndexerService.syncUpdatedStores(from, to);
    return ResponseEntity.ok("동기화 완료 (from=" + from + ", to=" + to + ")");
  }

  @Operation(summary = "stores 인덱스가 없으면 StoreDocument 매핑 기준으로 생성 (테스트용)")
  @PostMapping("/init")
  public ResponseEntity<Void> initIndex() {
    storeIndexerService.ensureIndexExists();
    return ResponseEntity.ok().build();
  }
}
