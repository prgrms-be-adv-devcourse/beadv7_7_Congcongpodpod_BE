package kr.lastdish.common.storage.image;

import java.util.Arrays;

public enum ImageContentType {
  JPEG("image/jpeg", "jpg"),
  PNG("image/png", "png"),
  WEBP("image/webp", "webp");

  private final String mediaType;
  private final String extension;

  ImageContentType(String mediaType, String extension) {
    this.mediaType = mediaType;
    this.extension = extension;
  }

  public static ImageContentType from(String mediaType) {
    return Arrays.stream(values())
        .filter(type -> type.mediaType.equalsIgnoreCase(mediaType))
        .findFirst()
        .orElseThrow(() -> new UnsupportedImageContentTypeException(mediaType));
  }

  public String mediaType() {
    return mediaType;
  }

  public String extension() {
    return extension;
  }
}
