package kr.lastdish.core.dish.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.storage.PresignedUploadUrl;
import kr.lastdish.common.storage.upload.application.PresignedUploadService;
import kr.lastdish.common.storage.upload.domain.PresignedUploadException;
import kr.lastdish.common.storage.upload.domain.UploadResourceType;
import kr.lastdish.core.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DishImageUploadServiceTest {

  @Mock private PresignedUploadService presignedUploadService;

  private DishImageUploadService dishImageUploadService;

  @BeforeEach
  void setUp() {
    dishImageUploadService = new DishImageUploadService(presignedUploadService);
  }

  @Test
  void Dish용_prefix로_업로드_URL을_발급한다() throws Exception {
    PresignedUploadUrl expected =
        new PresignedUploadUrl(
            "tmp/dish/3/test.jpg",
            URI.create("https://example.com/upload").toURL(),
            Map.of(),
            Instant.parse("2026-08-14T00:05:00Z"));
    when(presignedUploadService.issue(
            7L, UploadResourceType.DISH, "tmp/dish/3/", "image/jpeg", 1024L))
        .thenReturn(expected);

    PresignedUploadUrl result = dishImageUploadService.issueUploadUrl(7L, 3L, "image/jpeg", 1024L);

    assertThat(result).isSameAs(expected);
  }

  @Test
  void 임시_key를_Dish_최종_key로_확정한다() {
    String temporaryKey = "tmp/dish/3/test.jpg";
    when(presignedUploadService.confirm(
            7L, UploadResourceType.DISH, temporaryKey, "dish/3/test.jpg"))
        .thenReturn("dish/3/test.jpg");

    String result = dishImageUploadService.confirmUpload(7L, 3L, temporaryKey);

    assertThat(result).isEqualTo("dish/3/test.jpg");
  }

  @Test
  void 다른_매장의_key는_공통_서비스에_전달하지_않는다() {
    assertThatThrownBy(() -> dishImageUploadService.confirmUpload(7L, 3L, "tmp/dish/4/test.jpg"))
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

    assertThatThrownBy(() -> dishImageUploadService.issueUploadUrl(7L, 3L, "image/gif", 1024L))
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNSUPPORTED_IMAGE_TYPE));

    verify(presignedUploadService)
        .issue(7L, UploadResourceType.DISH, "tmp/dish/3/", "image/gif", 1024L);
  }
}
