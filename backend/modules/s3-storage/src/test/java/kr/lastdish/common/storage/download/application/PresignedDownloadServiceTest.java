package kr.lastdish.common.storage.download.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import kr.lastdish.common.storage.ObjectStorage;
import kr.lastdish.common.storage.PresignedDownloadUrl;
import kr.lastdish.common.storage.download.domain.PresignedDownloadException;
import kr.lastdish.common.storage.s3.S3StorageProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.unit.DataSize;

@ExtendWith(MockitoExtension.class)
class PresignedDownloadServiceTest {

  @Mock private ObjectStorage objectStorage;

  @Test
  void 설정된_만료시간으로_조회_URL을_발급한다() throws Exception {
    S3StorageProperties properties = properties();
    PresignedDownloadService service =
        new PresignedDownloadService(Optional.of(objectStorage), properties);
    PresignedDownloadUrl expected =
        new PresignedDownloadUrl(
            "dish/3/test.jpg",
            URI.create("https://example.com/download").toURL(),
            Instant.parse("2026-08-14T00:05:00Z"));
    when(objectStorage.issueGetUrl("dish/3/test.jpg", Duration.ofMinutes(5))).thenReturn(expected);

    PresignedDownloadUrl result = service.issue("dish/3/test.jpg");

    assertThat(result).isSameAs(expected);
    verify(objectStorage).issueGetUrl("dish/3/test.jpg", Duration.ofMinutes(5));
  }

  @Test
  void 저장소가_비활성화되면_명확한_오류를_반환한다() {
    PresignedDownloadService service = new PresignedDownloadService(Optional.empty(), properties());

    assertThatThrownBy(() -> service.issue("dish/3/test.jpg"))
        .isInstanceOfSatisfying(
            PresignedDownloadException.class,
            exception ->
                assertThat(exception.getReason())
                    .isEqualTo(PresignedDownloadException.Reason.STORAGE_DISABLED));
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
