package se.inera.intyg.logsender;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.UncheckedIOException;
import se.inera.intyg.logsender.helper.TestDataHelper;
import se.inera.intyg.logsender.helper.ValueInclude;
import se.inera.intyg.logsender.model.ActivityType;
import se.inera.intyg.logsender.model.PdlLogMessage;

public class TestDataMessage {

  private static final ObjectMapper OBJECT_MAPPER = build();

  private TestDataMessage() {
    throw new IllegalStateException("Utility class");
  }


  private static ObjectMapper build() {
    final var om = new ObjectMapper();
    om.registerModule(new JavaTimeModule());
    return om;
  }

  public static String toJson(PdlLogMessage message) {
    try {
      return OBJECT_MAPPER.writeValueAsString(message);
    } catch (JsonProcessingException e) {
      throw new UncheckedIOException(e);
    }
  }


  public static void createMessage() {
    PdlLogMessage pdlLogMessage =
        TestDataHelper.buildBasePdlLogMessage(ActivityType.CREATE, ValueInclude.INCLUDE,
            ValueInclude.INCLUDE);


  }


}
