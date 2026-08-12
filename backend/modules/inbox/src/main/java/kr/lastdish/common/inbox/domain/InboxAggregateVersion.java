package kr.lastdish.common.inbox.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@Table(name = "inbox_aggregate_versions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InboxAggregateVersion {

  @EmbeddedId private InboxAggregateVersionId id;

  @Column(name = "last_processed_version", nullable = false)
  private long lastProcessedVersion;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public static InboxAggregateVersion initial(InboxAggregateVersionId id, Instant now) {
    InboxAggregateVersion version = new InboxAggregateVersion();
    version.id = id;
    version.lastProcessedVersion = 0L;
    version.updatedAt = now;
    return version;
  }

  public void advanceLatestTo(long version, Instant now) {
    if (version <= lastProcessedVersion) {
      throw new IllegalArgumentException("현재 버전보다 큰 값만 적용할 수 있습니다.");
    }

    lastProcessedVersion = version;
    updatedAt = now;
  }
}
