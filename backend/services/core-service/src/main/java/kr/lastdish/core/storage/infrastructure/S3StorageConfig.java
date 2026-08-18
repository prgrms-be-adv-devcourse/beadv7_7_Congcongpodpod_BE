package kr.lastdish.core.storage.infrastructure;

import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.S3Presigner.Builder;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "storage.s3", name = "enabled", havingValue = "true")
public class S3StorageConfig {

  @Bean
  S3Presigner s3Presigner(S3StorageProperties properties) {
    Builder builder = S3Presigner.builder().region(Region.of(properties.region()));
    Optional.ofNullable(properties.endpoint()).ifPresent(builder::endpointOverride);
    return builder.build();
  }

  @Bean
  S3PresignedUploadUrlProvider s3PresignedUploadUrlProvider(
      S3Presigner s3Presigner, S3StorageProperties properties) {
    return new S3PresignedUploadUrlProvider(s3Presigner, properties);
  }
}
