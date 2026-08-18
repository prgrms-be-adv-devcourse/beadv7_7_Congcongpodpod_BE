package kr.lastdish.common.storage.upload.infrastructure;

import java.util.Optional;
import kr.lastdish.common.storage.upload.domain.PresignedUpload;
import kr.lastdish.common.storage.upload.domain.PresignedUploadRepository;

public class PresignedUploadRepositoryImpl implements PresignedUploadRepository {

  private final PresignedUploadJpaRepository presignedUploadJpaRepository;

  public PresignedUploadRepositoryImpl(PresignedUploadJpaRepository presignedUploadJpaRepository) {
    this.presignedUploadJpaRepository = presignedUploadJpaRepository;
  }

  @Override
  public PresignedUpload save(PresignedUpload presignedUpload) {
    return presignedUploadJpaRepository.save(presignedUpload);
  }

  @Override
  public Optional<PresignedUpload> findByObjectKeyForUpdate(String objectKey) {
    return presignedUploadJpaRepository.findByObjectKeyForUpdate(objectKey);
  }
}
