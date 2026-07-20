package kr.lastdish.core.store.presentation;

import jakarta.validation.Valid;
import kr.lastdish.core.store.application.StoreService;
import kr.lastdish.core.store.application.dto.StoreResult;
import kr.lastdish.core.store.presentation.dto.StoreCreateRequest;
import kr.lastdish.core.store.presentation.dto.StoreResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stores")
@RequiredArgsConstructor
public class StoreController {

  private final StoreService storeService;

  @PostMapping
  public ResponseEntity<StoreResponse> registerStore(
      @RequestHeader("X-Member-Id") Long memberId, @Valid @RequestBody StoreCreateRequest request) {
    StoreResult result = storeService.register(request.toCommand(memberId));

    return ResponseEntity.status(HttpStatus.CREATED).body(StoreResponse.from(result));
  }
}
