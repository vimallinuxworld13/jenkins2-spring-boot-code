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

package org.springframework.boot.autoconfigure.transaction.jta;

import java.io.File;

import javax.jms.Message;
import javax.transaction.TransactionManager;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.jndi.BitronixContext;

import org.springframework.boot.ApplicationHome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jta.XAConnectionFactoryWrapper;
import org.springframework.boot.jta.XADataSourceWrapper;
import org.springframework.boot.jta.bitronix.BitronixDependentBeanFactoryPostProcessor;
import org.springframework.boot.jta.bitronix.BitronixXAConnectionFactoryWrapper;
import org.springframework.boot.jta.bitronix.BitronixXADataSourceWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.util.StringUtils;

/**
 * JTA Configuration for <A href="http://docs.codehaus.org/display/BTM/Home">Bitronix</A>.
 *
 * @author Josh Long
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.2.0
 */
@Configuration
@ConditionalOnClass({ JtaTransactionManager.class, BitronixContext.class })
@ConditionalOnMissingBean(PlatformTransactionManager.class)
class BitronixJtaConfiguration {

	private final JtaProperties jtaProperties;

	BitronixJtaConfiguration(JtaProperties jtaProperties) {
		this.jtaProperties = jtaProperties;
	}

	@Bean
	@ConditionalOnMissingBean
	@ConfigurationProperties("spring.jta.bitronix.properties")
	public bitronix.tm.Configuration bitronixConfiguration() {
		bitronix.tm.Configuration config = TransactionManagerServices.getConfiguration();
		if (StringUtils.hasText(this.jtaProperties.getTransactionManagerId())) {
			config.setServerId(this.jtaProperties.getTransactionManagerId());
		}
		File logBaseDir = getLogBaseDir();
		config.setLogPart1Filename(new File(logBaseDir, "part1.btm").getAbsolutePath());
		config.setLogPart2Filename(new File(logBaseDir, "part2.btm").getAbsolutePath());
		config.setDisableJmx(true);
		return config;
	}

	private File getLogBaseDir() {
		if (StringUtils.hasLength(this.jtaProperties.getLogDir())) {
			return new File(this.jtaProperties.getLogDir());
		}
		File home = new ApplicationHome().getDir();
		return new File(home, "transaction-logs");
	}

	@Bean
	@ConditionalOnMissingBean(TransactionManager.class)
	public BitronixTransactionManager bitronixTransactionManager(
			bitronix.tm.Configuration configuration) {
		// Inject configuration to force ordering
		return TransactionManagerServices.getTransactionManager();
	}

	@Bean
	@ConditionalOnMissingBean(XADataSourceWrapper.class)
	public BitronixXADataSourceWrapper xaDataSourceWrapper() {
		return new BitronixXADataSourceWrapper();
	}

	@Bean
	@ConditionalOnMissingBean
	public static BitronixDependentBeanFactoryPostProcessor bitronixDependentBeanFactoryPostProcessor() {
		return new BitronixDependentBeanFactoryPostProcessor();
	}

	@Bean
	public JtaTransactionManager transactionManager(
			TransactionManager transactionManager) {
		return new JtaTransactionManager(transactionManager);
	}

	@ConditionalOnClass(Message.class)
	static class BitronixJtaJmsConfiguration {

		@Bean
		@ConditionalOnMissingBean(XAConnectionFactoryWrapper.class)
		public BitronixXAConnectionFactoryWrapper xaConnectionFactoryWrapper() {
			return new BitronixXAConnectionFactoryWrapper();
		}
	}
}
