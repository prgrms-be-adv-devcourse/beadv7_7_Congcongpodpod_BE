package kr.lastdish.member.member.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long tokenId;

  private String value;
  private Long memberId;

  public RefreshToken(String value, Long memberId) {
    this.value = value;
    this.memberId = memberId;
  }
}
