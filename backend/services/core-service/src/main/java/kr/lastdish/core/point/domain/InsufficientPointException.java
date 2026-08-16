package kr.lastdish.core.point.domain;


public class InsufficientPointException extends RuntimeException {
    public InsufficientPointException(Long memberId, BigDecimal balance, BigDecimal amount) {
        super(String.format(
                "포인트 잔액이 부족합니다. memberId=%d, balance=%s, amount=%s",
                memberId, balance, amount));
    }
}