package kr.lastdish.member.notification.presentation.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageResponse<T>(List<T> items, long total, int page, int size, boolean hasNext) {

  public static <T> PageResponse<T> from(Page<T> page) {
    return new PageResponse<>(
        page.getContent(),
        page.getTotalElements(),
        page.getNumber(),
        page.getSize(),
        page.hasNext());
  }
}
