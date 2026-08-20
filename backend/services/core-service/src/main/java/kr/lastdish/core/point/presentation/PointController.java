package kr.lastdish.core.point.presentation;

import kr.lastdish.core.point.application.PointService;
import kr.lastdish.core.point.application.dto.PointBalanceResponse;
import lombok.RequiredArgsConstructor;
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

  @GetMapping("/balance")
  public ResponseEntity<PointBalanceResponse> getPointBalance(
      @RequestHeader("X-Authenticated-Member-Id") Long memberId) {

    PointBalanceResponse response = pointService.getPointBalance(memberId);
    return ResponseEntity.ok(response);
  }
}
