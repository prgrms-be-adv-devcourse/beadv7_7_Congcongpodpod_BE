package kr.lastdish.member.auth.domain;

import java.util.Optional;

public interface RefreshTokenRepository {

  RefreshToken save(RefreshToken refreshToken);

  Optional<RefreshToken> findByEmail(String email);

  Optional<RefreshToken> findByToken(String token);

  void delete(RefreshToken refreshToken);

  void deleteByEmail(String email);
}