package kr.lastdish.common.inbox.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;
import lombok.*;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class InboxEventId implements Serializable {

  @Column(name = "consumer_id", length = 100, nullable = false)
  private String consumerId;

  @Column(name = "event_id", nullable = false)
  private UUID eventId;
}
