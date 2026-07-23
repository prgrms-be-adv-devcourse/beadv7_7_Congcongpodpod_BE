package kr.lastdish.common.outbox;

import kr.lastdish.common.outbox.application.OutboxClaimService;
import kr.lastdish.common.outbox.application.OutboxEventProcessor;
import kr.lastdish.common.outbox.application.OutboxEventWriter;
import kr.lastdish.common.outbox.application.OutboxFailureRecorder;
import kr.lastdish.common.outbox.application.OutboxScheduler;
import kr.lastdish.common.outbox.infrastructure.OutboxClaimRepository;
import kr.lastdish.common.outbox.infrastructure.OutboxEventSerializer;
import kr.lastdish.common.outbox.infrastructure.OutboxRepositoryAdapter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@AutoConfiguration(
    before = {HibernateJpaAutoConfiguration.class, DataJpaRepositoriesAutoConfiguration.class})
@EnableScheduling
@Import({
  OutboxAutoConfigurationPackagesRegistrar.class,
  OutboxClaimService.class,
  OutboxEventProcessor.class,
  OutboxEventWriter.class,
  OutboxFailureRecorder.class,
  OutboxScheduler.class,
  OutboxClaimRepository.class,
  OutboxEventSerializer.class,
  OutboxRepositoryAdapter.class
})
public class OutboxAutoConfiguration {}
