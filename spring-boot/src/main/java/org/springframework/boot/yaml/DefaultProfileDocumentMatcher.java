/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.yaml;

import java.util.Properties;

import org.springframework.beans.factory.config.YamlProcessor.DocumentMatcher;
import org.springframework.beans.factory.config.YamlProcessor.MatchStatus;

/**
 * A {@link DocumentMatcher} that matches the default profile implicitly but not
 * explicitly (i.e. matches if "spring.profiles" is not found and not otherwise).
 *
 * @author Dave Syer
 */
public class DefaultProfileDocumentMatcher implements DocumentMatcher {

	@Override
	public MatchStatus matches(Properties properties) {
		if (!properties.containsKey("spring.profiles")) {
			return MatchStatus.FOUND;
		}
		return MatchStatus.NOT_FOUND;
	}

}
