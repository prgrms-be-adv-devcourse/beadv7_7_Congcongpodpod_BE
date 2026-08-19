package kr.lastdish.common.storage.image;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ImageContentTypeTest {

  @Test
  void 지원하지_않는_MIME은_예외가_발생한다() {
    assertThatThrownBy(() -> ImageContentType.from("image/gif"))
        .isInstanceOf(UnsupportedImageContentTypeException.class);
  }
}
