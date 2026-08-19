package kr.lastdish.common.storage.domain;

import java.time.Duration;

/**
 * 애플리케이션이 특정 클라우드 SDK에 의존하지 않고 객체 저장소를 사용하기 위한 포트입니다.
 *
 * <p>Presigned URL 발급과 객체 메타데이터 조회, 복사, 삭제 기능을 정의합니다.
 */
public interface ObjectStorage {

  PresignedUploadUrl issuePutUrl(
      String objectKey, String contentType, long contentLength, Duration expiration);

  PresignedDownloadUrl issueGetUrl(String objectKey, Duration expiration);

  StoredObjectMetadata getMetadata(String objectKey);

  void copy(String sourceKey, String destinationKey);

  void delete(String objectKey);
}
