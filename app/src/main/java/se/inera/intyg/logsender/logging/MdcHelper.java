/*
 * Copyright (C) 2026 Inera AB (http://www.inera.se)
 *
 * This file is part of sklintyg (https://github.com/sklintyg).
 *
 * sklintyg is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * sklintyg is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package se.inera.intyg.logsender.logging;

import java.security.SecureRandom;

public class MdcHelper {

  private static final SecureRandom RNG = new SecureRandom();
  private static final char[] HEX = "0123456789abcdef".toCharArray();

  private MdcHelper() {}

  public static String traceId() {
    final var b = new byte[16];
    do {
      RNG.nextBytes(b);
    } while (isAllZero(b)); // W3C rule
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
