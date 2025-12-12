package se.inera.intyg.logsender.integrationtest.util;

import org.testcontainers.activemq.ActiveMQContainer;

public class Containers {

  public static ActiveMQContainer amqContainer;

  public static void ensureRunning() {
    amqContainer();
  }

  private static void amqContainer() {
    if (amqContainer == null) {
      amqContainer = new ActiveMQContainer("apache/activemq-classic:5.18.3")
          .withUser("activemqUser")
          .withPassword("activemqPassword");
    }

    if (!amqContainer.isRunning()) {
      amqContainer.start();
    }

    System.setProperty("spring.activemq.user", amqContainer.getUser());
    System.setProperty("spring.activemq.password", amqContainer.getPassword());
    System.setProperty("spring.activemq.broker-url",
        withRedeliveryPolicy(amqContainer.getBrokerUrl())
    );
  }


  private static String withRedeliveryPolicy(String brokerUrl) {
    return brokerUrl + "?jms.nonBlockingRedelivery=true"
        + "&jms.redeliveryPolicy.maximumRedeliveries=1"
        + "&jms.redeliveryPolicy.initialRedeliveryDelay=100"
        + "&jms.redeliveryPolicy.useExponentialBackOff=true"
        + "&jms.redeliveryPolicy.backOffMultiplier=2"
        + "&jms.redeliveryPolicy.maximumRedeliveryDelay=500";
  }
}

