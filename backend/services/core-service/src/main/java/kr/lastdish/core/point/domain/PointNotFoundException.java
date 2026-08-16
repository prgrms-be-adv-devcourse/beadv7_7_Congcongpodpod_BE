package kr.lastdish.core.point.domain;

public class PointNotFoundException extends RuntimeException {
    public PointNotFoundException(Long memberId) {
        super("포인트 정보를 찾을 수 없습니다. memberId=" + memberId);
    }
}