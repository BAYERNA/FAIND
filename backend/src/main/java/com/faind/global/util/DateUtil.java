package com.faind.global.util;

import java.time.Duration;
import java.time.LocalDateTime;

public final class DateUtil {

  private DateUtil() {}

  public static String formatElapsed(LocalDateTime from, LocalDateTime to) {
    Duration duration = Duration.between(from, to);
    long hours = duration.toHours();
    long minutes = duration.toMinutesPart();
    long seconds = duration.toSecondsPart();
    if (hours > 0) {
      return "%d시간%02d분%02d초".formatted(hours, minutes, seconds);
    }
    return "%d분%02d초".formatted(minutes, seconds);
  }

  public static boolean isWithin(LocalDateTime timestamp, LocalDateTime now, Duration window) {
    return !timestamp.isBefore(now.minus(window));
  }
}
