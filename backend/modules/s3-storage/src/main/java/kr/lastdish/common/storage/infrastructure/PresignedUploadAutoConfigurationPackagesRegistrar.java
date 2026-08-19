package kr.lastdish.common.storage.infrastructure;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/** 공통 모듈의 PresignedUpload 엔티티와 JPA 저장소 패키지를 자동 설정 스캔 범위에 등록합니다. */
final class PresignedUploadAutoConfigurationPackagesRegistrar
    implements ImportBeanDefinitionRegistrar {

  @Override
  public void registerBeanDefinitions(
      AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
    AutoConfigurationPackages.register(
        registry,
        "kr.lastdish.common.storage.domain",
        "kr.lastdish.common.storage.infrastructure");
  }
}
