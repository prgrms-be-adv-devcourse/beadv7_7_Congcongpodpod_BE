package kr.lastdish.ai.elastic.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StoreSearchRequest {
  @NotBlank(message = "검색어는 필수 입력 값입니다.")
  private String query;

  @NotNull(message = "위도는 필수 입력 값입니다.")
  private Double latitude;

  @NotNull(message = "경도는 필수 입력 값입니다.")
  private Double longitude;

  private BigDecimal walletBalance;
  private Double radiusKm = 3.0; // 기본 반경 설정
}
