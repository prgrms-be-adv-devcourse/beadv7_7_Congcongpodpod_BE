package kr.lastdish.common.storage.image;

public class UnsupportedImageContentTypeException extends IllegalArgumentException {

  public UnsupportedImageContentTypeException(String contentType) {
    super("Unsupported image content type: " + contentType);
  }
}
