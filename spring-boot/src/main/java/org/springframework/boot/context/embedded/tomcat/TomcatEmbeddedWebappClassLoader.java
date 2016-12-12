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

package org.springframework.boot.context.embedded.tomcat;

import java.net.URL;

import org.apache.catalina.loader.WebappClassLoader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Extension of Tomcat's {@link WebappClassLoader} that does not consider the
 * {@link ClassLoader#getSystemClassLoader() system classloader}. This is required to to
 * ensure that any custom context classloader is always used (as is the case with some
 * executable archives).
 *
 * @author Phillip Webb
 */
public class TomcatEmbeddedWebappClassLoader extends WebappClassLoader {

	private static final Log logger = LogFactory
			.getLog(TomcatEmbeddedWebappClassLoader.class);

	public TomcatEmbeddedWebappClassLoader() {
		super();
	}

	public TomcatEmbeddedWebappClassLoader(ClassLoader parent) {
		super(parent);
	}

	@Override
	public synchronized Class<?> loadClass(String name, boolean resolve)
			throws ClassNotFoundException {
		Class<?> resultClass = null;

		// Check local class caches
		resultClass = (resultClass == null ? findLoadedClass0(name) : resultClass);
		resultClass = (resultClass == null ? findLoadedClass(name) : resultClass);
		if (resultClass != null) {
			return resolveIfNecessary(resultClass, resolve);
		}

		// Check security
		checkPackageAccess(name);

		// Perform the actual load
		boolean delegateLoad = (this.delegate || filter(name, true));

		if (delegateLoad) {
			resultClass = (resultClass == null ? loadFromParent(name) : resultClass);
		}
		resultClass = (resultClass == null ? findClassIgnoringNotFound(name)
				: resultClass);
		if (!delegateLoad) {
			resultClass = (resultClass == null ? loadFromParent(name) : resultClass);
		}

		if (resultClass == null) {
			throw new ClassNotFoundException(name);
		}

		return resolveIfNecessary(resultClass, resolve);
	}

	private Class<?> resolveIfNecessary(Class<?> resultClass, boolean resolve) {
		if (resolve) {
			resolveClass(resultClass);
		}
		return (resultClass);
	}

	@Override
	protected void addURL(URL url) {
		// Ignore URLs added by the Tomcat 8 implementation (see gh-919)
		if (logger.isTraceEnabled()) {
			logger.trace("Ignoring request to add " + url + " to the tomcat classloader");
		}
	}

	private Class<?> loadFromParent(String name) {
		if (this.parent == null) {
			return null;
		}
		try {
			return Class.forName(name, false, this.parent);
		}
		catch (ClassNotFoundException ex) {
			return null;
		}
	}

	private Class<?> findClassIgnoringNotFound(String name) {
		try {
			return findClass(name);
		}
		catch (ClassNotFoundException ex) {
			return null;
		}
	}

	private void checkPackageAccess(String name) throws ClassNotFoundException {
		if (this.securityManager != null && name.lastIndexOf('.') >= 0) {
			try {
				this.securityManager
						.checkPackageAccess(name.substring(0, name.lastIndexOf('.')));
			}
			catch (SecurityException ex) {
				throw new ClassNotFoundException("Security Violation, attempt to use "
						+ "Restricted Class: " + name, ex);
			}
		}
	}

}
