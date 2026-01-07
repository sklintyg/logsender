package se.inera.intyg.logsender.logging;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class MdcHelperTest {

  @Test
  void shouldGenerateTraceId() {
    final var result = MdcHelper.traceId();
    assertNotNull(result);
  }

  @Test
  void shouldGenerateSpanId() {
    final var result = MdcHelper.spanId();
    assertNotNull(result);
  }
}
