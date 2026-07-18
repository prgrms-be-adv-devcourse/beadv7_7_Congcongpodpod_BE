package kr.lastdish.core.payment.presentation;

import kr.lastdish.core.payment.application.DepositService;
import kr.lastdish.core.payment.application.dto.DepositBalanceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/deposits")
@RequiredArgsConstructor
public class DepositController {

    private final DepositService depositService;

    @GetMapping("/balance")
    public ResponseEntity<DepositBalanceResponse> getDepositBalance(
            @RequestHeader("X-Authenticated-Member-Id") Long memberId
    ) {

        DepositBalanceResponse response = depositService.getDepositBalance(memberId);

        return ResponseEntity.ok(response);

}
