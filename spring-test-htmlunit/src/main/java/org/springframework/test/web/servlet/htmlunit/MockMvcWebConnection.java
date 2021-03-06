/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.test.web.servlet.htmlunit;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.htmlunit.geb.GebSpecTestExecutionListener;
import org.springframework.test.web.servlet.htmlunit.webdriver.MockMvcHtmlUnitDriver;
import org.springframework.util.Assert;

import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.WebConnection;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;

import javax.servlet.http.HttpServletRequest;

/**
 * <p>
 * Allows {@link MockMvc} to transform a {@link WebRequest} into a {@link WebResponse}. This is the core integration
 * with <a href="http://htmlunit.sourceforge.net/">HTML Unit</a>.
 * </p>
 * <p>
 * Example usage can be seen below:
 * </p>
 *
 * <pre>
 * MockMvc mockMvc = ...
 * MockMvcWebConnection webConnection = new MockMvcWebConnection(mockMvc);
 * WebClient webClient = new WebClient();
 * webClient.setWebConnection(webConnection);
 *
 * ... use webClient as normal ...
 * </pre>
 *
 * @author Rob Winch
 * @see MockMvcHtmlUnitDriver
 * @see GebSpecTestExecutionListener
 */
public final class MockMvcWebConnection implements WebConnection {
	private final CookieManager cookieManager;

	private final Map<String, MockHttpSession> sessions = new HashMap<String, MockHttpSession>();

	private final MockMvc mockMvc;

	private final String contextPath;

	/**
	 * Creates a new instance that assumes that the first path segment is the context root of the application. For example,
	 * the URL http://localhost/context/test/this would use /context as the context root.
	 *
	 * @param mockMvc the MockMvc instance to use
	 */
	public MockMvcWebConnection(MockMvc mockMvc) {
		this(mockMvc,null);
	}

	/**
	 * Creates a new instance with a specified context root.
	 *
	 * @param mockMvc the MockMvc instance to use
	 * @param contextPath the contextPath to use. The value may be null in which case the first path segment of the URL is turned
	 * into the contextPath. Otherwise it must conform to {@link HttpServletRequest#getContextPath()} which states it
	 * can be empty string or it must start with a "/" and not end in a "/".
	 */
	public MockMvcWebConnection(MockMvc mockMvc, String contextPath) {
		Assert.notNull(mockMvc, "mockMvc cannot be null");
		validateContextPath(contextPath);

		this.mockMvc = mockMvc;
		this.cookieManager = new CookieManager();
		this.contextPath = contextPath;
	}

	public WebResponse getResponse(WebRequest webRequest) throws IOException {
		long startTime = System.currentTimeMillis();
		HtmlUnitRequestBuilder requestBuilder = new HtmlUnitRequestBuilder(sessions, cookieManager, webRequest);
		requestBuilder.setContextPath(contextPath);

		ResultActions resultActions;
		try {
			resultActions = mockMvc.perform(requestBuilder);
		}
		catch (Exception e) {
			throw (IOException) new IOException(e.getMessage()).initCause(e);
		}

		MockHttpServletResponse httpServletResponse = resultActions.andReturn().getResponse();

		return new MockWebResponseBuilder(startTime, webRequest, httpServletResponse).build();
	}

	/**
	 * Performs validation on the contextPath
	 *
	 * @param contextPath the contextPath to validate
	 */
	private static void validateContextPath(String contextPath) {
		if (contextPath == null || "".equals(contextPath)) {
			return;
		}
		if (contextPath.endsWith("/")) {
			throw new IllegalArgumentException("contextPath cannot end with /. Got '" + contextPath + "'");
		}
		if (!contextPath.startsWith("/")) {
			throw new IllegalArgumentException("contextPath must start with /. Got '" + contextPath + "'");
		}
	}
}