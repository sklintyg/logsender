package se.inera.intyg.logsender.integrationtest.util;

import org.testcontainers.activemq.ActiveMQContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

public class Containers {

  private static final Network network = Network.newNetwork();

  public static ActiveMQContainer amqContainer;
  public static GenericContainer<?> redisContainer;

  public static void ensureRunning() {
    redisContainer();
    amqContainer();
  }

  public static void stopAll() {
    if (amqContainer != null && amqContainer.isRunning()) {
      amqContainer.stop();
    }
    if (redisContainer != null && redisContainer.isRunning()) {
      redisContainer.stop();
    }
  }

  private static void amqContainer() {
    if (amqContainer == null) {
      amqContainer = new ActiveMQContainer("apache/activemq-classic:5.18.3")
          .withNetwork(network)
          .withNetworkAliases("activemq")
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

  private static void redisContainer() {
    if (redisContainer == null) {
      redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
          .withNetwork(network)
          .withNetworkAliases("redis")
          .withExposedPorts(6379)
          .withCommand("redis-server", "--requirepass", "redis");
    }

    if (!redisContainer.isRunning()) {
      redisContainer.start();
    }

    System.setProperty("spring.data.redis.host", redisContainer.getHost());
    System.setProperty("spring.data.redis.port",
        String.valueOf(redisContainer.getMappedPort(6379)));
    System.setProperty("spring.data.redis.password", "redis");
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
