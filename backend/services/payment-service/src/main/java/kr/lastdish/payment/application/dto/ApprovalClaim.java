package kr.lastdish.payment.application.dto;

import kr.lastdish.payment.domain.ApprovalClaimResult;
import kr.lastdish.payment.domain.Payment;

public record ApprovalClaim(ApprovalClaimResult result, Payment payment) {}
