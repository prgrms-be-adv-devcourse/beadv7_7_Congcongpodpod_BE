package kr.lastdish.member.member.application.event;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import kr.lastdish.common.event.EventMessage;
import kr.lastdish.member.member.application.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class StoreRegisteredMessageHandlerTest {
  @Mock private MemberService memberService;

  @Mock private EventMessage message;

  @InjectMocks private StoreRegisteredMessageHandler handler;

  @BeforeEach
  void setUp() {
    handler = new StoreRegisteredMessageHandler(new ObjectMapper(), memberService);
  }

  @Test
  void grants_seller_role_when_store_registered_event_is_received() {
    // given
    Long memberId = 1L;
    String payloadJson =
        """
        {
          "memberId": 1
        }
        """;

    when(message.payload()).thenReturn(payloadJson);

    // when
    handler.handle(message);

    // then
    verify(memberService).grantSellerRole(memberId);
  }

  @Test
  void throws_exception_when_payload_deserialization_fails() {
    // given
    String invalidPayload = "invalid-payload";

    when(message.payload()).thenReturn(invalidPayload);

    // when & then
    assertThatThrownBy(() -> handler.handle(message))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("역직렬화 실패");

    verifyNoInteractions(memberService);
  }
}
