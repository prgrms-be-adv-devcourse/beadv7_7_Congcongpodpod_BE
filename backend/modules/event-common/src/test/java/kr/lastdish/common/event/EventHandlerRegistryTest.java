package kr.lastdish.common.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class EventHandlerRegistryTest {

  @Test
  void 동일한_consumerId라도_eventType이_다르면_각_handler를_조회한다() {
    EventHandler created = new TestEventHandler("member-snapshot", "MEMBER_CREATED");
    EventHandler deleted = new TestEventHandler("member-snapshot", "MEMBER_DELETED");
    EventHandlerRegistry registry = new EventHandlerRegistry(List.of(created, deleted));

    assertThat(registry.get("member-snapshot", "MEMBER_CREATED")).isSameAs(created);
    assertThat(registry.get("member-snapshot", "MEMBER_DELETED")).isSameAs(deleted);
  }

  @Test
  void consumerId와_eventType_조합에_해당하는_handler가_없으면_예외가_발생한다() {
    EventHandlerRegistry registry =
        new EventHandlerRegistry(
            List.of(new TestEventHandler("member-snapshot", "MEMBER_CREATED")));

    assertThatThrownBy(() -> registry.get("member-snapshot", "MEMBER_DELETED"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("consumerId=member-snapshot")
        .hasMessageContaining("eventType=MEMBER_DELETED");
  }

  @Test
  void consumerId와_eventType이_모두_같은_handler는_중복_등록할_수_없다() {
    EventHandler first = new TestEventHandler("member-snapshot", "MEMBER_CREATED");
    EventHandler duplicate = new TestEventHandler("member-snapshot", "MEMBER_CREATED");

    assertThatThrownBy(() -> new EventHandlerRegistry(List.of(first, duplicate)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Duplicate key");
  }

  private record TestEventHandler(String consumerId, String eventType) implements EventHandler {

    @Override
    public void handle(EventMessage message) {}
  }
}
