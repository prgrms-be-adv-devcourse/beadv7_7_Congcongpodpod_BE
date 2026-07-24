package kr.lastdish.core.settlement.presentation;

import kr.lastdish.common.api.response.ApiResponse;
import kr.lastdish.core.settlement.application.SettlementBatchService;
import kr.lastdish.core.settlement.presentation.dto.MonthlySettlementJobRequest;
import kr.lastdish.core.settlement.presentation.dto.MonthlySettlementJobResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/settlements")
public class SettlementController {
  private final SettlementBatchService settlementBatchService;

  @PostMapping("/jobs")
  public ApiResponse<MonthlySettlementJobResponse> runMonthlySettlement(
      @RequestBody MonthlySettlementJobRequest request) {
    MonthlySettlementJobResponse response =
        settlementBatchService.runMonthlySettlement(request.settlementMonth());

    return ApiResponse.ok(response);
  }

  @GetMapping
  public ApiResponse<String> helloSettlement() {
    return ApiResponse.ok("정산 모듈입니다.");
  }
}
