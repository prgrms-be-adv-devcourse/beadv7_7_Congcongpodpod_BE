package kr.lastdish.common.storage.domain;

/** Presigned 업로드 이력의 발급 대기, 확정 완료, 만료 상태를 표현합니다. */
public enum UploadStatus {
  PENDING,
  CONFIRMED,
  EXPIRED
}
