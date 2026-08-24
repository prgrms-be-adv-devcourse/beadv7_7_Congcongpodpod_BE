package kr.lastdish.core.point.application;

import kr.lastdish.core.point.application.dto.PointHistoryResponse;
import kr.lastdish.core.point.domain.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointHistoryService {

  private final PointHistoryRepository pointHistoryRepository;

  @Transactional(readOnly = true)
  public Page<PointHistoryResponse> getHistory(Long memberId, Pageable pageable) {
    return pointHistoryRepository
        .findByMemberId(memberId, pageable)
        .map(PointHistoryResponse::from);
  }
}
