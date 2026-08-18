package kr.lastdish.common.storage.upload.infrastructure;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

final class PresignedUploadAutoConfigurationPackagesRegistrar
    implements ImportBeanDefinitionRegistrar {

  @Override
  public void registerBeanDefinitions(
      AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
    AutoConfigurationPackages.register(registry, "kr.lastdish.common.storage.upload");
  }
}
