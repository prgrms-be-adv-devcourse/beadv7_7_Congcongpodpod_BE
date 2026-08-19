package kr.lastdish.common.event.config;

import kr.lastdish.common.event.EventHandlerRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import(EventHandlerRegistry.class)
public class EventHandlerAutoConfiguration {}
