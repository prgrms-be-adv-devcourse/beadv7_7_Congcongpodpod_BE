package kr.lastdish.member.notification.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "notification_id")
  private Long id;

  @Column(name = "member_id", nullable = false)
  private Long memberId;

  @Column(name = "type", nullable = false, length = 100)
  private String type;

  @Column(name = "title", length = 255)
  private String title;

  @Column(name = "body", length = 1000)
  private String body;

  @Column(name = "data", columnDefinition = "TEXT")
  private String data;

  @Column(name = "link_target", length = 100)
  private String linkTarget;

  @Column(name = "link_id")
  private Long linkId;

  @Column(name = "read_yn", nullable = false)
  private boolean readYn;

  @Column(name = "event_id", unique = true)
  private UUID eventId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public Notification(
      Long memberId,
      String type,
      String title,
      String body,
      String data,
      String linkTarget,
      Long linkId,
      UUID eventId) {
    this.memberId = memberId;
    this.type = type;
    this.title = title;
    this.body = body;
    this.data = data;
    this.linkTarget = linkTarget;
    this.linkId = linkId;
    this.eventId = eventId;
    this.readYn = false;
    this.createdAt = LocalDateTime.now();
  }

  public void markAsRead() {
    this.readYn = true;
  }
}
