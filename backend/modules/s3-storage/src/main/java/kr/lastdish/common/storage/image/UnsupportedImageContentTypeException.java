package kr.lastdish.common.storage.image;

/** 요청한 MIME 타입이 {@link ImageContentType}의 허용 목록에 없을 때 발생합니다. */
public class UnsupportedImageContentTypeException extends IllegalArgumentException {

  public UnsupportedImageContentTypeException(String contentType) {
    super("Unsupported image content type: " + contentType);
  }
}
