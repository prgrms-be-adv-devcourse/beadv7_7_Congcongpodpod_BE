package kr.lastdish.common.storage.download.domain;

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
