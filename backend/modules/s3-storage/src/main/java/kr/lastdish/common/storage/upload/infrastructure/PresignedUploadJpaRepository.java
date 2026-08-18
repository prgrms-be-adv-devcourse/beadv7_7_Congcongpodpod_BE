package kr.lastdish.common.storage.upload.infrastructure;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import kr.lastdish.common.storage.upload.domain.PresignedUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** PresignedUpload 엔티티의 영속화와 Object Key 기준 비관적 잠금 조회를 수행합니다. */
public interface PresignedUploadJpaRepository extends JpaRepository<PresignedUpload, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select upload from PresignedUpload upload where upload.objectKey = :objectKey")
  Optional<PresignedUpload> findByObjectKeyForUpdate(@Param("objectKey") String objectKey);
}
