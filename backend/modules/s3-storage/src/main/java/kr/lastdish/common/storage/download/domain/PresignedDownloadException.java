package kr.lastdish.common.storage.download.domain;

/** 조회 URL 발급 기능의 비활성화 또는 객체 저장소 오류를 호출 도메인에 전달하는 예외입니다. */
public class PresignedDownloadException extends RuntimeException {

  private final Reason reason;

  public PresignedDownloadException(Reason reason) {
    this.reason = reason;
  }

  public PresignedDownloadException(Reason reason, Throwable cause) {
    super(cause);
    this.reason = reason;
  }

  public Reason getReason() {
    return reason;
  }

  public enum Reason {
    STORAGE_DISABLED,
    STORAGE_ERROR
  }
}
