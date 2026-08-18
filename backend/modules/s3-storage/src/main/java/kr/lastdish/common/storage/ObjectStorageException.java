package kr.lastdish.common.storage;

/** 객체 저장소 구현에서 발생한 SDK 오류를 공통 오류 사유로 변환해 전달하는 예외입니다. */
public class ObjectStorageException extends RuntimeException {

  private final Reason reason;

  public ObjectStorageException(Reason reason, Throwable cause) {
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
