package kr.lastdish.core.storage.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import kr.lastdish.core.storage.domain.PresignedUpload;
import kr.lastdish.core.storage.domain.PresignedUploadRepository;
import kr.lastdish.core.storage.domain.UploadResourceType;
import kr.lastdish.core.storage.domain.UploadStatus;
import kr.lastdish.core.storage.infrastructure.S3PresignedUploadUrlProvider;
import kr.lastdish.core.storage.infrastructure.S3StorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.unit.DataSize;

@ExtendWith(MockitoExtension.class)
class ImageUploadServiceTest {

  @Mock private S3PresignedUploadUrlProvider provider;
  @Mock private PresignedUploadRepository presignedUploadRepository;

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
    imageUploadService =
        new ImageUploadService(Optional.of(provider), properties, presignedUploadRepository);
  }

  @Test
  void 유효한_이미지는_MIME으로_확장자를_결정해_URL을_발급한다() throws Exception {
    Instant expiresAt = Instant.parse("2026-08-14T00:05:00Z");
    when(provider.issue(anyString(), eq("image/jpeg"), eq(1024L)))
        .thenAnswer(
            invocation ->
                new PresignedUploadUrl(
                    invocation.getArgument(0),
                    URI.create("https://example.com/upload").toURL(),
                    Map.of("Content-Type", "image/jpeg"),
                    expiresAt));

    PresignedUploadUrl result = imageUploadService.issueDishUploadUrl(7L, 3L, "image/jpeg", 1024L);

    assertThat(result.objectKey()).matches("tmp/dish/3/[0-9a-f-]{36}\\.jpg");
    verify(provider).issue(result.objectKey(), "image/jpeg", 1024L);
    ArgumentCaptor<PresignedUpload> issuanceCaptor = ArgumentCaptor.forClass(PresignedUpload.class);
    verify(presignedUploadRepository).save(issuanceCaptor.capture());
    PresignedUpload issuance = issuanceCaptor.getValue();
    assertThat(issuance.getMemberId()).isEqualTo(7L);
    assertThat(issuance.getResourceType()).isEqualTo(UploadResourceType.DISH);
    assertThat(issuance.getObjectKey()).isEqualTo(result.objectKey());
    assertThat(issuance.getContentType()).isEqualTo("image/jpeg");
    assertThat(issuance.getContentLength()).isEqualTo(1024L);
    assertThat(issuance.getStatus()).isEqualTo(UploadStatus.PENDING);
    assertThat(issuance.getExpiresAt()).isEqualTo(expiresAt);
    assertThat(issuance.getCreatedAt()).isNotNull();
  }

  @Test
  void 허용_크기를_초과하면_URL을_발급하지_않는다() {
    assertThatThrownBy(
            () ->
                imageUploadService.issueDishUploadUrl(
                    7L, 3L, "image/jpeg", DataSize.ofMegabytes(10).toBytes() + 1))
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_IMAGE_SIZE));

    verifyNoInteractions(provider, presignedUploadRepository);
  }

  @Test
  void 지원하지_않는_MIME이면_URL을_발급하지_않는다() {
    assertThatThrownBy(() -> imageUploadService.issueDishUploadUrl(7L, 3L, "image/gif", 1024L))
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNSUPPORTED_IMAGE_TYPE));

    verifyNoInteractions(provider, presignedUploadRepository);
  }
}
