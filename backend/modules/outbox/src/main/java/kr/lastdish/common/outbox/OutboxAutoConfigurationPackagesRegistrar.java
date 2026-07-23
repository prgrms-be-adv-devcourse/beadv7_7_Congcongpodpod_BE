package kr.lastdish.common.outbox;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

final class OutboxAutoConfigurationPackagesRegistrar implements ImportBeanDefinitionRegistrar {

  @Override
  public void registerBeanDefinitions(
      AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
    AutoConfigurationPackages.register(registry, "kr.lastdish.common.outbox");
  }
}
