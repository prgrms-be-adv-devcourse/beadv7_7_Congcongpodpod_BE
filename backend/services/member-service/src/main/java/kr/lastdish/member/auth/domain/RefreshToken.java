package kr.lastdish.member.auth.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false, length = 512)
  private String token;

  @Column(nullable = false)
  private LocalDateTime expiryDate;

  @Builder
  public RefreshToken(String email, String token, LocalDateTime expiryDate) {
    this.email = email;
    this.token = token;
    this.expiryDate = expiryDate;
  }

  public void updateToken(String newToken, LocalDateTime newExpiryDate) {
    this.token = newToken;
    this.expiryDate = newExpiryDate;
  }
}
