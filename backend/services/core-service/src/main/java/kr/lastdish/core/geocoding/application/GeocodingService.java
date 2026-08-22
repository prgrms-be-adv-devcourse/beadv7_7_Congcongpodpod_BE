package kr.lastdish.core.geocoding.application;

import java.util.List;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.core.common.exception.ErrorCode;
import kr.lastdish.core.geocoding.infrastructure.NaverGeocodingClient;
import kr.lastdish.core.geocoding.presentation.dto.GeocodingAddressResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GeocodingService {

  private final NaverGeocodingClient geocodingClient;

  public List<GeocodingAddressResponse> search(String query) {
    List<GeocodingAddressResponse> addresses = geocodingClient.search(query.trim());
    if (addresses.isEmpty()) {
      throw new BusinessException(ErrorCode.GEOCODING_ADDRESS_NOT_FOUND);
    }
    return addresses;
  }
}
