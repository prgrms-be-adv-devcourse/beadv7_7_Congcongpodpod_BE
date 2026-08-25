package kr.lastdish.ai.elastic.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import kr.lastdish.ai.elastic.application.StoreSearchFacade;
import kr.lastdish.ai.elastic.presentation.dto.StoreSearchRequest;
import kr.lastdish.ai.elastic.presentation.dto.StoreSearchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI Search API", description = "자연어 기반 가게/메뉴 검색 도메인 API")
@RestController
@RequestMapping("/api/v1/ai/search")
@RequiredArgsConstructor
public class StoreSearchController {

  private final StoreSearchFacade storeSearchFacade;

  @Operation(summary = "자연어 쿼리 기반 하이브리드 검색 (배지 + 상위 5개 RAG 추천 이유 포함)")
  @PostMapping
  public ResponseEntity<List<StoreSearchResult>> search(@RequestBody StoreSearchRequest request) {
    return ResponseEntity.ok(storeSearchFacade.search(request));
  }
}
