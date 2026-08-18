package kr.lastdish.core.storage.infrastructure;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import kr.lastdish.core.storage.domain.PresignedUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PresignedUploadJpaRepository extends JpaRepository<PresignedUpload, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select upload from PresignedUpload upload where upload.objectKey = :objectKey")
  Optional<PresignedUpload> findByObjectKeyForUpdate(@Param("objectKey") String objectKey);
}
