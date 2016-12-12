/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.freemarker;

import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProvider;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;

/**
 * {@link TemplateAvailabilityProvider} that provides availability information for
 * FreeMarker view templates.
 *
 * @author Andy Wilkinson
 * @since 1.1.0
 */
public class FreeMarkerTemplateAvailabilityProvider
		implements TemplateAvailabilityProvider {

	@Override
	public boolean isTemplateAvailable(String view, Environment environment,
			ClassLoader classLoader, ResourceLoader resourceLoader) {
		if (ClassUtils.isPresent("freemarker.template.Configuration", classLoader)) {
			RelaxedPropertyResolver resolver = new RelaxedPropertyResolver(environment,
					"spring.freemarker.");
			String loaderPath = resolver.getProperty("template-loader-path",
					FreeMarkerProperties.DEFAULT_TEMPLATE_LOADER_PATH);
			String prefix = resolver.getProperty("prefix",
					FreeMarkerProperties.DEFAULT_PREFIX);
			String suffix = resolver.getProperty("suffix",
					FreeMarkerProperties.DEFAULT_SUFFIX);
			return resourceLoader.getResource(loaderPath + prefix + view + suffix)
					.exists();
		}
		return false;
	}

}
