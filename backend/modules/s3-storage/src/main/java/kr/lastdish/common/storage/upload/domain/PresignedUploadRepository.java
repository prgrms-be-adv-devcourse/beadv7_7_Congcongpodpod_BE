package kr.lastdish.common.storage.upload.domain;

import java.util.Optional;

/** Presigned 업로드 이력 저장과 동시 확정 방지를 위한 잠금 조회를 정의하는 도메인 저장소입니다. */
public interface PresignedUploadRepository {

  PresignedUpload save(PresignedUpload presignedUpload);

  Optional<PresignedUpload> findByObjectKeyForUpdate(String objectKey);
}
