package com.dynatrace.vertx.aspects;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

import org.vertx.java.core.MultiMap;
import org.vertx.java.core.http.HttpVersion;
import org.vertx.java.core.http.impl.DefaultHttpServerRequest;

public final class VertxServletRequest implements HttpServletRequest {
	
	private static final String SERVER_NAME = "vert-x";
	
	private final DefaultHttpServerRequest request;
	
	public VertxServletRequest(final DefaultHttpServerRequest request) {
		this.request = request;
	}

	@Override
	public final String getMethod() {
		return request.method();
	}
	
	@Override
	public final String getServerName() {
		return SERVER_NAME;
	}
	
	@Override
	public final String getRemoteAddr() {
		return request.remoteAddress().getAddress().getHostAddress();
	}

	@Override
	public final String getRemoteHost() {
		return request.remoteAddress().getHostName();
	}
	
	@Override
	public final String getRequestURI() {
		return request.path();
	}
	
	@Override
	public final String getQueryString() {
		return request.query();
	}

	@Override
	public final String getProtocol() {
		return request.version() == HttpVersion.HTTP_1_1 ? "HTTP/1.1" : "HTTP/1.0";
	}

	@Override
	public final String getHeader(final String name) {
		return request.headers().get(name);
	}
	
	@Override
	public Enumeration<String> getHeaders(final String name) {
		final MultiMap headers = request.headers();
		if (headers == null) {
			return Collections.emptyEnumeration();
		}
		final List<String> allValues = headers.getAll(name);
		if (allValues == null) {
			return Collections.emptyEnumeration();
		}
		final Iterator<String> it = allValues.iterator();
		if (it == null) {
			return null;
		}
		return new Enumeration<String>() {

			@Override
			public final boolean hasMoreElements() {
				return it.hasNext();
			}

			@Override
			public final String nextElement() {
				return it.next();
			}
		};	}
	
	
	@Override
	public Cookie[] getCookies() {
		return null;
	}
	
	@Override
	public StringBuffer getRequestURL() {
		return new StringBuffer(request.uri());
	}

	@Override
	public String getParameter(String name) {
		return request.params().get(name);
	}

	@Override
	public Enumeration<String> getParameterNames() {
		final Iterator<Entry<String, String>> it = request.params().iterator();
		return new Enumeration<String>() {

			@Override
			public boolean hasMoreElements() {
				return it.hasNext();
			}

			@Override
			public String nextElement() {
				return it.next().getKey();
			}
		};
	}

	@Override
	public String[] getParameterValues(String name) {
		final List<String> values = request.params().getAll(name);
		if (values == null) {
			return null;
		}
		return values.toArray(new String[values.size()]);
	}


	@Override
	public Enumeration<String> getHeaderNames() {
		final Iterator<Entry<String, String>> iterator = request.headers().iterator();
		return new Enumeration<String>() {

			@Override
			public boolean hasMoreElements() {
				return iterator.hasNext();
			}

			@Override
			public String nextElement() {
				return iterator.next().getKey();
			}
		};
	}
	
	@Override
	public Object getAttribute(String name) {
		return null;
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		return null;
	}

	@Override
	public String getCharacterEncoding() {
		return null;
	}

	@Override
	public void setCharacterEncoding(String env)
			throws UnsupportedEncodingException {
	}

	@Override
	public int getContentLength() {
		return 0;
	}

	@Override
	public String getContentType() {
		return null;
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		return null;
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		return null;
	}

	@Override
	public String getScheme() {
		return null;
	}

	@Override
	public int getServerPort() {
		return 0;
	}

	@Override
	public BufferedReader getReader() throws IOException {
		return null;
	}

	@Override
	public void setAttribute(String name, Object o) {
	}

	@Override
	public void removeAttribute(String name) {
	}

	@Override
	public Locale getLocale() {
		return null;
	}

	@Override
	public Enumeration<Locale> getLocales() {
		return null;
	}

	@Override
	public boolean isSecure() {
		return false;
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String path) {
		return null;
	}

	@Override
	public String getRealPath(String path) {
		return null;
	}

	@Override
	public int getRemotePort() {
		return 0;
	}

	@Override
	public String getLocalName() {
		return null;
	}

	@Override
	public String getLocalAddr() {
		return null;
	}

	@Override
	public int getLocalPort() {
		return 0;
	}

	@Override
	public ServletContext getServletContext() {
		return null;
	}

	@Override
	public AsyncContext startAsync() throws IllegalStateException {
		return null;
	}

	@Override
	public AsyncContext startAsync(ServletRequest servletRequest,
			ServletResponse servletResponse) throws IllegalStateException {
		return null;
	}

	@Override
	public boolean isAsyncStarted() {
		return false;
	}

	@Override
	public boolean isAsyncSupported() {
		return false;
	}

	@Override
	public AsyncContext getAsyncContext() {
		return null;
	}

	@Override
	public DispatcherType getDispatcherType() {
		return null;
	}

	@Override
	public String getAuthType() {
		return null;
	}

	@Override
	public long getDateHeader(String name) {
		return 0;
	}

	@Override
	public int getIntHeader(String name) {
		return 0;
	}

	@Override
	public String getPathInfo() {
		return null;
	}

	@Override
	public String getPathTranslated() {
		return null;
	}

	@Override
	public String getContextPath() {
		return null;
	}

	@Override
	public String getRemoteUser() {
		return null;
	}

	@Override
	public boolean isUserInRole(String role) {
		return false;
	}

	@Override
	public Principal getUserPrincipal() {
		return null;
	}

	@Override
	public String getRequestedSessionId() {
		return null;
	}

	@Override
	public String getServletPath() {
		return null;
	}

	@Override
	public HttpSession getSession(boolean create) {
		return null;
	}

	@Override
	public HttpSession getSession() {
		return null;
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		return false;
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		return false;
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		return false;
	}

	@Override
	public boolean isRequestedSessionIdFromUrl() {
		return false;
	}

	@Override
	public boolean authenticate(HttpServletResponse response)
			throws IOException, ServletException {
		return false;
	}

	@Override
	public void login(String username, String password) throws ServletException {
	}

	@Override
	public void logout() throws ServletException {
	}

	@Override
	public Collection<Part> getParts() throws IOException, ServletException {
		return null;
	}

	@Override
	public Part getPart(String name) throws IOException, ServletException {
		return null;
	}

	@Override
	public long getContentLengthLong() {
		return 0;
	}

	@Override
	public String changeSessionId() {
		return null;
	}

	@Override
	public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass)
			throws IOException, ServletException {
		return null;
	}

}
