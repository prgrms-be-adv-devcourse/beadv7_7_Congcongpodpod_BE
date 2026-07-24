package kr.lastdish.core.settlement.presentation;

import jakarta.validation.Valid;
import kr.lastdish.common.api.response.ApiResponse;
import kr.lastdish.core.settlement.application.SettlementBatchService;
import kr.lastdish.core.settlement.presentation.dto.MonthlySettlementJobRequest;
import kr.lastdish.core.settlement.presentation.dto.MonthlySettlementJobResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/settlement")
public class SettlementController {
    private final SettlementBatchService settlementBatchService;

    @PostMapping("/jobs")
    public ResponseEntity<MonthlySettlementJobResponse> runMonthlySettlement(
            @RequestBody
            MonthlySettlementJobRequest request
    ) {
        MonthlySettlementJobResponse response =
                settlementBatchService.runMonthlySettlement(
                        request.settlementMonth()
                );

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(response);
    }

    @GetMapping
    public ApiResponse<String> helloSettlement(){
        return ApiResponse.ok("정산 모듈입니다.");
    }
}
