package se.inera.intyg.logsender.logging;

public class MdcLogConstants {

  private MdcLogConstants() {

  }

  public static final String EVENT_ACTION = "event.action";
  public static final String EVENT_CATEGORY = "event.category";
  public static final String EVENT_CATEGORY_API = "[api]";
  public static final String EVENT_CATEGORY_PROCESS = "[process]";
  public static final String EVENT_TYPE = "event.type";
  public static final String EVENT_START = "event.start";
  public static final String EVENT_END = "event.end";
  public static final String EVENT_DURATION = "event.duration";
  public static final String EVENT_CLASS = "event.class";
  public static final String EVENT_METHOD = "event.method";
  public static final String EVENT_OUTCOME = "event.outcome";
  public static final String EVENT_OUTCOME_FAILURE = "failure";
  public static final String EVENT_OUTCOME_SUCCESS = "success";
  public static final String SPAN_ID_KEY = "span.id";
  public static final String TRACE_ID_KEY = "trace.id";

  public static final String EVENT_TYPE_CHANGE = "change";
}
