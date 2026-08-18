package kr.lastdish.core.dish.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.core.common.exception.ErrorCode;
import kr.lastdish.core.storage.application.ImageUploadService;
import kr.lastdish.core.storage.application.dto.PresignedUploadUrl;
import kr.lastdish.core.store.application.StoreFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DishImageServiceTest {

  @Mock private StoreFacade storeFacade;
  @Mock private ImageUploadService imageUploadService;

  private DishImageService facade;

  @BeforeEach
  void setUp() {
    facade = new DishImageService(storeFacade, imageUploadService);
  }

  @Test
  void SELLER이며_본인_매장이면_소유권_검증_후_URL을_발급한다() throws Exception {
    PresignedUploadUrl expected =
        new PresignedUploadUrl(
            "tmp/dish/3/test.jpg",
            URI.create("https://example.com/upload").toURL(),
            Map.of(),
            Instant.parse("2026-08-14T00:05:00Z"));
    when(imageUploadService.issueDishUploadUrl(7L, 3L, "image/jpeg", 1024L)).thenReturn(expected);

    facade.issue(7L, "SELLER", 3L, "image/jpeg", 1024L);

    verify(storeFacade).validateStoreOwner(3L, 7L);
    verify(imageUploadService).issueDishUploadUrl(7L, 3L, "image/jpeg", 1024L);
  }

  @Test
  void SELLER가_아니면_소유권_조회도_하지_않는다() {
    assertThatThrownBy(() -> facade.issue(7L, "MEMBER", 3L, "image/jpeg", 1024L))
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception ->
                org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.IMAGE_UPLOAD_ACCESS_DENIED));

    verifyNoInteractions(storeFacade, imageUploadService);
  }
}
