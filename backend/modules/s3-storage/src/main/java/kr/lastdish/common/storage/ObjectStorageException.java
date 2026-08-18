package kr.lastdish.common.storage;

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
