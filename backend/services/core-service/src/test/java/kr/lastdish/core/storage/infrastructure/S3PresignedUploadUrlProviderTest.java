package kr.lastdish.core.storage.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import kr.lastdish.core.storage.application.dto.PresignedUploadUrl;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

class S3PresignedUploadUrlProviderTest {

  @Test
  void ContentType과_정확한_파일_크기를_서명에_포함한다() {
    Instant now = Instant.parse("2026-08-14T00:00:00Z");
    S3StorageProperties properties =
        new S3StorageProperties(
            true,
            "test-bucket",
            "ap-northeast-2",
            Duration.ofMinutes(5),
            DataSize.ofMegabytes(10),
            null);

    try (S3Presigner presigner =
        S3Presigner.builder()
            .region(Region.AP_NORTHEAST_2)
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create("access", "secret")))
            .build()) {
      S3PresignedUploadUrlProvider provider =
          new S3PresignedUploadUrlProvider(presigner, properties, Clock.fixed(now, ZoneOffset.UTC));

      PresignedUploadUrl result = provider.issue("tmp/dish/3/test.jpg", "image/jpeg", 1024L);

      assertThat(result.objectKey()).isEqualTo("tmp/dish/3/test.jpg");
      assertThat(result.requiredHeaders())
          .containsEntry("content-type", "image/jpeg")
          .containsEntry("content-length", "1024")
          .doesNotContainKey("host");
      assertThat(result.expiresAt()).isEqualTo(now.plus(Duration.ofMinutes(5)));
    }
  }
}
