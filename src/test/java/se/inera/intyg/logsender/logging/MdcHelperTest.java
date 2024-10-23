package se.inera.intyg.logsender.logging;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MdcHelperTest {

  private MdcHelper mdcHelper;

  @BeforeEach
  void setUp() {
    mdcHelper = new MdcHelper();
  }

    @Test
    void shouldGenerateTraceId() {
      final var result = mdcHelper.traceId();
      assertNotNull(result);
    }

    @Test
    void shouldGenerateSpanId() {
      final var result = mdcHelper.spanId();
      assertNotNull(result);
    }
}
