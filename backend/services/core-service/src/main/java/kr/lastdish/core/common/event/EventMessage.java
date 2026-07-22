package kr.lastdish.core.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Outbox에 저장된 이벤트를 발행 기술로 전달하기 위한 직렬화 메시지입니다.
 *
 * <p>구체적인 DomainEvent 객체 대신 이벤트 metadata와 JSON payload를 전달합니다. 따라서 Outbox Processor는 Dish, Order 등
 * 구체적인 도메인 이벤트 타입을 알 필요가 없습니다.
 *
 * @param eventId 이벤트 고유 식별자
 * @param eventType 이벤트 종류
 * @param aggregateType 이벤트가 발생한 도메인 종류
 * @param aggregateId 이벤트가 발생한 도메인 식별자
 * @param payload 직렬화된 이벤트 JSON
 * @param occurredAt 이벤트 발생 시각
 */
public record EventMessage(
    UUID eventId,
    String eventType,
    String aggregateType,
    Long aggregateId,
    String payload,
    Instant occurredAt) {}
