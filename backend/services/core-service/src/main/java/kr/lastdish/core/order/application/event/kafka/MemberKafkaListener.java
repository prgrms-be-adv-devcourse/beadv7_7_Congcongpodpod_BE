package kr.lastdish.core.order.application.event.kafka;

import kr.lastdish.common.event.EventMessage;
import kr.lastdish.common.inbox.application.InboxEventWriter;
import kr.lastdish.core.order.application.event.MemberCreatedMessageHandler;
import kr.lastdish.core.order.application.event.MemberDeletedMessageHandler;
import kr.lastdish.core.order.application.event.MemberUpdatedMessageHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberKafkaListener {
  public static final String TOPIC = "MEMBER_EVENTS";
  public static final String GROUP_ID = "core-order-member-events";

  private final InboxEventWriter inboxEventWriter;

  @KafkaListener(
      topics = TOPIC,
      groupId = GROUP_ID,
      autoStartup = "${event.kafka.listener-auto-startup:true}")
  public void consume(EventMessage message) {
    inboxEventWriter.saveIfAbsent(consumerId(message.eventType()), message);
  }

  private String consumerId(String eventType) {
    return switch (eventType) {
      case MemberCreatedMessageHandler.EVENT_TYPE -> MemberCreatedMessageHandler.CONSUMER_ID;
      case MemberUpdatedMessageHandler.EVENT_TYPE -> MemberUpdatedMessageHandler.CONSUMER_ID;
      case MemberDeletedMessageHandler.EVENT_TYPE -> MemberDeletedMessageHandler.CONSUMER_ID;
      default -> throw new IllegalArgumentException("지원하지 않는 회원 이벤트입니다: " + eventType);
    };
  }
}
