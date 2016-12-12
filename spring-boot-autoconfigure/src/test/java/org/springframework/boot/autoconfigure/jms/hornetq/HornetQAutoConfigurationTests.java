/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.jms.hornetq;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.core.remoting.impl.invm.InVMConnectorFactory;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.hornetq.jms.client.HornetQConnectionFactory;
import org.hornetq.jms.server.config.JMSConfiguration;
import org.hornetq.jms.server.config.JMSQueueConfiguration;
import org.hornetq.jms.server.config.TopicConfiguration;
import org.hornetq.jms.server.config.impl.JMSConfigurationImpl;
import org.hornetq.jms.server.config.impl.JMSQueueConfigurationImpl;
import org.hornetq.jms.server.config.impl.TopicConfigurationImpl;
import org.hornetq.jms.server.embedded.EmbeddedJMS;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.core.SessionCallback;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.jms.support.destination.DynamicDestinationResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Tests for {@link HornetQAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
@Deprecated
@SuppressWarnings("deprecation")
public class HornetQAutoConfigurationTests {

	@Rule
	public final TemporaryFolder folder = new TemporaryFolder();

	private AnnotationConfigApplicationContext context;

	@After
	public void tearDown() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void nativeConnectionFactory() {
		load(EmptyConfiguration.class, "spring.hornetq.mode:native");
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		HornetQConnectionFactory connectionFactory = this.context
				.getBean(HornetQConnectionFactory.class);
		assertThat(connectionFactory).isEqualTo(jmsTemplate.getConnectionFactory());
		assertNettyConnectionFactory(connectionFactory, "localhost", 5445);
	}

	@Test
	public void nativeConnectionFactoryCustomHost() {
		load(EmptyConfiguration.class, "spring.hornetq.mode:native",
				"spring.hornetq.host:192.168.1.144", "spring.hornetq.port:9876");
		HornetQConnectionFactory connectionFactory = this.context
				.getBean(HornetQConnectionFactory.class);
		assertNettyConnectionFactory(connectionFactory, "192.168.1.144", 9876);
	}

	@Test
	public void nativeConnectionFactoryCredentials() throws JMSException {
		load(EmptyConfiguration.class, "spring.hornetq.mode:native",
				"spring.hornetq.user:user", "spring.hornetq.password:secret");
		HornetQConnectionFactory connectionFactory = this.context
				.getBean(HornetQConnectionFactory.class);

		// Validate the secured variant is invoked
		HornetQConnectionFactory testCf = spy(connectionFactory);
		Connection connection = mock(Connection.class);
		doReturn(connection).when(testCf).createConnection("user", "secret");

		Connection actual = testCf.createConnection();
		assertThat(actual).isSameAs(connection);
	}

	@Test
	public void embeddedConnectionFactory() {
		load(EmptyConfiguration.class, "spring.hornetq.mode:embedded");

		HornetQProperties properties = this.context.getBean(HornetQProperties.class);
		assertThat(properties.getMode()).isEqualTo(HornetQMode.EMBEDDED);

		assertThat(this.context.getBeansOfType(EmbeddedJMS.class)).hasSize(1);
		org.hornetq.core.config.Configuration configuration = this.context
				.getBean(org.hornetq.core.config.Configuration.class);
		assertThat(configuration.isPersistenceEnabled()).isFalse();
		assertThat(configuration.isSecurityEnabled()).isFalse();

		HornetQConnectionFactory connectionFactory = this.context
				.getBean(HornetQConnectionFactory.class);
		assertInVmConnectionFactory(connectionFactory);
	}

	@Test
	public void embeddedConnectionFactoryByDefault() {
		// No mode is specified
		load(EmptyConfiguration.class);

		assertThat(this.context.getBeansOfType(EmbeddedJMS.class)).hasSize(1);
		org.hornetq.core.config.Configuration configuration = this.context
				.getBean(org.hornetq.core.config.Configuration.class);
		assertThat(configuration.isPersistenceEnabled()).isFalse();
		assertThat(configuration.isSecurityEnabled()).isFalse();

		HornetQConnectionFactory connectionFactory = this.context
				.getBean(HornetQConnectionFactory.class);
		assertInVmConnectionFactory(connectionFactory);
	}

	@Test
	public void nativeConnectionFactoryIfEmbeddedServiceDisabledExplicitly() {
		// No mode is specified
		load(EmptyConfiguration.class, "spring.hornetq.embedded.enabled:false");

		assertThat(this.context.getBeansOfType(EmbeddedJMS.class)).isEmpty();

		HornetQConnectionFactory connectionFactory = this.context
				.getBean(HornetQConnectionFactory.class);
		assertNettyConnectionFactory(connectionFactory, "localhost", 5445);
	}

	@Test
	public void embeddedConnectionFactoryEvenIfEmbeddedServiceDisabled() {
		// No mode is specified
		load(EmptyConfiguration.class, "spring.hornetq.mode:embedded",
				"spring.hornetq.embedded.enabled:false");

		assertThat(this.context.getBeansOfType(EmbeddedJMS.class)).isEmpty();

		HornetQConnectionFactory connectionFactory = this.context
				.getBean(HornetQConnectionFactory.class);
		assertInVmConnectionFactory(connectionFactory);
	}

	@Test
	public void embeddedServerWithDestinations() {
		load(EmptyConfiguration.class, "spring.hornetq.embedded.queues=Queue1,Queue2",
				"spring.hornetq.embedded.topics=Topic1");

		DestinationChecker checker = new DestinationChecker(this.context);
		checker.checkQueue("Queue1", true);
		checker.checkQueue("Queue2", true);
		checker.checkQueue("QueueDoesNotExist", false);

		checker.checkTopic("Topic1", true);
		checker.checkTopic("TopicDoesNotExist", false);
	}

	@Test
	public void embeddedServerWithDestinationConfig() {
		load(DestinationConfiguration.class);

		DestinationChecker checker = new DestinationChecker(this.context);
		checker.checkQueue("sampleQueue", true);
		checker.checkTopic("sampleTopic", true);
	}

	@Test
	public void embeddedServiceWithCustomJmsConfiguration() {
		// Ignored with custom config
		load(CustomJmsConfiguration.class,
				"spring.hornetq.embedded.queues=Queue1,Queue2");
		DestinationChecker checker = new DestinationChecker(this.context);
		checker.checkQueue("custom", true); // See CustomJmsConfiguration

		checker.checkQueue("Queue1", false);
		checker.checkQueue("Queue2", false);
	}

	@Test
	public void embeddedServiceWithCustomHornetQConfiguration() {
		load(CustomHornetQConfiguration.class);
		org.hornetq.core.config.Configuration configuration = this.context
				.getBean(org.hornetq.core.config.Configuration.class);
		assertThat(configuration.getName()).isEqualTo("customFooBar");
	}

	@Test
	public void embeddedWithPersistentMode() throws IOException, JMSException {
		File dataFolder = this.folder.newFolder();

		// Start the server and post a message to some queue
		load(EmptyConfiguration.class, "spring.hornetq.embedded.queues=TestQueue",
				"spring.hornetq.embedded.persistent:true",
				"spring.hornetq.embedded.dataDirectory:" + dataFolder.getAbsolutePath());

		final String msgId = UUID.randomUUID().toString();
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		jmsTemplate.send("TestQueue", new MessageCreator() {
			@Override
			public Message createMessage(Session session) throws JMSException {
				return session.createTextMessage(msgId);
			}
		});
		this.context.close(); // Shutdown the broker

		// Start the server again and check if our message is still here
		load(EmptyConfiguration.class, "spring.hornetq.embedded.queues=TestQueue",
				"spring.hornetq.embedded.persistent:true",
				"spring.hornetq.embedded.dataDirectory:" + dataFolder.getAbsolutePath());

		JmsTemplate jmsTemplate2 = this.context.getBean(JmsTemplate.class);
		jmsTemplate2.setReceiveTimeout(1000L);
		Message message = jmsTemplate2.receive("TestQueue");
		assertThat(message).isNotNull();
		assertThat(((TextMessage) message).getText()).isEqualTo(msgId);
	}

	@Test
	public void severalEmbeddedBrokers() {
		load(EmptyConfiguration.class, "spring.hornetq.embedded.queues=Queue1");

		AnnotationConfigApplicationContext anotherContext = doLoad(
				EmptyConfiguration.class, "spring.hornetq.embedded.queues=Queue2");

		try {
			HornetQProperties properties = this.context.getBean(HornetQProperties.class);
			HornetQProperties anotherProperties = anotherContext
					.getBean(HornetQProperties.class);
			assertThat(properties.getEmbedded().getServerId() < anotherProperties
					.getEmbedded().getServerId()).isTrue();

			DestinationChecker checker = new DestinationChecker(this.context);
			checker.checkQueue("Queue1", true);
			checker.checkQueue("Queue2", false);

			DestinationChecker anotherChecker = new DestinationChecker(anotherContext);
			anotherChecker.checkQueue("Queue2", true);
			anotherChecker.checkQueue("Queue1", false);
		}
		finally {
			anotherContext.close();
		}
	}

	@Test
	public void connectToASpecificEmbeddedBroker() {
		load(EmptyConfiguration.class, "spring.hornetq.embedded.serverId=93",
				"spring.hornetq.embedded.queues=Queue1");

		AnnotationConfigApplicationContext anotherContext = doLoad(
				EmptyConfiguration.class, "spring.hornetq.mode=embedded",
				"spring.hornetq.embedded.serverId=93", // Connect to the "main" broker
				"spring.hornetq.embedded.enabled=false"); // do not start a specific one

		try {
			DestinationChecker checker = new DestinationChecker(this.context);
			checker.checkQueue("Queue1", true);

			DestinationChecker anotherChecker = new DestinationChecker(anotherContext);
			anotherChecker.checkQueue("Queue1", true);
		}
		finally {
			anotherContext.close();
		}
	}

	private TransportConfiguration assertInVmConnectionFactory(
			HornetQConnectionFactory connectionFactory) {
		TransportConfiguration transportConfig = getSingleTransportConfiguration(
				connectionFactory);
		assertThat(transportConfig.getFactoryClassName())
				.isEqualTo(InVMConnectorFactory.class.getName());
		return transportConfig;
	}

	private TransportConfiguration assertNettyConnectionFactory(
			HornetQConnectionFactory connectionFactory, String host, int port) {
		TransportConfiguration transportConfig = getSingleTransportConfiguration(
				connectionFactory);
		assertThat(transportConfig.getFactoryClassName())
				.isEqualTo(NettyConnectorFactory.class.getName());
		assertThat(transportConfig.getParams().get("host")).isEqualTo(host);
		assertThat(transportConfig.getParams().get("port")).isEqualTo(port);
		return transportConfig;
	}

	private TransportConfiguration getSingleTransportConfiguration(
			HornetQConnectionFactory connectionFactory) {
		TransportConfiguration[] transportConfigurations = connectionFactory
				.getServerLocator().getStaticTransportConfigurations();
		assertThat(transportConfigurations.length).isEqualTo(1);
		return transportConfigurations[0];
	}

	private void load(Class<?> config, String... environment) {
		this.context = doLoad(config, environment);
	}

	private AnnotationConfigApplicationContext doLoad(Class<?> config,
			String... environment) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		applicationContext.register(config);
		applicationContext.register(HornetQAutoConfiguration.class,
				JmsAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(applicationContext, environment);
		applicationContext.refresh();
		return applicationContext;
	}

	private final static class DestinationChecker {

		private final JmsTemplate jmsTemplate;

		private final DestinationResolver destinationResolver;

		private DestinationChecker(ApplicationContext applicationContext) {
			this.jmsTemplate = applicationContext.getBean(JmsTemplate.class);
			this.destinationResolver = new DynamicDestinationResolver();
		}

		public void checkQueue(String name, boolean shouldExist) {
			checkDestination(name, false, shouldExist);
		}

		public void checkTopic(String name, boolean shouldExist) {
			checkDestination(name, true, shouldExist);
		}

		public void checkDestination(final String name, final boolean pubSub,
				final boolean shouldExist) {
			this.jmsTemplate.execute(new SessionCallback<Void>() {
				@Override
				public Void doInJms(Session session) throws JMSException {
					try {
						Destination destination = DestinationChecker.this.destinationResolver
								.resolveDestinationName(session, name, pubSub);
						if (!shouldExist) {
							throw new IllegalStateException("Destination '" + name
									+ "' was not expected but got " + destination);
						}
					}
					catch (JMSException e) {
						if (shouldExist) {
							throw new IllegalStateException("Destination '" + name
									+ "' was expected but got " + e.getMessage());
						}
					}
					return null;
				}
			});
		}
	}

	@Configuration
	protected static class EmptyConfiguration {
	}

	@Configuration
	protected static class DestinationConfiguration {

		@Bean
		JMSQueueConfiguration sampleQueueConfiguration() {
			return new JMSQueueConfigurationImpl("sampleQueue", "foo=bar", false,
					"/queue/1");
		}

		@Bean
		TopicConfiguration sampleTopicConfiguration() {
			return new TopicConfigurationImpl("sampleTopic", "/topic/1");
		}
	}

	@Configuration
	protected static class CustomJmsConfiguration {

		@Bean
		public JMSConfiguration myJmsConfiguration() {
			JMSConfiguration config = new JMSConfigurationImpl();
			config.getQueueConfigurations()
					.add(new JMSQueueConfigurationImpl("custom", null, false));
			return config;
		}
	}

	@Configuration
	protected static class CustomHornetQConfiguration {

		@Bean
		public HornetQConfigurationCustomizer myHornetQCustomize() {
			return new HornetQConfigurationCustomizer() {
				@Override
				public void customize(
						org.hornetq.core.config.Configuration configuration) {
					configuration.setClusterPassword("Foobar");
					configuration.setName("customFooBar");
				}
			};
		}
	}

}
