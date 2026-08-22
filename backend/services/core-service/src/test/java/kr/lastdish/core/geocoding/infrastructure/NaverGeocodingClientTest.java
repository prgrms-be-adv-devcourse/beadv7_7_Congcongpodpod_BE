package kr.lastdish.core.geocoding.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class NaverGeocodingClientTest {

  @Test
  void mapsNaverCoordinatesAndSendsAuthenticationHeaders() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    NaverGeocodingClient client = new NaverGeocodingClient(builder, "client-id", "client-secret");

    server
        .expect(method(HttpMethod.GET))
        .andExpect(queryParam("count", "10"))
        .andExpect(header("x-ncp-apigw-api-key-id", "client-id"))
        .andExpect(header("x-ncp-apigw-api-key", "client-secret"))
        .andRespond(
            withSuccess(
                """
                {
                  "addresses": [{
                    "roadAddress": "서울특별시 서초구 효령로 292",
                    "jibunAddress": "서울특별시 서초구 서초동 1446-1",
                    "englishAddress": "292 Hyoryeong-ro, Seocho-gu, Seoul",
                    "x": "127.0161972",
                    "y": "37.4849387"
                  }]
                }
                """,
                MediaType.APPLICATION_JSON));

    var result = client.search("서울남부터미널역");

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().latitude()).isEqualByComparingTo(new BigDecimal("37.4849387"));
    assertThat(result.getFirst().longitude()).isEqualByComparingTo(new BigDecimal("127.0161972"));
    server.verify();
  }
}
