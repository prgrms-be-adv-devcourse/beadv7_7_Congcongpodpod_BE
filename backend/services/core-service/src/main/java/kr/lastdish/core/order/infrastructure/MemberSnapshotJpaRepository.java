package kr.lastdish.core.order.infrastructure;

import kr.lastdish.core.order.domain.MemberSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberSnapshotJpaRepository extends JpaRepository<MemberSnapshot, Long> {}
