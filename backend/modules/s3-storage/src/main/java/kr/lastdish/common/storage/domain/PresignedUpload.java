package kr.lastdish.common.storage.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Presigned PUT URL 발급 이력을 보관하는 엔티티입니다.
 *
 * <p>발급 회원, 리소스 종류, Object Key, 파일 조건, 만료 시각과 처리 상태를 저장해 실제 업로드 확정 시 검증에 사용합니다.
 */
@Getter
@Entity
@Table(name = "presigned_uploads")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PresignedUpload {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "member_id", nullable = false)
  private Long memberId;

  @Enumerated(EnumType.STRING)
  @Column(name = "resource_type", nullable = false, length = 30)
  private UploadResourceType resourceType;

  @Column(name = "object_key", nullable = false, unique = true, length = 512)
  private String objectKey;

  @Column(name = "content_type", nullable = false, length = 100)
  private String contentType;

  @Column(name = "content_length", nullable = false)
  private long contentLength;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private UploadStatus status;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  private PresignedUpload(
      Long memberId,
      UploadResourceType resourceType,
      String objectKey,
      String contentType,
      long contentLength,
      Instant expiresAt) {
    this.memberId = memberId;
    this.resourceType = resourceType;
    this.objectKey = objectKey;
    this.contentType = contentType;
    this.contentLength = contentLength;
    this.status = UploadStatus.PENDING;
    this.expiresAt = expiresAt;
    this.createdAt = Instant.now();
  }

  public static PresignedUpload issue(
      Long memberId,
      UploadResourceType resourceType,
      String objectKey,
      String contentType,
      long contentLength,
      Instant expiresAt) {
    return new PresignedUpload(
        memberId, resourceType, objectKey, contentType, contentLength, expiresAt);
  }

  public void confirm() {
    this.status = UploadStatus.CONFIRMED;
  }
}
