package kr.lastdish.core.storage.domain;

import java.util.Optional;

public interface PresignedUploadRepository {

  PresignedUpload save(PresignedUpload presignedUpload);

  Optional<PresignedUpload> findByObjectKeyForUpdate(String objectKey);
}
