package kr.lastdish.payment.domain;

public enum ApprovedStatus {
    READY, // 결제 준비 (결제 위젯 호출 전/후 대기)
    APPROVED,
    FAILED
}