package kr.lastdish.common.storage.infrastructure.s3;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import kr.lastdish.common.storage.application.dto.PresignedDownloadUrl;
import kr.lastdish.common.storage.application.dto.PresignedUploadUrl;
import kr.lastdish.common.storage.application.dto.StoredObjectMetadata;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * AWS SDK의 S3Client와 S3Presigner를 이용해 객체 저장소 기능을 제공합니다.
 *
 * <p>URL 서명은 S3 네트워크 호출 없이 계산하며, 메타데이터 조회·복사·삭제는 실제 S3 API를 호출합니다.
 */
public class S3ObjectStorage {

  private final S3Client s3Client;
  private final S3Presigner presigner;
  private final S3StorageProperties properties;
  private final Clock clock;

  public S3ObjectStorage(S3Client s3Client, S3Presigner presigner, S3StorageProperties properties) {
    this(s3Client, presigner, properties, Clock.systemUTC());
  }

  S3ObjectStorage(
      S3Client s3Client, S3Presigner presigner, S3StorageProperties properties, Clock clock) {
    this.s3Client = s3Client;
    this.presigner = presigner;
    this.properties = properties;
    this.clock = clock;
  }

  public PresignedDownloadUrl issueGetUrl(String objectKey, Duration expiration) {
    try {
      GetObjectRequest getObjectRequest =
          GetObjectRequest.builder().bucket(properties.bucket()).key(objectKey).build();
      GetObjectPresignRequest presignRequest =
          GetObjectPresignRequest.builder()
              .signatureDuration(expiration)
              .getObjectRequest(getObjectRequest)
              .build();
      PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);

      return new PresignedDownloadUrl(
          objectKey, presignedRequest.url(), clock.instant().plus(expiration));
    } catch (SdkException exception) {
      throw new S3StorageException(S3StorageException.Reason.OPERATION_FAILED, exception);
    }
  }

  public PresignedUploadUrl issuePutUrl(String objectKey, Duration expiration) {
    try {
      PutObjectRequest putObjectRequest =
          PutObjectRequest.builder().bucket(properties.bucket()).key(objectKey).build();

      PutObjectPresignRequest presignRequest =
          PutObjectPresignRequest.builder()
              .signatureDuration(expiration)
              .putObjectRequest(putObjectRequest)
              .build();

      PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);
      Instant expiresAt = clock.instant().plus(expiration);

      return new PresignedUploadUrl(
          objectKey,
          presignedRequest.url(),
          presignedRequest.signedHeaders().entrySet().stream()
              .filter(entry -> !entry.getKey().equalsIgnoreCase("host"))
              .collect(
                  Collectors.toUnmodifiableMap(
                      Map.Entry::getKey, entry -> String.join(",", entry.getValue()))),
          expiresAt);
    } catch (SdkException exception) {
      throw new S3StorageException(S3StorageException.Reason.OPERATION_FAILED, exception);
    }
  }

  public StoredObjectMetadata getMetadata(String objectKey) {
    try {
      HeadObjectResponse response =
          s3Client.headObject(
              HeadObjectRequest.builder().bucket(properties.bucket()).key(objectKey).build());
      return new StoredObjectMetadata(response.contentType(), response.contentLength());
    } catch (S3Exception exception) {
      if (exception.statusCode() == 404) {
        throw new S3StorageException(S3StorageException.Reason.OBJECT_NOT_FOUND, exception);
      }
      throw new S3StorageException(S3StorageException.Reason.OPERATION_FAILED, exception);
    } catch (SdkException exception) {
      throw new S3StorageException(S3StorageException.Reason.OPERATION_FAILED, exception);
    }
  }

  public void copy(String sourceKey, String destinationKey) {
    try {
      s3Client.copyObject(
          CopyObjectRequest.builder()
              .copySource(properties.bucket() + "/" + sourceKey)
              .destinationBucket(properties.bucket())
              .destinationKey(destinationKey)
              .build());
    } catch (SdkException exception) {
      throw new S3StorageException(S3StorageException.Reason.OPERATION_FAILED, exception);
    }
  }

  public void delete(String objectKey) {
    try {
      s3Client.deleteObject(
          DeleteObjectRequest.builder().bucket(properties.bucket()).key(objectKey).build());
    } catch (SdkException exception) {
      throw new S3StorageException(S3StorageException.Reason.OPERATION_FAILED, exception);
    }
  }
}
