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
package se.inera.intyg.logsender.integrationtest.util;

import org.testcontainers.activemq.ActiveMQContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class Containers {

  public static ActiveMQContainer amqContainer;
  public static GenericContainer<?> redisContainer;

  public static void ensureRunning() {
    amqContainer();
    redisContainer();
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
      amqContainer =
          new ActiveMQContainer("apache/activemq-classic:5.18.3")
              .withUser("activemqUser")
              .withPassword("activemqPassword");
    }

    if (!amqContainer.isRunning()) {
      amqContainer.start();
    }

    System.setProperty("spring.activemq.user", amqContainer.getUser());
    System.setProperty("spring.activemq.password", amqContainer.getPassword());
    System.setProperty(
        "spring.activemq.broker-url", withRedeliveryPolicy(amqContainer.getBrokerUrl()));
  }

  private static void redisContainer() {
    if (redisContainer == null) {
      redisContainer =
          new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
              .withExposedPorts(6379)
              .withCommand("redis-server", "--requirepass", "redis");
    }

    if (!redisContainer.isRunning()) {
      redisContainer.start();
    }

    System.setProperty("spring.data.redis.host", redisContainer.getHost());
    System.setProperty(
        "spring.data.redis.port", String.valueOf(redisContainer.getMappedPort(6379)));
    System.setProperty("spring.data.redis.password", "redis");
  }

  private static String withRedeliveryPolicy(String brokerUrl) {
    return brokerUrl
        + "?jms.nonBlockingRedelivery=true"
        + "&jms.redeliveryPolicy.maximumRedeliveries=1"
        + "&jms.redeliveryPolicy.initialRedeliveryDelay=100"
        + "&jms.redeliveryPolicy.useExponentialBackOff=true"
        + "&jms.redeliveryPolicy.backOffMultiplier=2"
        + "&jms.redeliveryPolicy.maximumRedeliveryDelay=500";
  }
}
