package se.inera.intyg.logsender.logging;

import java.nio.CharBuffer;
import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import org.springframework.stereotype.Component;

@Component
public class MdcHelper {

  private static final SecureRandom RNG = new SecureRandom();
  private static final char[] HEX = "0123456789abcdef".toCharArray();

  private MdcHelper() {
  }

  public static String traceId() {
    final var b = new byte[16];
    do {
      RNG.nextBytes(b);
    } while (isAllZero(b));  // W3C rule
    return toHex(b);
  }

  public static String spanId() {
    final var b = new byte[8];
    do {
      RNG.nextBytes(b);
    } while (isAllZero(b));
    return toHex(b);
  }

  private static boolean isAllZero(byte[] a) {
    for (byte v : a) {
      if (v != 0) {
        return false;
      }
    }
    return true;
  }

  private static String toHex(byte[] bytes) {
    char[] out = new char[bytes.length * 2];
    int i = 0;
    for (byte b : bytes) {
      out[i++] = HEX[(b >>> 4) & 0xF];
      out[i++] = HEX[b & 0xF];
    }
    return new String(out);
  }
}
