package kr.lastdish.common.event;

import kr.lastdish.common.event.spring.SpringEventPublisher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ConditionalOnProperty(name = "event.publisher", havingValue = "spring", matchIfMissing = true)
@Import(SpringEventPublisher.class)
public class EventCommonAutoConfiguration {}
