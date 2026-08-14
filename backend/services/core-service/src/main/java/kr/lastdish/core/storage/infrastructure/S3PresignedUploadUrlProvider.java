package kr.lastdish.core.storage.infrastructure;

import java.time.Clock;
import java.time.Instant;
import kr.lastdish.core.storage.application.PresignedUploadUrlProvider;
import kr.lastdish.core.storage.application.dto.PresignedUploadUrl;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

public class S3PresignedUploadUrlProvider implements PresignedUploadUrlProvider {

  private final S3Presigner presigner;
  private final S3StorageProperties properties;
  private final Clock clock;

  public S3PresignedUploadUrlProvider(S3Presigner presigner, S3StorageProperties properties) {
    this(presigner, properties, Clock.systemUTC());
  }

  S3PresignedUploadUrlProvider(S3Presigner presigner, S3StorageProperties properties, Clock clock) {
    this.presigner = presigner;
    this.properties = properties;
    this.clock = clock;
  }

  @Override
  public PresignedUploadUrl issue(String objectKey, String contentType, long contentLength) {
    PutObjectRequest putObjectRequest =
        PutObjectRequest.builder()
            .bucket(properties.bucket())
            .key(objectKey)
            .contentType(contentType)
            .contentLength(contentLength)
            .build();

    PutObjectPresignRequest presignRequest =
        PutObjectPresignRequest.builder()
            .signatureDuration(properties.presignedUrlExpiration())
            .putObjectRequest(putObjectRequest)
            .build();

    PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);
    Instant expiresAt = clock.instant().plus(properties.presignedUrlExpiration());

    return new PresignedUploadUrl(
        objectKey,
        presignedRequest.url(),
        presignedRequest.signedHeaders().entrySet().stream()
            .filter(entry -> !entry.getKey().equalsIgnoreCase("host"))
            .collect(
                java.util.stream.Collectors.toUnmodifiableMap(
                    java.util.Map.Entry::getKey, entry -> String.join(",", entry.getValue()))),
        expiresAt);
  }
}
