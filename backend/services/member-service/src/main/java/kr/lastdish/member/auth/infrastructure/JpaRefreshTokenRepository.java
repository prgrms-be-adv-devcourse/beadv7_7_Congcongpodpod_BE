package kr.lastdish.member.auth.infrastructure;

import kr.lastdish.member.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaRefreshTokenRepository
    extends JpaRepository<RefreshToken, Long> {

  Optional<RefreshToken> findByEmail(String email);

  Optional<RefreshToken> findByToken(String token);

  void deleteByEmail(String email);
}