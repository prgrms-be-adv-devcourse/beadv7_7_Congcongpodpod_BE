package kr.lastdish.common.event.config;

import kr.lastdish.common.event.publisher.spring.SpringEventPublisher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ConditionalOnProperty(name = "event.publisher", havingValue = "spring")
@Import(SpringEventPublisher.class)
public class EventCommonAutoConfiguration {}
