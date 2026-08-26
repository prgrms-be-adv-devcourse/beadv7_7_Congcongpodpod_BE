package kr.lastdish.ai.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import kr.lastdish.ai.application.StoreSearchFacade;
import kr.lastdish.ai.domain.document.StoreDocument;
import kr.lastdish.ai.presentation.dto.StoreSearchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI Search API", description = "자연어 기반 가게/메뉴 검색 도메인 API")
@RestController
@RequestMapping("/api/v1/ai/search")
@RequiredArgsConstructor
public class StoreSearchController {

  private final StoreSearchFacade storeSearchFacade;

  @Operation(summary = "자연어 쿼리 기반 하이브리드 검색")
  @PostMapping
  public ResponseEntity<List<StoreSearchController.StoreSearchResult>> search(
      @RequestBody StoreSearchRequest request) {

    List<SearchHit<StoreDocument>> hits = storeSearchFacade.search(request);

    List<StoreSearchResult> results =
        hits.stream().map(hit -> new StoreSearchResult(hit.getScore(), hit.getContent())).toList();

    return ResponseEntity.ok(results);
  }

  // ES SearchHit는 Jackson 기본 직렬화가 지저분해서, 테스트 편의를 위한 얇은 응답 래퍼
  public record StoreSearchResult(float score, StoreDocument store) {}
}
