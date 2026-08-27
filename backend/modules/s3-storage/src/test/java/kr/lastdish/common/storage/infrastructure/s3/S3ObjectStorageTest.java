package kr.lastdish.common.storage.infrastructure.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import kr.lastdish.common.storage.application.dto.PresignedDownloadUrl;
import kr.lastdish.common.storage.application.dto.PresignedUploadUrl;
import kr.lastdish.common.storage.application.dto.StoredObjectMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

class S3ObjectStorageTest {

  private final S3StorageProperties properties =
      new S3StorageProperties(
          true,
          "test-bucket",
          "ap-northeast-2",
          Duration.ofMinutes(5),
          DataSize.ofMegabytes(10),
          null);

  @Test
  void 조회용_GET_URL을_발급한다() {
    Instant now = Instant.parse("2026-08-14T00:00:00Z");

    try (S3Presigner presigner =
        S3Presigner.builder()
            .region(Region.AP_NORTHEAST_2)
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create("access", "secret")))
            .build()) {
      S3ObjectStorage storage =
          new S3ObjectStorage(
              mock(S3Client.class), presigner, properties, Clock.fixed(now, ZoneOffset.UTC));

      PresignedDownloadUrl result = storage.issueGetUrl("dish/3/test.jpg", Duration.ofMinutes(5));

      assertThat(result.objectKey()).isEqualTo("dish/3/test.jpg");
      assertThat(result.url().getPath()).endsWith("/dish/3/test.jpg");
      assertThat(result.expiresAt()).isEqualTo(now.plus(Duration.ofMinutes(5)));
    }
  }

  @Test
  void ContentType만_필수_업로드_헤더로_서명한다() {
    Instant now = Instant.parse("2026-08-14T00:00:00Z");

    try (S3Presigner presigner =
        S3Presigner.builder()
            .region(Region.AP_NORTHEAST_2)
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create("access", "secret")))
            .build()) {
      S3ObjectStorage storage =
          new S3ObjectStorage(
              mock(S3Client.class), presigner, properties, Clock.fixed(now, ZoneOffset.UTC));

      PresignedUploadUrl result =
          storage.issuePutUrl("tmp/dish/3/test.jpg", "image/jpeg", Duration.ofMinutes(5));

      assertThat(result.objectKey()).isEqualTo("tmp/dish/3/test.jpg");
      assertThat(result.requiredHeaders())
          .containsEntry("content-type", "image/jpeg")
          .doesNotContainKeys("content-length", "host");
      assertThat(result.expiresAt()).isEqualTo(now.plus(Duration.ofMinutes(5)));
    }
  }

  @Test
  void 객체_메타데이터를_조회한다() {
    S3Client client = mock(S3Client.class);
    when(client.headObject(any(HeadObjectRequest.class)))
        .thenReturn(
            HeadObjectResponse.builder().contentType("image/jpeg").contentLength(1024L).build());
    S3ObjectStorage storage = new S3ObjectStorage(client, mock(S3Presigner.class), properties);

    StoredObjectMetadata result = storage.getMetadata("tmp/dish/3/test.jpg");

    assertThat(result).isEqualTo(new StoredObjectMetadata("image/jpeg", 1024L));
  }

  @Test
  void 객체를_같은_bucket의_다른_key로_복사한다() {
    S3Client client = mock(S3Client.class);
    S3ObjectStorage storage = new S3ObjectStorage(client, mock(S3Presigner.class), properties);

    storage.copy("tmp/dish/3/test.jpg", "dish/3/test.jpg");

    verify(client)
        .copyObject(
            org.mockito.ArgumentMatchers.argThat(
                (CopyObjectRequest request) ->
                    request.copySource().equals("test-bucket/tmp/dish/3/test.jpg")
                        && request.destinationBucket().equals("test-bucket")
                        && request.destinationKey().equals("dish/3/test.jpg")));
  }

  @Test
  void 객체를_삭제한다() {
    S3Client client = mock(S3Client.class);
    S3ObjectStorage storage = new S3ObjectStorage(client, mock(S3Presigner.class), properties);

    storage.delete("dish/3/test.jpg");

    verify(client)
        .deleteObject(
            org.mockito.ArgumentMatchers.argThat(
                (DeleteObjectRequest request) ->
                    request.bucket().equals("test-bucket")
                        && request.key().equals("dish/3/test.jpg")));
  }

  @Test
  void 없는_객체는_NOT_FOUND로_변환한다() {
    S3Client client = mock(S3Client.class);
    when(client.headObject(any(HeadObjectRequest.class)))
        .thenThrow(S3Exception.builder().statusCode(404).message("not found").build());
    S3ObjectStorage storage = new S3ObjectStorage(client, mock(S3Presigner.class), properties);

    assertThatThrownBy(() -> storage.getMetadata("missing.jpg"))
        .isInstanceOfSatisfying(
            S3StorageException.class,
            exception ->
                assertThat(exception.getReason())
                    .isEqualTo(S3StorageException.Reason.OBJECT_NOT_FOUND));
  }
}
