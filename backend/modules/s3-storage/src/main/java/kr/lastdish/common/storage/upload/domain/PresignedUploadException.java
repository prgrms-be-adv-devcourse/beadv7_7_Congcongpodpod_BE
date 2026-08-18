package kr.lastdish.common.storage.upload.domain;

public class PresignedUploadException extends RuntimeException {

  private final Reason reason;

  public PresignedUploadException(Reason reason) {
    this.reason = reason;
  }

  public PresignedUploadException(Reason reason, Throwable cause) {
    super(cause);
    this.reason = reason;
  }

  public Reason getReason() {
    return reason;
  }

  public enum Reason {
    STORAGE_DISABLED,
    INVALID_FILE_SIZE,
    UNSUPPORTED_CONTENT_TYPE,
    UPLOAD_NOT_FOUND,
    ACCESS_DENIED,
    INVALID_STATE,
    METADATA_MISMATCH,
    OBJECT_NOT_FOUND,
    STORAGE_ERROR
  }
}
