package kr.lastdish.core.geocoding.presentation;

import jakarta.validation.constraints.Size;
import kr.lastdish.common.api.response.ApiResponse;
import kr.lastdish.core.geocoding.application.GeocodingService;
import kr.lastdish.core.geocoding.presentation.dto.GeocodingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/locations")
public class GeocodingController {

  private final GeocodingService geocodingService;

  @GetMapping("/geocode")
  public ApiResponse<GeocodingResponse> geocode(
      @RequestParam @Size(min = 2, max = 200) String query) {
    return ApiResponse.ok(new GeocodingResponse(geocodingService.search(query)));
  }
}
