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

package org.springframework.boot.test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.test.context.BootstrapWith;

/**
 * Test class annotation signifying that the tests are "web integration tests" and
 * therefore require full startup in the same way as a production application (listening
 * on normal ports). Normally used in conjunction with
 * {@code @SpringApplicationConfiguration},
 * <p>
 * This annotation can be used as an alternative to {@code @IntegrationTest} and
 * {@code @WebAppConfiguration}.
 *
 * @author Phillip Webb
 * @since 1.2.1
 * @see IntegrationTest
 * @deprecated as of 1.4 in favor of
 * {@link org.springframework.boot.test.context.SpringBootTest} with
 * {@code webEnvironment=RANDOM_PORT} or {@code webEnvironment=DEFINED_PORT}.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@BootstrapWith(WebAppIntegrationTestContextBootstrapper.class)
@Deprecated
public @interface WebIntegrationTest {

	/**
	 * Properties in form {@literal key=value} that should be added to the Spring
	 * {@link Environment} before the test runs.
	 * @return properties to add to the context
	 */
	String[] value() default {};

	/**
	 * Convenience attribute that can be used to set a {@code server.port=0}
	 * {@link Environment} property which usually triggers listening on a random port.
	 * Often used in conjunction with a {@link LocalServerPort} injected field on the
	 * test.
	 * @return if a random port should be used
	 */
	boolean randomPort() default false;

}
