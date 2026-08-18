package kr.lastdish.common.storage.s3;

import java.util.Optional;
import kr.lastdish.common.storage.ObjectStorage;
import kr.lastdish.common.storage.download.application.PresignedDownloadService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.S3Presigner.Builder;

/**
 * {@code storage.s3.enabled=true}일 때 AWS S3 클라이언트와 {@link ObjectStorage} 구현을 등록합니다.
 *
 * <p>기능이 비활성화된 환경에서도 조회 서비스를 생성해 명확한 비활성화 오류를 반환할 수 있게 합니다.
 */
@AutoConfiguration
@EnableConfigurationProperties(S3StorageProperties.class)
public class S3StorageAutoConfiguration {

  @Bean
  @ConditionalOnProperty(prefix = "storage.s3", name = "enabled", havingValue = "true")
  S3Client s3Client(S3StorageProperties properties) {
    S3ClientBuilder builder = S3Client.builder().region(Region.of(properties.region()));
    Optional.ofNullable(properties.endpoint()).ifPresent(builder::endpointOverride);
    return builder.build();
  }

  @Bean
  @ConditionalOnProperty(prefix = "storage.s3", name = "enabled", havingValue = "true")
  S3Presigner s3Presigner(S3StorageProperties properties) {
    Builder builder = S3Presigner.builder().region(Region.of(properties.region()));
    Optional.ofNullable(properties.endpoint()).ifPresent(builder::endpointOverride);
    return builder.build();
  }

  @Bean
  @ConditionalOnMissingBean(ObjectStorage.class)
  @ConditionalOnProperty(prefix = "storage.s3", name = "enabled", havingValue = "true")
  ObjectStorage objectStorage(
      S3Client s3Client, S3Presigner s3Presigner, S3StorageProperties properties) {
    return new S3ObjectStorage(s3Client, s3Presigner, properties);
  }

  @Bean
  PresignedDownloadService presignedDownloadService(
      Optional<ObjectStorage> objectStorage, S3StorageProperties properties) {
    return new PresignedDownloadService(objectStorage, properties);
  }
}
