package kr.lastdish.core.point.domain.event;

public record MemberRewardPayload(
    Long memberId,
    String type,
    String title,
    String body,
    String data,
    String linkTarget,
    Long linkId) {}
