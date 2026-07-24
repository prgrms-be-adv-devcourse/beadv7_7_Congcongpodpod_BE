package kr.lastdish.core.settlement.domain;

//import kr.lastdish.core.common.exception.BusinessException;
//import kr.lastdish.core.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class SettlementCalculator {
    public static final BigDecimal DEFAULT_FEE_RATE = new BigDecimal("0.1000");

    //주문 건당 수수료 계산
    public long calculateFeeAmount(
            long salesAmount,
            BigDecimal feeRate
    ){
        validateSalesAmount(salesAmount);
        validateFeeRate(feeRate);

        return BigDecimal.valueOf(salesAmount)
                .multiply(feeRate)
                .setScale(0, RoundingMode.DOWN)
                .longValueExact();
    }

    //주문 건당 정산액 계산 (주문 금액 - 계산된 수수료)
    public long calculateSettlementAmount(
            long salesAmount,
            long feeAmount
    ){
        validateSalesAmount(salesAmount);

//        if (feeAmount < 0) {
//            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "수수료는 음수일 수 없습니다.");
//        }
//
//        if (feeAmount > salesAmount) {
//            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "수수료는 판매 금액보다 클 수 없습니다.");
//        }

        return salesAmount - feeAmount;
    }

    private void validateSalesAmount(long salesAmount){
//        if (salesAmount < 0) {
//            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "판매 금액은 음수일 수 없습니다.");
//        }
    }

    private void validateFeeRate(BigDecimal feeRate){
//        if (feeRate == null){
//            throw new BusinessException(ErrorCode.INVALID_INPUT, "수수료율은 필수입니다.");
//        }
//
//        if (feeRate.compareTo(BigDecimal.ZERO) < 0
//                || feeRate.compareTo(BigDecimal.ONE) > 0) {
//            throw new BusinessException(ErrorCode.INVALID_INPUT, "수수료율은 0 이상 1 이하여야 합니다.");
//        }
    }
}
