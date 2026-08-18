package kr.lastdish.common.storage.s3;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import kr.lastdish.common.storage.ObjectStorage;
import kr.lastdish.common.storage.ObjectStorageException;
import kr.lastdish.common.storage.PresignedUploadUrl;
import kr.lastdish.common.storage.StoredObjectMetadata;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

public class S3ObjectStorage implements ObjectStorage {

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

  @Override
  public PresignedUploadUrl issuePutUrl(
      String objectKey, String contentType, long contentLength, Duration expiration) {
    try {
      PutObjectRequest putObjectRequest =
          PutObjectRequest.builder()
              .bucket(properties.bucket())
              .key(objectKey)
              .contentType(contentType)
              .contentLength(contentLength)
              .build();

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
      throw new ObjectStorageException(ObjectStorageException.Reason.OPERATION_FAILED, exception);
    }
  }

  @Override
  public StoredObjectMetadata getMetadata(String objectKey) {
    try {
      HeadObjectResponse response =
          s3Client.headObject(
              HeadObjectRequest.builder().bucket(properties.bucket()).key(objectKey).build());
      return new StoredObjectMetadata(response.contentType(), response.contentLength());
    } catch (S3Exception exception) {
      if (exception.statusCode() == 404) {
        throw new ObjectStorageException(ObjectStorageException.Reason.OBJECT_NOT_FOUND, exception);
      }
      throw new ObjectStorageException(ObjectStorageException.Reason.OPERATION_FAILED, exception);
    } catch (SdkException exception) {
      throw new ObjectStorageException(ObjectStorageException.Reason.OPERATION_FAILED, exception);
    }
  }

  @Override
  public void copy(String sourceKey, String destinationKey) {
    try {
      s3Client.copyObject(
          CopyObjectRequest.builder()
              .copySource(properties.bucket() + "/" + sourceKey)
              .destinationBucket(properties.bucket())
              .destinationKey(destinationKey)
              .build());
    } catch (SdkException exception) {
      throw new ObjectStorageException(ObjectStorageException.Reason.OPERATION_FAILED, exception);
    }
  }
}
