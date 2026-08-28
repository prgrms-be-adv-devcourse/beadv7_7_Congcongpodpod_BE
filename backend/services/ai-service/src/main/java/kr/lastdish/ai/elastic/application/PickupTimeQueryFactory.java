package kr.lastdish.ai.elastic.application;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

final class PickupTimeQueryFactory {
  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
  private static final String SPANS_MIDNIGHT_FIELD = "dishes.pickupSpansMidnight";
  private static final String START_TIME_FIELD = "dishes.pickupStartTime";
  private static final String END_TIME_FIELD = "dishes.pickupEndTime";

  private PickupTimeQueryFactory() {}

  static Query notExpired(LocalTime now) {
    String currentTime = now.format(TIME_FORMATTER);
    BoolQuery normalWindow = normalWindow().filter(endTimeOnOrAfter(currentTime)).build();
    return either(normalWindow, overnightWindow(currentTime));
  }

  static Query currentlyAvailable(LocalTime now) {
    String currentTime = now.format(TIME_FORMATTER);
    BoolQuery normalWindow =
        normalWindow()
            .filter(startTimeOnOrBefore(currentTime))
            .filter(endTimeOnOrAfter(currentTime))
            .build();
    return either(normalWindow, overnightWindow(currentTime));
  }

  static Query startsByDeadline(LocalTime now, LocalTime deadline) {
    String currentTime = now.format(TIME_FORMATTER);
    String deadlineTime = deadline.format(TIME_FORMATTER);
    BoolQuery normalWindow = normalWindow().filter(startTimeOnOrBefore(deadlineTime)).build();
    BoolQuery.Builder overnightWindow = overnightWindow();

    if (!deadline.isBefore(now)) {
      overnightWindow
          .should(startTimeOnOrBefore(deadlineTime))
          .should(endTimeOnOrAfter(currentTime))
          .minimumShouldMatch("1");
    }

    return either(normalWindow, overnightWindow.build());
  }

  private static BoolQuery.Builder normalWindow() {
    return new BoolQuery.Builder().mustNot(spansMidnight(true));
  }

  private static BoolQuery overnightWindow(String currentTime) {
    return overnightWindow()
        .should(startTimeOnOrBefore(currentTime))
        .should(endTimeOnOrAfter(currentTime))
        .minimumShouldMatch("1")
        .build();
  }

  private static BoolQuery.Builder overnightWindow() {
    return new BoolQuery.Builder().filter(spansMidnight(true));
  }

  private static Query either(BoolQuery normalWindow, BoolQuery overnightWindow) {
    return Query.of(
        q ->
            q.bool(
                b ->
                    b.should(Query.of(n -> n.bool(normalWindow)))
                        .should(Query.of(n -> n.bool(overnightWindow)))
                        .minimumShouldMatch("1")));
  }

  private static Query spansMidnight(boolean value) {
    return Query.of(q -> q.term(t -> t.field(SPANS_MIDNIGHT_FIELD).value(value)));
  }

  private static Query startTimeOnOrBefore(String time) {
    return Query.of(q -> q.range(r -> r.date(d -> d.field(START_TIME_FIELD).lte(time))));
  }

  private static Query endTimeOnOrAfter(String time) {
    return Query.of(q -> q.range(r -> r.date(d -> d.field(END_TIME_FIELD).gte(time))));
  }
}
