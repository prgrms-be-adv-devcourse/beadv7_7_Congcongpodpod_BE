package kr.lastdish.common.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class EventHandlerRegistryTest {

  @Test
  void 같은_consumerId의_서로_다른_eventType_핸들러를_등록한다() {
    EventHandler created = handler("member-snapshot", "MEMBER_CREATED");
    EventHandler updated = handler("member-snapshot", "MEMBER_UPDATED");
    EventHandlerRegistry registry = new EventHandlerRegistry(List.of(created, updated));

    assertThat(registry.get("member-snapshot", "MEMBER_CREATED")).isSameAs(created);
    assertThat(registry.get("member-snapshot", "MEMBER_UPDATED")).isSameAs(updated);
  }

  @Test
  void consumerId와_eventType이_모두_같은_핸들러는_등록할_수_없다() {
    EventHandler first = handler("member-snapshot", "MEMBER_CREATED");
    EventHandler duplicate = handler("member-snapshot", "MEMBER_CREATED");

    assertThatThrownBy(() -> new EventHandlerRegistry(List.of(first, duplicate)))
        .isInstanceOf(IllegalStateException.class);
  }

  private EventHandler handler(String consumerId, String eventType) {
    return new EventHandler() {
      @Override
      public String consumerId() {
        return consumerId;
      }

      @Override
      public String eventType() {
        return eventType;
      }

      @Override
      public void handle(EventMessage message) {}
    };
  }
}
