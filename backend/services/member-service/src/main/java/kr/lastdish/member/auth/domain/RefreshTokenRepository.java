package kr.lastdish.member.auth.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
  Optional<RefreshToken> findByEmail(String email);

  Optional<RefreshToken> findByToken(String token);
}
