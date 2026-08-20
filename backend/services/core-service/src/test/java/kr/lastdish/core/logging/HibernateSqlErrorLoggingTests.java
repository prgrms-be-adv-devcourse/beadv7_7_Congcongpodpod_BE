package kr.lastdish.core.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import kr.lastdish.core.payment.domain.deposit.Deposit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hibernate가 SQL 오류를 남길 때 제약을 위반한 값이 로그에 새지 않는지 확인한다.
 *
 * <p>Hibernate는 예외를 위로 올리기 전에 {@code org.hibernate.orm.jdbc.error} 로거로 WARN 두 줄을 먼저 남기고, 그중 한 줄에
 * DB가 돌려준 메시지가 그대로 들어간다. PostgreSQL은 그 메시지에 위반한 값을 담는다. 그래서 각 서비스의 {@code application.yml}에서 이 로거만
 * ERROR로 올려 WARN을 억제한다.
 *
 * <p>여기서는 레벨을 코드로 바꿔가며 두 가지를 고정한다. 첫 번째 테스트는 <b>억제하지 않으면 실제로 값이 샌다</b>는 사실을 못박는다. Hibernate가 로거 이름을
 * 바꾸거나 더 이상 값을 남기지 않게 되면 이 테스트가 깨지고, 그때 설정이 낡았다는 것을 알게 된다.
 */
@SpringBootTest
class HibernateSqlErrorLoggingTests {

  private static final String HIBERNATE_SQL_ERROR_LOGGER = "org.hibernate.orm.jdbc.error";
  private static final long 중복_회원_번호 = 987654321L;

  @Autowired private EntityManager entityManager;

  private ch.qos.logback.classic.Logger 로거;
  private Level 원래_레벨;
  private ListAppender<ILoggingEvent> appender;

  @BeforeEach
  void 로그수집기를_붙인다() {
    로거 = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(HIBERNATE_SQL_ERROR_LOGGER);
    원래_레벨 = 로거.getLevel();
    appender = new ListAppender<>();
    appender.start();
    로거.addAppender(appender);
  }

  @AfterEach
  void 로그수집기를_뗀다() {
    로거.detachAppender(appender);
    appender.stop();
    로거.setLevel(원래_레벨);
  }

  @Test
  @Transactional
  @DisplayName("억제하지 않으면 제약을 위반한 값이 로그에 남는다")
  void 억제하지_않으면_값이_샌다() {
    로거.setLevel(Level.WARN);

    제약을_위반한다();

    assertThat(수집된_메시지())
        .as("이 테스트가 깨지면 Hibernate의 로깅 방식이 바뀐 것이므로 application.yml의 로거 이름을 다시 확인한다")
        .anyMatch(message -> message.contains(String.valueOf(중복_회원_번호)));
  }

  @Test
  @Transactional
  @DisplayName("ERROR로 올리면 값이 로그에 남지 않는다")
  void 억제하면_값이_새지_않는다() {
    로거.setLevel(Level.ERROR);

    제약을_위반한다();

    assertThat(수집된_메시지()).isEmpty();
  }

  private void 제약을_위반한다() {
    entityManager.persist(new Deposit(중복_회원_번호, BigDecimal.ZERO));
    entityManager.flush();

    assertThatThrownBy(
            () -> {
              entityManager.persist(new Deposit(중복_회원_번호, BigDecimal.ONE));
              entityManager.flush();
            })
        .isInstanceOf(Exception.class);
  }

  private java.util.List<String> 수집된_메시지() {
    return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
  }
}
