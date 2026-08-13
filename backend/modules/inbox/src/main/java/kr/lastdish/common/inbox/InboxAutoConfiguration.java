package kr.lastdish.common.inbox;

import kr.lastdish.common.inbox.application.*;
import kr.lastdish.common.inbox.infrastructure.InboxAggregateVersionRepositoryAdapter;
import kr.lastdish.common.inbox.infrastructure.InboxClaimRepositoryAdapter;
import kr.lastdish.common.inbox.infrastructure.InboxRepositoryAdapter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@AutoConfiguration(
    before = {HibernateJpaAutoConfiguration.class, DataJpaRepositoriesAutoConfiguration.class})
@EnableScheduling
@Import({
  InboxAutoConfigurationPackagesRegistry.class,
  InboxEventWriter.class,
  InboxClaimService.class,
  InboxScheduler.class,
  InboxEventProcessor.class,
  InboxFailureRecorder.class,
  InboxEventHandlerRegistry.class,
  InboxRepositoryAdapter.class,
  InboxClaimRepositoryAdapter.class,
  InboxAggregateVersionRepositoryAdapter.class
})
public class InboxAutoConfiguration {}
