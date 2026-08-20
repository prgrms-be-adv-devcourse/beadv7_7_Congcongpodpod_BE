package kr.lastdish.core.level.presentation;

import kr.lastdish.core.level.application.LevelService;
import kr.lastdish.core.level.application.dto.LevelResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/levels")
@RequiredArgsConstructor
public class LevelController {

  private final LevelService levelService;

  @GetMapping("/info")
  public ResponseEntity<LevelResponse> getLevelInfo(
      @RequestHeader("X-Authenticated-Member-Id") Long memberId) {

    LevelResponse response = levelService.getLevel(memberId);
    return ResponseEntity.ok(response);
  }
}
