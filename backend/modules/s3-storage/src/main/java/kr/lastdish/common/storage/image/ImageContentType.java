package kr.lastdish.common.storage.image;

import java.util.Arrays;

/** 업로드를 허용하는 이미지 MIME 타입과 해당 Object Key 확장자를 정의합니다. */
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
