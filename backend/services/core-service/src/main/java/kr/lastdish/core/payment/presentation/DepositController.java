package kr.lastdish.core.payment.presentation;

import kr.lastdish.core.payment.application.DepositService;
import kr.lastdish.core.payment.application.DepositHistoryService;
import kr.lastdish.core.payment.application.dto.DepositBalanceResponse;
import kr.lastdish.core.payment.application.dto.DepositHistoryResponse;
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
@RequestMapping("/api/v1/deposits")
@RequiredArgsConstructor
public class DepositController {

  private final DepositService depositService;
  private final DepositHistoryService depositHistoryService;

  @GetMapping("/balance")
  public ResponseEntity<DepositBalanceResponse> getDepositBalance(
      @RequestHeader("X-Authenticated-Member-Id") Long memberId) {

    DepositBalanceResponse response = depositService.getDepositBalance(memberId);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/history")
  public ResponseEntity<Page<DepositHistoryResponse>> getHistory(
          @RequestHeader("X-Authenticated-Member-Id") Long memberId,
          @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    Page<DepositHistoryResponse> response = depositHistoryService.getHistory(memberId, pageable);
    return ResponseEntity.ok(response);
  }
}
