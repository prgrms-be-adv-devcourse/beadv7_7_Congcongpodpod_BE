package kr.lastdish.common.storage.upload.domain;

import java.util.Optional;

public interface PresignedUploadRepository {

  PresignedUpload save(PresignedUpload presignedUpload);

  Optional<PresignedUpload> findByObjectKeyForUpdate(String objectKey);
}
