package kr.lastdish.core.geocoding.presentation.dto;

import java.util.List;

public record GeocodingResponse(List<GeocodingAddressResponse> addresses) {
  public GeocodingResponse {
    addresses = List.copyOf(addresses);
  }
}
