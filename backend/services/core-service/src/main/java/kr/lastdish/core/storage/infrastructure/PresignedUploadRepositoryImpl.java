package kr.lastdish.core.storage.infrastructure;

import java.util.Optional;
import kr.lastdish.core.storage.domain.PresignedUpload;
import kr.lastdish.core.storage.domain.PresignedUploadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PresignedUploadRepositoryImpl implements PresignedUploadRepository {

  private final PresignedUploadJpaRepository presignedUploadJpaRepository;

  @Override
  public PresignedUpload save(PresignedUpload presignedUpload) {
    return presignedUploadJpaRepository.save(presignedUpload);
  }

  @Override
  public Optional<PresignedUpload> findByObjectKeyForUpdate(String objectKey) {
    return presignedUploadJpaRepository.findByObjectKeyForUpdate(objectKey);
  }
}
