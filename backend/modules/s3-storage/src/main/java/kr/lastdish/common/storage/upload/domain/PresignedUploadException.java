package kr.lastdish.common.storage.upload.domain;

/** 업로드 URL 발급·확정 과정의 검증 실패와 객체 저장소 오류를 사유별로 전달하는 예외입니다. */
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
