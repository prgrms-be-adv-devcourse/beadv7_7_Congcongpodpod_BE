package kr.lastdish.common.storage.infrastructure.s3;

/** S3 작업에서 발생한 SDK 오류를 저장소 오류 사유로 변환해 전달합니다. */
public class S3StorageException extends RuntimeException {

  private final Reason reason;

  public S3StorageException(Reason reason, Throwable cause) {
    super(cause);
    this.reason = reason;
  }

  public Reason getReason() {
    return reason;
  }

  public enum Reason {
    OBJECT_NOT_FOUND,
    OPERATION_FAILED
  }
}
