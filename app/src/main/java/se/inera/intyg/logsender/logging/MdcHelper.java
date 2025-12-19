package se.inera.intyg.logsender.logging;

import java.nio.CharBuffer;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import org.springframework.stereotype.Component;

@Component
public class MdcHelper {

  private static final int LENGTH_LIMIT = 8;
  private static final char[] BASE62CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

  public String traceId() {
    return generateId();
  }

  public String spanId() {
    return generateId();
  }

  private String generateId() {
    final CharBuffer charBuffer = CharBuffer.allocate(LENGTH_LIMIT);
    IntStream.generate(() -> ThreadLocalRandom.current().nextInt(BASE62CHARS.length))
        .limit(LENGTH_LIMIT)
        .forEach(value -> charBuffer.append(BASE62CHARS[value]));
    return charBuffer.rewind().toString();
  }
}
