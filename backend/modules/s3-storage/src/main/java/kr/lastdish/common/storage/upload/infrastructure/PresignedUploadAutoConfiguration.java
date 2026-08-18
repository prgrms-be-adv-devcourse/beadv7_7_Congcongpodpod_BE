package kr.lastdish.common.storage.upload.infrastructure;

import jakarta.persistence.EntityManager;
import java.util.Optional;
import kr.lastdish.common.storage.ObjectStorage;
import kr.lastdish.common.storage.s3.S3StorageProperties;
import kr.lastdish.common.storage.upload.application.PresignedUploadService;
import kr.lastdish.common.storage.upload.domain.PresignedUploadRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.JpaRepository;

/** JPA 환경에서 Presigned 업로드 저장소와 애플리케이션 서비스를 자동 등록합니다. */
@AutoConfiguration(
    before = {HibernateJpaAutoConfiguration.class, DataJpaRepositoriesAutoConfiguration.class})
@ConditionalOnClass({EntityManager.class, JpaRepository.class})
@Import(PresignedUploadAutoConfigurationPackagesRegistrar.class)
public class PresignedUploadAutoConfiguration {

  @Bean
  PresignedUploadRepository presignedUploadRepository(
      PresignedUploadJpaRepository presignedUploadJpaRepository) {
    return new PresignedUploadRepositoryImpl(presignedUploadJpaRepository);
  }

  @Bean
  PresignedUploadService presignedUploadService(
      Optional<ObjectStorage> objectStorage,
      S3StorageProperties properties,
      PresignedUploadRepository presignedUploadRepository) {
    return new PresignedUploadService(objectStorage, properties, presignedUploadRepository);
  }
}
