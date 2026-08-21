package kr.lastdish.common.event;

@FunctionalInterface
public interface EventTopicResolver {

  String resolve(EventMessage message);
}
