package kr.lastdish.common.inbox.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InboxAggregateVersionId implements Serializable {

  @Column(name = "consumer_id", nullable = false, length = 100)
  private String consumerId;

  @Column(name = "aggregate_type", nullable = false, length = 50)
  private String aggregateType;

  @Column(name = "aggregate_id", nullable = false)
  private Long aggregateId;

  public InboxAggregateVersionId(String consumerId, String aggregateType, Long aggregateId) {
    this.consumerId = consumerId;
    this.aggregateType = aggregateType;
    this.aggregateId = aggregateId;
  }
}
