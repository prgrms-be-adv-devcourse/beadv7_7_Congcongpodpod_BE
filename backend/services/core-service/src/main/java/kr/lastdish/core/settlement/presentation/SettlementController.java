package kr.lastdish.core.settlement.presentation;

import kr.lastdish.common.api.response.ApiResponse;
import kr.lastdish.core.settlement.application.SettlementBatchService;
import kr.lastdish.core.settlement.application.SettlementService;
import kr.lastdish.core.settlement.presentation.dto.MonthlySettlementJobRequest;
import kr.lastdish.core.settlement.presentation.dto.MonthlySettlementJobResponse;
import kr.lastdish.core.settlement.presentation.dto.SettlementDetailResponse;
import kr.lastdish.core.settlement.presentation.dto.SettlementResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/settlements")
public class SettlementController {
  private final SettlementBatchService settlementBatchService;
  private final SettlementService settlementService;

  // 스케쥴러 전환 시 제거
  @PostMapping("/jobs")
  public ApiResponse<MonthlySettlementJobResponse> runMonthlySettlement(
      @RequestBody MonthlySettlementJobRequest request) {
    MonthlySettlementJobResponse response =
        settlementBatchService.runMonthlySettlement(request.settlementMonth());

    return ApiResponse.ok(response);
  }

  @GetMapping
  public ApiResponse<Page<SettlementResponse>> getSettlements(
      @RequestHeader("X-Authenticated-Member-Id") Long memberId,
      @RequestHeader("X-Authenticated-Role") String role,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    Pageable pageable = PageRequest.of(page, size);

    return ApiResponse.ok(settlementService.getSettlements(memberId, role, pageable));
  }

  @GetMapping("/{settlementId}")
  public ApiResponse<SettlementDetailResponse> getSettlement(
      @PathVariable Long settlementId,
      @RequestHeader("X-Authenticated-Member-Id") Long memberId,
      @RequestHeader("X-Authenticated-Role") String role) {
    return ApiResponse.ok(settlementService.getSettlement(memberId, role, settlementId));
  }
}
