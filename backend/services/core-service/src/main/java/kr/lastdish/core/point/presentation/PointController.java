package kr.lastdish.core.point.presentation;

import kr.lastdish.core.point.application.PointHistoryService;
import kr.lastdish.core.point.application.PointService;
import kr.lastdish.core.point.application.dto.PointBalanceResponse;
import kr.lastdish.core.point.application.dto.PointHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/points")
@RequiredArgsConstructor
public class PointController {

  private final PointService pointService;
  private final PointHistoryService pointHistoryService;

  @GetMapping("/balance")
  public ResponseEntity<PointBalanceResponse> getPointBalance(
      @RequestHeader("X-Authenticated-Member-Id") Long memberId) {

    PointBalanceResponse response = pointService.getPointBalance(memberId);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/history")
  public ResponseEntity<Page<PointHistoryResponse>> getHistory(
      @RequestHeader("X-Authenticated-Member-Id") Long memberId,
      @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    Page<PointHistoryResponse> response = pointHistoryService.getHistory(memberId, pageable);
    return ResponseEntity.ok(response);
  }
}
