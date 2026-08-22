package kr.lastdish.core.favorite.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "store_favorites",
    uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "store_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreFavorite {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "store_favorite_id")
  private Long id;

  @Column(name = "member_id", nullable = false, updatable = false)
  private Long memberId;

  @Column(name = "store_id", nullable = false, updatable = false)
  private Long storeId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  private StoreFavorite(Long memberId, Long storeId) {
    this.memberId = memberId;
    this.storeId = storeId;
    this.createdAt = LocalDateTime.now();
  }

  public static StoreFavorite create(Long memberId, Long storeId) {
    return new StoreFavorite(memberId, storeId);
  }
}
