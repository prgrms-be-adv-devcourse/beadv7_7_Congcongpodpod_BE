package kr.lastdish.ai.elastic.presentation.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StoreSearchRequest {
  private String query; // 검색어
  private Double latitude;
  private Double longitude;
  private Double radiusKm = 3.0; // 기본 반경 설정
}
