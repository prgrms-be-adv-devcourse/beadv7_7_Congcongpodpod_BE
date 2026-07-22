package kr.lastdish.core.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Core Service의 Spring Scheduler 기능을 활성화합니다.
 *
 * <p>실제 OutboxScheduler Bean은 outbox.scheduler.enabled 설정이 true일 때만 등록되므로 Scheduling 기능을 활성화해도
 * Outbox 조회가 항상 실행되지는 않습니다.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {}
