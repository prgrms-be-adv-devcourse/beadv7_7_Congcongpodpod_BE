package kr.lastdish.common.storage.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ImageContentTypeTest {

  @Test
  void MIME에_맞는_확장자를_반환한다() {
    assertThat(ImageContentType.from("image/jpeg").extension()).isEqualTo("jpg");
    assertThat(ImageContentType.from("IMAGE/PNG").extension()).isEqualTo("png");
    assertThat(ImageContentType.from("image/webp").extension()).isEqualTo("webp");
  }

  @Test
  void 지원하지_않는_MIME은_예외가_발생한다() {
    assertThatThrownBy(() -> ImageContentType.from("image/gif"))
        .isInstanceOf(UnsupportedImageContentTypeException.class);
  }
}
