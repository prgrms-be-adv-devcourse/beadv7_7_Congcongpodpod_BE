package kr.lastdish.member.auth.infrastructure;

import java.util.Optional;
import kr.lastdish.member.auth.domain.RefreshToken;
import kr.lastdish.member.auth.domain.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

  private final JpaRefreshTokenRepository jpaRepository;

  @Override
  public RefreshToken save(RefreshToken refreshToken) {
    return jpaRepository.save(refreshToken);
  }

  @Override
  public Optional<RefreshToken> findByEmail(String email) {
    return jpaRepository.findByEmail(email);
  }

  @Override
  public Optional<RefreshToken> findByToken(String token) {
    return jpaRepository.findByToken(token);
  }

  @Override
  public void delete(RefreshToken refreshToken) {
    jpaRepository.delete(refreshToken);
  }

  @Override
  public void deleteByEmail(String email) {
    jpaRepository.deleteByEmail(email);
  }
}
