package kr.lastdish.core.storage.application;

import kr.lastdish.core.storage.application.dto.PresignedUploadUrl;

public interface PresignedUploadUrlProvider {

  PresignedUploadUrl issue(String objectKey, String contentType, long contentLength);
}
