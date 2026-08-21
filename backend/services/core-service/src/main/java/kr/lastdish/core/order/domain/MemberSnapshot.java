package kr.lastdish.core.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "member_snapshots")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberSnapshot {

  @Id
  @Column(name = "member_id")
  private Long memberId;

  @Column(nullable = false, length = 50)
  private String name;

  @Column(nullable = false, length = 50)
  private String phone;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "aggregate_version", nullable = false)
  private long aggregateVersion;

  @Column(name = "is_deleted", nullable = false)
  private boolean deleted;

  public static MemberSnapshot create(Long memberId, String name, String phone) {
    MemberSnapshot snapshot = new MemberSnapshot();
    snapshot.memberId = memberId;
    snapshot.update(name, phone);
    snapshot.aggregateVersion = 0L;
    snapshot.deleted = false;
    return snapshot;
  }

  public void update(String name, String phone) {
    this.name = name;
    this.phone = phone;
    this.updatedAt = LocalDateTime.now();
  }
}
