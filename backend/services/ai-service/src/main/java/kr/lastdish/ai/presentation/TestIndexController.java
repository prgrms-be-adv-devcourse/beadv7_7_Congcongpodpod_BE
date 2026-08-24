package kr.lastdish.ai.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import kr.lastdish.ai.application.StoreIndexerService;
import kr.lastdish.ai.presentation.dto.TestStoreIndexRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI Test Index API", description = "core-service 없이 ES에 테스트 데이터 색인")
@RestController
@RequestMapping("/api/v1/ai/index")
@RequiredArgsConstructor
public class TestIndexController {

  private final StoreIndexerService storeIndexerService;

  @Operation(summary = "테스트용 가게 데이터 직접 색인 (core-service 미연동)")
  @PostMapping("/test")
  public ResponseEntity<Void> indexTestStore(@RequestBody TestStoreIndexRequest request) {
    storeIndexerService.indexTestStore(request);
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "테스트용 가게 데이터 일괄 색인 (Bulk)")
  @PostMapping("/test/bulk")
  public ResponseEntity<String> indexTestStoresBulk(
      @RequestBody List<TestStoreIndexRequest> requests) {
    storeIndexerService.indexTestStoresBulk(requests);
    return ResponseEntity.ok(requests.size() + "건의 더미 데이터 색인이 완료되었습니다.");
  }
}
