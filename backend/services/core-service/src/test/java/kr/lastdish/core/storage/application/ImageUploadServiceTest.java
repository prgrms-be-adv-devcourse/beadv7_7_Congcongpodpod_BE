package kr.lastdish.core.storage.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.core.common.exception.ErrorCode;
import kr.lastdish.core.storage.application.dto.PresignedUploadUrl;
import kr.lastdish.core.storage.infrastructure.S3StorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.unit.DataSize;

@ExtendWith(MockitoExtension.class)
class ImageUploadServiceTest {

  @Mock private PresignedUploadUrlProvider provider;
  @Mock private TemporaryImageKeyGenerator keyGenerator;

  private ImageUploadService imageUploadService;

  @BeforeEach
  void setUp() {
    S3StorageProperties properties =
        new S3StorageProperties(
            true,
            "test-bucket",
            "ap-northeast-2",
            Duration.ofMinutes(5),
            DataSize.ofMegabytes(10),
            null);
    imageUploadService = new ImageUploadService(Optional.of(provider), keyGenerator, properties);
  }

  @Test
  void 유효한_이미지는_MIME으로_확장자를_결정해_URL을_발급한다() throws Exception {
    String key = "tmp/dish/3/test-uuid.jpg";
    PresignedUploadUrl expected =
        new PresignedUploadUrl(
            key,
            URI.create("https://example.com/upload").toURL(),
            Map.of("Content-Type", "image/jpeg"),
            Instant.parse("2026-08-14T00:05:00Z"));
    when(keyGenerator.generateDishKey(3L, ImageContentType.JPEG)).thenReturn(key);
    when(provider.issue(key, "image/jpeg", 1024L)).thenReturn(expected);

    PresignedUploadUrl result =
        imageUploadService.issueDishUploadUrl(3L, "menu.not-really-png.png", 1024L);

    assertThat(result).isSameAs(expected);
    verify(provider).issue(key, "image/jpeg", 1024L);
  }

  @Test
  void 허용_크기를_초과하면_URL을_발급하지_않는다() {
    assertThatThrownBy(
            () ->
                imageUploadService.issueDishUploadUrl(
                    3L, "menu.jpg", DataSize.ofMegabytes(10).toBytes() + 1))
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_IMAGE_SIZE));

    verifyNoInteractions(provider, keyGenerator);
  }

  @Test
  void 지원하지_않는_MIME이면_URL을_발급하지_않는다() {
    assertThatThrownBy(
            () -> imageUploadService.issueDishUploadUrl(3L, "menu.gif", 1024L))
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNSUPPORTED_IMAGE_TYPE));

    verifyNoInteractions(provider, keyGenerator);
  }
}
