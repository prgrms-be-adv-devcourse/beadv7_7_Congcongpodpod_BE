package kr.lastdish.payment.application.event;

import java.time.Instant;
import java.util.UUID;
import kr.lastdish.common.outbox.application.OutboxEventWriter;
import kr.lastdish.payment.domain.Payment;
import kr.lastdish.payment.domain.event.ChargeRequestedEvent;
import kr.lastdish.payment.domain.event.ChargeRequestedPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChargeRequestedEventWriter {

    private final OutboxEventWriter outboxEventWriter;

    public void append(Payment payment, long aggregateVersion) {
        ChargeRequestedEvent event =
                new ChargeRequestedEvent(
                        UUID.randomUUID(),
                        ChargeRequestedEvent.SCHEMA_VERSION,
                        payment.getId(),
                        aggregateVersion,
                        new ChargeRequestedPayload(payment.getMemberId(), payment.getId(), payment.getAmount()),
                        Instant.now());

        outboxEventWriter.append(event);
    }
}