package kr.lastdish.common.storage.upload.application;

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
import kr.lastdish.common.storage.ObjectStorage;
import kr.lastdish.common.storage.PresignedUploadUrl;
import kr.lastdish.common.storage.StoredObjectMetadata;
import kr.lastdish.common.storage.s3.S3StorageProperties;
import kr.lastdish.common.storage.upload.domain.PresignedUpload;
import kr.lastdish.common.storage.upload.domain.PresignedUploadException;
import kr.lastdish.common.storage.upload.domain.PresignedUploadRepository;
import kr.lastdish.common.storage.upload.domain.UploadResourceType;
import kr.lastdish.common.storage.upload.domain.UploadStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.unit.DataSize;

@ExtendWith(MockitoExtension.class)
class PresignedUploadServiceTest {

  @Mock private ObjectStorage objectStorage;
  @Mock private PresignedUploadRepository presignedUploadRepository;

  private PresignedUploadService presignedUploadService;

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
    presignedUploadService =
        new PresignedUploadService(
            Optional.of(objectStorage), properties, presignedUploadRepository);
  }

  @Test
  void MIME에_맞는_key와_URL을_발급하고_이력을_저장한다() throws Exception {
    Instant expiresAt = Instant.parse("2026-08-14T00:05:00Z");
    when(objectStorage.issuePutUrl(
            anyString(), eq("image/jpeg"), eq(1024L), eq(Duration.ofMinutes(5))))
        .thenAnswer(
            invocation ->
                new PresignedUploadUrl(
                    invocation.getArgument(0),
                    URI.create("https://example.com/upload").toURL(),
                    Map.of("Content-Type", "image/jpeg"),
                    expiresAt));

    PresignedUploadUrl result =
        presignedUploadService.issue(
            7L, UploadResourceType.DISH, "tmp/dish/3/", "image/jpeg", 1024L);

    assertThat(result.objectKey()).matches("tmp/dish/3/[0-9a-f-]{36}\\.jpg");
    ArgumentCaptor<PresignedUpload> uploadCaptor = ArgumentCaptor.forClass(PresignedUpload.class);
    verify(presignedUploadRepository).save(uploadCaptor.capture());
    PresignedUpload upload = uploadCaptor.getValue();
    assertThat(upload.getMemberId()).isEqualTo(7L);
    assertThat(upload.getResourceType()).isEqualTo(UploadResourceType.DISH);
    assertThat(upload.getObjectKey()).isEqualTo(result.objectKey());
    assertThat(upload.getContentType()).isEqualTo("image/jpeg");
    assertThat(upload.getContentLength()).isEqualTo(1024L);
    assertThat(upload.getStatus()).isEqualTo(UploadStatus.PENDING);
    assertThat(upload.getExpiresAt()).isEqualTo(expiresAt);
  }

  @Test
  void 허용_크기를_초과하면_URL을_발급하지_않는다() {
    assertThatThrownBy(
            () ->
                presignedUploadService.issue(
                    7L,
                    UploadResourceType.DISH,
                    "tmp/dish/3/",
                    "image/jpeg",
                    DataSize.ofMegabytes(10).toBytes() + 1))
        .isInstanceOfSatisfying(
            PresignedUploadException.class,
            exception ->
                assertThat(exception.getReason())
                    .isEqualTo(PresignedUploadException.Reason.INVALID_FILE_SIZE));

    verifyNoInteractions(objectStorage, presignedUploadRepository);
  }

  @Test
  void 업로드_이력과_객체를_검증한_뒤_복사하고_확정한다() {
    String temporaryKey = "tmp/dish/3/test.jpg";
    PresignedUpload upload =
        PresignedUpload.issue(
            7L,
            UploadResourceType.DISH,
            temporaryKey,
            "image/jpeg",
            1024L,
            Instant.parse("2026-08-14T00:05:00Z"));
    when(presignedUploadRepository.findByObjectKeyForUpdate(temporaryKey))
        .thenReturn(Optional.of(upload));
    when(objectStorage.getMetadata(temporaryKey))
        .thenReturn(new StoredObjectMetadata("image/jpeg", 1024L));

    String result =
        presignedUploadService.confirm(
            7L, UploadResourceType.DISH, temporaryKey, "dish/3/test.jpg");

    assertThat(result).isEqualTo("dish/3/test.jpg");
    assertThat(upload.getStatus()).isEqualTo(UploadStatus.CONFIRMED);
    verify(objectStorage).copy(temporaryKey, "dish/3/test.jpg");
    verify(presignedUploadRepository).save(upload);
  }

  @Test
  void 객체의_MIME이나_크기가_다르면_확정하지_않는다() {
    String temporaryKey = "tmp/dish/3/test.jpg";
    PresignedUpload upload =
        PresignedUpload.issue(
            7L,
            UploadResourceType.DISH,
            temporaryKey,
            "image/jpeg",
            1024L,
            Instant.parse("2026-08-14T00:05:00Z"));
    when(presignedUploadRepository.findByObjectKeyForUpdate(temporaryKey))
        .thenReturn(Optional.of(upload));
    when(objectStorage.getMetadata(temporaryKey))
        .thenReturn(new StoredObjectMetadata("image/png", 2048L));

    assertThatThrownBy(
            () ->
                presignedUploadService.confirm(
                    7L, UploadResourceType.DISH, temporaryKey, "dish/3/test.jpg"))
        .isInstanceOfSatisfying(
            PresignedUploadException.class,
            exception ->
                assertThat(exception.getReason())
                    .isEqualTo(PresignedUploadException.Reason.METADATA_MISMATCH));

    assertThat(upload.getStatus()).isEqualTo(UploadStatus.PENDING);
  }
}
