package kr.lastdish.common.mvc;

import java.util.OptionalInt;

/**
 * 요청 하나가 실행한 SQL 문의 수를 스레드별로 센다.
 *
 * <p>부하 테스트로는 "느리다"까지만 알 수 있고 "왜 비싼가"는 알 수 없다. 같은 응답시간이라도 SQL을 3번 던지는 API와 12번 던지는 API는 부하가 커질 때 전혀
 * 다르게 움직인다. 요청당 실행 수를 남겨 두면 N+1처럼 호출 수가 데이터 수에 비례해 늘어나는 문제를 부하를 주지 않고도 찾을 수 있다.
 *
 * <p>계측용이므로 {@link #start()}를 호출한 요청에서만 값을 센다. 시작하지 않았으면 {@link #increment()}는 아무 일도 하지 않고 {@link
 * #count()}는 빈 값을 돌려준다. 그래서 계측을 꺼도 호출부를 들어낼 필요가 없다.
 *
 * <p>값은 {@link ThreadLocal}에 담기므로 요청을 처리한 스레드에서만 읽을 수 있다. 요청이 끝나면 반드시 {@link #clear()}로 비운다. 스레드
 * 풀에서는 같은 스레드가 다음 요청에 재사용되어 값이 이어져 보이고, 값이 남으면 그대로 누수가 된다.
 */
public final class SqlStatementCounter {

  /** 배열 한 칸을 쓰는 것은 매번 새 객체를 만들지 않고 값을 올리기 위해서다. */
  private static final ThreadLocal<int[]> COUNT = new ThreadLocal<>();

  private SqlStatementCounter() {}

  /** 현재 스레드의 계측을 0에서 다시 시작한다. */
  public static void start() {
    COUNT.set(new int[1]);
  }

  /** 계측 중일 때만 실행 수를 하나 올린다. */
  public static void increment() {
    int[] holder = COUNT.get();
    if (holder != null) {
      holder[0]++;
    }
  }

  /** 계측 중이면 지금까지의 실행 수를, 아니면 빈 값을 돌려준다. */
  public static OptionalInt count() {
    int[] holder = COUNT.get();
    return holder == null ? OptionalInt.empty() : OptionalInt.of(holder[0]);
  }

  /** 현재 스레드의 계측값을 지운다. 요청이 끝날 때 반드시 호출한다. */
  public static void clear() {
    COUNT.remove();
  }
}
