package kr.lastdish.core.dish.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.storage.ObjectStorage;
import kr.lastdish.common.storage.PresignedDownloadUrl;
import kr.lastdish.common.storage.PresignedUploadUrl;
import kr.lastdish.common.storage.download.application.PresignedDownloadService;
import kr.lastdish.common.storage.upload.application.PresignedUploadService;
import kr.lastdish.common.storage.upload.domain.PresignedUploadException;
import kr.lastdish.common.storage.upload.domain.UploadResourceType;
import kr.lastdish.core.common.exception.ErrorCode;
import kr.lastdish.core.dish.presentation.dto.DishResponse;
import kr.lastdish.core.store.application.StoreFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DishImageServiceTest {

  @Mock private StoreFacade storeFacade;
  @Mock private PresignedUploadService presignedUploadService;
  @Mock private PresignedDownloadService presignedDownloadService;
  @Mock private DishService dishService;
  @Mock private ObjectStorage objectStorage;

  private DishImageService dishImageService;

  @BeforeEach
  void setUp() {
    dishImageService =
        new DishImageService(
            storeFacade,
            presignedUploadService,
            presignedDownloadService,
            dishService,
            Optional.of(objectStorage));
  }

  @Test
  void SELLER이며_본인_매장이면_Dish용_업로드_URL을_발급한다() throws Exception {
    PresignedUploadUrl expected =
        new PresignedUploadUrl(
            "tmp/dish/3/test.jpg",
            URI.create("https://example.com/upload").toURL(),
            Map.of(),
            Instant.parse("2026-08-14T00:05:00Z"));
    when(presignedUploadService.issue(
            7L, UploadResourceType.DISH, "tmp/dish/3/", "image/jpeg", 1024L))
        .thenReturn(expected);

    PresignedUploadUrl result = dishImageService.issue(7L, "SELLER", 3L, "image/jpeg", 1024L);

    assertThat(result).isSameAs(expected);
    verify(storeFacade).validateStoreOwner(3L, 7L);
    verify(presignedUploadService)
        .issue(7L, UploadResourceType.DISH, "tmp/dish/3/", "image/jpeg", 1024L);
  }

  @Test
  void SELLER가_아니면_업로드_URL을_발급하지_않는다() {
    assertThatThrownBy(() -> dishImageService.issue(7L, "MEMBER", 3L, "image/jpeg", 1024L))
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception ->
                assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.IMAGE_UPLOAD_ACCESS_DENIED));

    verifyNoInteractions(storeFacade, presignedUploadService);
  }

  @Test
  void 임시_key를_Dish_최종_key로_확정한다() {
    String temporaryKey = "tmp/dish/3/test.jpg";
    when(presignedUploadService.confirm(
            7L, UploadResourceType.DISH, temporaryKey, "dish/3/test.jpg"))
        .thenReturn("dish/3/test.jpg");

    String result = dishImageService.confirmUpload(7L, 3L, temporaryKey);

    assertThat(result).isEqualTo("dish/3/test.jpg");
  }

  @Test
  void 다른_매장의_key는_업로드_확정에_사용할_수_없다() {
    assertThatThrownBy(() -> dishImageService.confirmUpload(7L, 3L, "tmp/dish/4/test.jpg"))
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception ->
                assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.IMAGE_UPLOAD_ACCESS_DENIED));

    verifyNoInteractions(presignedUploadService);
  }

  @Test
  void 공통_업로드_오류를_Core_오류로_변환한다() {
    when(presignedUploadService.issue(
            7L, UploadResourceType.DISH, "tmp/dish/3/", "image/gif", 1024L))
        .thenThrow(
            new PresignedUploadException(PresignedUploadException.Reason.UNSUPPORTED_CONTENT_TYPE));

    assertThatThrownBy(() -> dishImageService.issue(7L, "SELLER", 3L, "image/gif", 1024L))
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNSUPPORTED_IMAGE_TYPE));
  }

  @Test
  void Dish의_최종_key로_조회_URL을_발급한다() throws Exception {
    PresignedDownloadUrl expected =
        new PresignedDownloadUrl(
            "dish/3/test.jpg",
            URI.create("https://example.com/download").toURL(),
            Instant.parse("2026-08-14T00:05:00Z"));
    when(dishService.getImageKey(10L)).thenReturn("dish/3/test.jpg");
    when(presignedDownloadService.issue("dish/3/test.jpg")).thenReturn(expected);

    PresignedDownloadUrl result = dishImageService.issueDownloadUrl(10L);

    assertThat(result).isSameAs(expected);
    verify(dishService).getImageKey(10L);
    verify(presignedDownloadService).issue("dish/3/test.jpg");
  }

  @Test
  void Dish_조회_응답의_이미지_key를_조회_URL로_변환한다() throws Exception {
    DishResponse response =
        new DishResponse(
            10L,
            3L,
            "김치찌개",
            LocalDateTime.parse("2026-08-18T18:00:00"),
            "상품 설명",
            "한식",
            "dish/3/test.jpg",
            10L,
            "ON_SALE",
            BigDecimal.valueOf(10_000),
            BigDecimal.valueOf(7_000));
    when(presignedDownloadService.issue("dish/3/test.jpg"))
        .thenReturn(
            new PresignedDownloadUrl(
                "dish/3/test.jpg",
                URI.create("https://example.com/download").toURL(),
                Instant.parse("2026-08-14T00:05:00Z")));

    DishResponse result = dishImageService.withDownloadUrl(response);

    assertThat(result.thumbnailUrl()).isEqualTo("https://example.com/download");
    assertThat(result.dishId()).isEqualTo(response.dishId());
    assertThat(result.dishName()).isEqualTo(response.dishName());
  }

  @Test
  void Dish의_최종_이미지를_삭제한다() {
    dishImageService.deleteImage("dish/3/test.jpg");

    verify(objectStorage).delete("dish/3/test.jpg");
  }

  @Test
  void 임시_이미지는_Dish_삭제_대상으로_허용하지_않는다() {
    assertThatThrownBy(() -> dishImageService.deleteImage("tmp/dish/3/test.jpg"))
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.IMAGE_OBJECT_NOT_FOUND));

    verifyNoInteractions(objectStorage);
  }
}
