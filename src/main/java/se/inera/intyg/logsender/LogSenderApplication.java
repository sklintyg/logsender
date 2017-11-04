package se.inera.intyg.logsender;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.component.jms.JmsQueueEndpoint;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

@SpringBootApplication
@EnableJms
@Configuration
@ImportResource({"log-sender-ws-config.xml", "loggtjanst-stub-context.xml"})
@PropertySource("file:${credentials.file}")
public class LogSenderApplication {

    private static final Logger LOG = LoggerFactory.getLogger(LogSenderApplication.class);

    @Autowired
    private CamelContext camelContext;

    @Value("${receiveLogMessageEndpointUri}")
    private String receiveLogMessageEndpointUri;

    @Value("${receiveAggregatedLogMessageEndpointUri}")
    private String receiveAggregatedLogMessageEndpointUri;

    @Value("${logging.queue.name}")
    private String loggingQueueName;

    @Value("${aggregated.logging.queue.name}")
    private String aggregatedLoggingQueueName;

    @Value("${activemq.broker.url}")
    private String activeMqBrokerUrl;

    public static void main(String[] args) {
        int verNo = 11;
        SpringApplication.run(LogSenderApplication.class, args);
        LOG.info("LogSenderApplication v{} started", verNo);
    }


    @Bean
    JmsQueueEndpoint receiveLogMessageEndpoint() {
        JmsQueueEndpoint endpoint = camelContext.getEndpoint(receiveLogMessageEndpointUri, JmsQueueEndpoint.class);
        endpoint.setConnectionFactory(connectionFactory());
        endpoint.setDestinationName(loggingQueueName);
        return endpoint;
    }

    @Bean
    JmsQueueEndpoint receiveAggregatedLogMessageEndpoint() {
        JmsQueueEndpoint endpoint = camelContext.getEndpoint(receiveAggregatedLogMessageEndpointUri, JmsQueueEndpoint.class);
        endpoint.setConnectionFactory(connectionFactory());
        endpoint.setDestinationName(aggregatedLoggingQueueName);
        return endpoint;
    }

    @Bean
    public JmsTransactionManager jmsTransactionManager() {
        return new JmsTransactionManager(connectionFactory());
    }

    @Bean
    public ConnectionFactory connectionFactory() {
        return new ActiveMQConnectionFactory(activeMqBrokerUrl);
    }

    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) throws JMSException {
        return new JmsTemplate(connectionFactory);
    }

    @Bean
    public TomcatEmbeddedServletContainerFactory tomcatFactory() {
        return new TomcatEmbeddedServletContainerFactory() {

            @Override
            protected TomcatEmbeddedServletContainer getTomcatEmbeddedServletContainer(
                    Tomcat tomcat) {
                tomcat.enableNaming();
                return super.getTomcatEmbeddedServletContainer(tomcat);
            }

            @Override
            protected void postProcessContext(Context context) {

            }
        };
    }

    @Bean
    public ServletRegistrationBean cxfServlet() {
        CXFServlet cxfServlet = new CXFServlet();
        ServletRegistrationBean bean = new ServletRegistrationBean(
                cxfServlet, "/cxf/*");
        bean.setLoadOnStartup(1);
        return bean;
    }
}
