package kr.lastdish.common.storage.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import kr.lastdish.common.storage.application.dto.PresignedDownloadUrl;
import kr.lastdish.common.storage.domain.PresignedUploadRepository;
import kr.lastdish.common.storage.domain.PresignedUrlException;
import kr.lastdish.common.storage.infrastructure.s3.S3StorageProperties;
import kr.lastdish.common.storage.infrastructure.s3.S3ObjectStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.unit.DataSize;

@ExtendWith(MockitoExtension.class)
class PresignedUrlServiceDownloadTest {

  @Mock private S3ObjectStorage s3ObjectStorage;
  @Mock private PresignedUploadRepository presignedUploadRepository;

  @Test
  void 설정된_만료시간으로_조회_URL을_발급한다() throws Exception {
    S3StorageProperties properties = properties();
    PresignedUrlService service =
        new PresignedUrlService(
            Optional.of(s3ObjectStorage), properties, presignedUploadRepository);
    PresignedDownloadUrl expected =
        new PresignedDownloadUrl(
            "dish/3/test.jpg",
            URI.create("https://example.com/download").toURL(),
            Instant.parse("2026-08-14T00:05:00Z"));
    when(s3ObjectStorage.issueGetUrl("dish/3/test.jpg", Duration.ofMinutes(5))).thenReturn(expected);

    PresignedDownloadUrl result = service.issueDownload("dish/3/test.jpg");

    assertThat(result).isSameAs(expected);
    verify(s3ObjectStorage).issueGetUrl("dish/3/test.jpg", Duration.ofMinutes(5));
  }

  @Test
  void 저장소가_비활성화되면_명확한_오류를_반환한다() {
    PresignedUrlService service =
        new PresignedUrlService(Optional.empty(), properties(), presignedUploadRepository);

    assertThatThrownBy(() -> service.issueDownload("dish/3/test.jpg"))
        .isInstanceOfSatisfying(
            PresignedUrlException.class,
            exception ->
                assertThat(exception.getReason())
                    .isEqualTo(PresignedUrlException.Reason.STORAGE_DISABLED));
  }

  private S3StorageProperties properties() {
    return new S3StorageProperties(
        true,
        "test-bucket",
        "ap-northeast-2",
        Duration.ofMinutes(5),
        DataSize.ofMegabytes(10),
        null);
  }
}
