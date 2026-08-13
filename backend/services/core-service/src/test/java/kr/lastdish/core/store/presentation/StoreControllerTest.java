package kr.lastdish.core.store.presentation;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import kr.lastdish.core.dish.domain.Dish;
import kr.lastdish.core.dish.domain.DishRepository;
import kr.lastdish.core.dish.domain.DishStatus;
import kr.lastdish.core.store.application.port.out.SellerRoleGrantPort;
import kr.lastdish.core.store.domain.Category;
import kr.lastdish.core.store.domain.Store;
import kr.lastdish.core.store.domain.StoreRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StoreControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private StoreRepository storeRepository;
  @Autowired private DishRepository dishRepository;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @MockitoBean private SellerRoleGrantPort sellerRoleGrantPort;

  @Test
  void 매장_등록과_수정_API에_카테고리가_반영된다() throws Exception {
    String createResponse =
        mockMvc
            .perform(
                post("/api/v1/stores")
                    .header("X-Authenticated-Member-Id", 10L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "storeName": "테스트 한식집",
                          "businessNumber": "123-45-67890",
                          "storeAddress": "서울시 테스트 주소",
                          "storePhone": "02-1234-5678",
                          "openTime": "09:00",
                          "closeTime": "22:00",
                          "latitude": 37.5,
                          "longitude": 127.0,
                          "category": "KOREAN",
                          "holidays": []
                        }
                        """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.category").value("KOREAN"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    verify(sellerRoleGrantPort).grantSellerRole(10L);

    long storeId = objectMapper.readTree(createResponse).path("data").path("storeId").asLong();

    mockMvc
        .perform(
            put("/api/v1/stores/{storeId}", storeId)
                .header("X-Authenticated-Member-Id", 10L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "storeName": "테스트 치킨집",
                      "storeAddress": "서울시 변경 주소",
                      "storePhone": "02-9999-9999",
                      "openTime": "10:00",
                      "closeTime": "23:00",
                      "latitude": 37.5,
                      "longitude": 127.0,
                      "category": "CHICKEN",
                      "holidays": []
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.storeId").value(storeId))
        .andExpect(jsonPath("$.data.category").value("CHICKEN"));
  }

  @Test
  void 주변_매장을_카테고리로_필터링하고_판매중인_상품만_응답한다() throws Exception {
    Store koreanStore = saveStore(1L, "한식 매장", Category.KOREAN, "37.5000", "127.0000");
    Store chickenStore = saveStore(2L, "치킨 매장", Category.CHICKEN, "37.5010", "127.0010");

    Dish onSaleDish = saveDish(koreanStore.getId(), "김치찌개");
    Dish soldOutDish = saveDish(koreanStore.getId(), "품절 비빔밥");
    soldOutDish.updateStatus(DishStatus.SOLD_OUT);
    saveDish(chickenStore.getId(), "후라이드 치킨");

    mockMvc
        .perform(
            get("/api/v1/stores/nearby")
                .param("latitude", "37.5000")
                .param("longitude", "127.0000")
                .param("radiusKm", "3")
                .param("category", "KOREAN"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.stores.length()").value(1))
        .andExpect(jsonPath("$.data.stores[0].storeId").value(koreanStore.getId()))
        .andExpect(jsonPath("$.data.stores[0].category").value("KOREAN"))
        .andExpect(jsonPath("$.data.stores[0].dishes.length()").value(1))
        .andExpect(jsonPath("$.data.stores[0].dishes[0].dishId").value(onSaleDish.getId()))
        .andExpect(jsonPath("$.data.stores[0].dishes[0].dishName").value("김치찌개"));
  }

  private Store saveStore(
      Long memberId, String storeName, Category category, String latitude, String longitude) {
    return storeRepository.save(
        new Store(
            memberId,
            storeName,
            "business-" + memberId,
            "서울시 테스트 주소",
            "010-0000-000" + memberId,
            LocalTime.of(9, 0),
            LocalTime.of(22, 0),
            new BigDecimal(latitude),
            new BigDecimal(longitude),
            category));
  }

  private Dish saveDish(Long storeId, String dishName) {
    return dishRepository.save(
        Dish.create(
            storeId,
            dishName,
            LocalDateTime.now(),
            "테스트 상품",
            null,
            10L,
            BigDecimal.valueOf(10_000),
            BigDecimal.valueOf(7_000),
            LocalTime.of(18, 0),
            LocalTime.of(19, 0)));
  }
}
