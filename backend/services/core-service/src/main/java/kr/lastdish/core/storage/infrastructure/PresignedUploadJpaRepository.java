package kr.lastdish.core.storage.infrastructure;

import kr.lastdish.core.storage.domain.PresignedUpload;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PresignedUploadJpaRepository extends JpaRepository<PresignedUpload, Long> {}
