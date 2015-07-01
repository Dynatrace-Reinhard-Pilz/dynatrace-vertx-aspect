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

/**
 * <p>
 * In order to provide the required information about the HTTP request to the
 * dynaTrace Servlet Sensor the internal {@link DefaultHttpServerRequest} object
 * is being wrapped and instead offered as a {@link HttpServletRequest}.
 * </p>
 * <p>
 * Not all methods offered by {@link HttpServletRequest} are required to deliver
 * proper values since they are not being queried by the dynaTrace Servlet
 * Sensor anyways.
 * </p>
 * 
 * @author reinhard.pilz@dynatrace.com
 *
 */
public final class VertxServletRequest implements HttpServletRequest {
	
	/**
	 * A Server Name to offer during Servlet Invocation
	 */
	private static final String SERVER_NAME = "vert-x";
	
	/**
	 * The internal request object of vertx
	 */
	private final DefaultHttpServerRequest request;
	
	/**
	 * c'tor
	 * 
	 * @param request the internal request object of vertx
	 */
	public VertxServletRequest(DefaultHttpServerRequest request) {
		this.request = request;
	}

	/**
	 * @return the request method of the HTTP request (GET, POST, ...)
	 */
	@Override
	public String getMethod() {
		return request.method();
	}
	
	/**
	 * @return always the same server name since there does not exist such a
	 * 		feature of naming the server in vertx
	 */
	@Override
	public String getServerName() {
		return SERVER_NAME;
	}
	
	/**
	 * @return the client IP address of the HTTP request
	 */
	@Override
	public String getRemoteAddr() {
		return request.remoteAddress().getAddress().getHostAddress();
	}

	/**
	 * @return the client host name if applicable of the HTTP request
	 */
	@Override
	public String getRemoteHost() {
		return request.remoteAddress().getHostName();
	}
	
	/**
	 * @return the request URI of the HTTP request
	 */
	@Override
	public String getRequestURI() {
		return request.path();
	}
	
	/**
	 * @return the query string of the HTTP request
	 */
	@Override
	public String getQueryString() {
		return request.query();
	}

	/**
	 * @return the protocol version of the HTTP request, either {@code HTTP/1.1}
	 * 		or {@code HTTP/1.0}
	 */
	@Override
	public String getProtocol() {
		if (request.version() == HttpVersion.HTTP_1_1) {
			return "HTTP/1.1";
		}
		return "HTTP/1.0";
	}

	/**
	 * @return the request header with the given name or {@code null} if no
	 * 		header with the given name has been sent during the HTTP request
	 */
	@Override
	public String getHeader(String name) {
		return request.headers().get(name);
	}
	
	/**
	 * @return all the values of the headers with the given name or an empty
	 * 		{@link Enumeration} if no such head has been sent during the
	 * 		HTTP request
	 */
	@Override
	public Enumeration<String> getHeaders(String name) {
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
	
	
	/**
	 * @return always {@code null} because the internal representation of the
	 * 		HTTP request of vertx does not offer methods to query for cookies.
	 * 		It is possible to parse the request headers manually and produce
	 * 		the {@link Cookie} values here, but it is currently not implemented 
	 */
	@Override
	public Cookie[] getCookies() {
		return null;
	}
	
	/**
	 * @return the URL of the HTTP request
	 */
	@Override
	public StringBuffer getRequestURL() {
		return new StringBuffer(request.uri());
	}

	/**
	 * @return the value of the request parameter passed within the query string
	 * 		with the given name or {@code null} if no such parameter has been
	 * 		passed with this HTTP request. This method will not take POST
	 * 		parameters into considerations because their values are not known
	 * 		until the request body has been parsed, which is not being ensured
	 * 		by vertx once the internal representation of the HTTP request is
	 * 		being created and handed over to the request handlers.
	 */
	@Override
	public String getParameter(String name) {
		return request.params().get(name);
	}

	/**
	 * @return the names of all request parameters passed within the query
	 * 		string of the HTTP request
	 */
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

	/**
	 * @return all values of the request parameters matching the given name
	 * 		passed within the query string of the HTTP request or {@code null}
	 * 		if no such parameter exists
	 */
	@Override
	public String[] getParameterValues(String name) {
		final List<String> values = request.params().getAll(name);
		if (values == null) {
			return null;
		}
		return values.toArray(new String[values.size()]);
	}

	/**
	 * @return the names of all headers passed with the HTTP request
	 */
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
	
	/**
	 * @return always {@code null} because setting request attributes is not
	 * 		supported in vertx
	 */
	@Override
	public Object getAttribute(String name) {
		return null;
	}

	/**
	 * @return always an empty {@link Enumeration} because setting request
	 * 		attributes is not supported in vertx
	 */
	@Override
	public Enumeration<String> getAttributeNames() {
		return Collections.emptyEnumeration();
	}

	/**
	 * @return always {@code null}. There is a chance to implement this method
	 * 		by parsing the HTTP header {@code Content-Encoding} but it is not
	 * 		being queried for by the dynaTrace Servlet Sensor, so there would
	 * 		be no consumer for this value.
	 */
	@Override
	public String getCharacterEncoding() {
		return null;
	}

	/**
	 * ignored
	 */
	@Override
	public void setCharacterEncoding(String env)
			throws UnsupportedEncodingException {
	}

	/**
	 * @return always {@code 0}, not being queried
	 */
	@Override
	public int getContentLength() {
		return 0;
	}

	/**
	 * @return always {@code null}, not being queried
	 */
	@Override
	public String getContentType() {
		return null;
	}

	/**
	 * @return always {@code null}, not being queried
	 */
	@Override
	public ServletInputStream getInputStream() throws IOException {
		return null;
	}

	/**
	 * @return always {@code null}, not being queried
	 */
	@Override
	public Map<String, String[]> getParameterMap() {
		return null;
	}

	/**
	 * @return always {@code null}, not being queried
	 */
	@Override
	public String getScheme() {
		return null;
	}

	/**
	 * @return always {@code 0}, not being queried
	 */
	@Override
	public int getServerPort() {
		return 0;
	}

	/**
	 * @return always {@code null}, not being queried
	 */
	@Override
	public BufferedReader getReader() throws IOException {
		return null;
	}

	/**
	 * ignored because vertx does not support request attributes
	 */
	@Override
	public void setAttribute(String name, Object o) {
	}

	/**
	 * ignored because vertx does not support request attributes
	 */
	@Override
	public void removeAttribute(String name) {
	}

	/**
	 * @return always {@code null}, not being queried
	 */
	@Override
	public Locale getLocale() {
		return null;
	}

	/**
	 * @return always {@code null}, not being queried
	 */
	@Override
	public Enumeration<Locale> getLocales() {
		return null;
	}

	/**
	 * @return always {@code false}, not being queried
	 */
	@Override
	public boolean isSecure() {
		return false;
	}

	/**
	 * @return always {@code null}, not being queried
	 */
	@Override
	public RequestDispatcher getRequestDispatcher(String path) {
		return null;
	}

	/**
	 * @return always {@code null}, not being queried
	 */
	@Override
	public String getRealPath(String path) {
		return null;
	}

	/**
	 * @return always {@code null}, not being queried
	 */
	@Override
	public int getRemotePort() {
		return 0;
	}

	/**
	 * @return always {@code null}, not being queried
	 */
	@Override
	public String getLocalName() {
		return null;
	}

	/**
	 * @return always {@code null}, not being queried
	 */
	@Override
	public String getLocalAddr() {
		return null;
	}

	/**
	 * @return always {@code null}, not being queried
	 */
	@Override
	public int getLocalPort() {
		return 0;
	}

	/**
	 * @return always {@code null}, not being queried
	 */
	@Override
	public ServletContext getServletContext() {
		return null;
	}

	/**
	 * @return always {@code null}, not being queried
	 */
	@Override
	public AsyncContext startAsync() throws IllegalStateException {
		return null;
	}

	/**
	 * @return always {@code null}, not being queried
	 */
	@Override
	public AsyncContext startAsync(ServletRequest servletRequest,
			ServletResponse servletResponse) throws IllegalStateException {
		return null;
	}

	/**
	 * @return always {@code false}, not being queried
	 */
	@Override
	public boolean isAsyncStarted() {
		return false;
	}

	/**
	 * @return always {@code false}, not being queried
	 */
	@Override
	public boolean isAsyncSupported() {
		return false;
	}

	/**
	 * @return always {@code null}, not being queried
	 */
	@Override
	public AsyncContext getAsyncContext() {
		return null;
	}

	/**
	 * @return always {@code null}, not being queried
	 */
	@Override
	public DispatcherType getDispatcherType() {
		return null;
	}

	/**
	 * @return always {@code null}, not being queried
	 */
	@Override
	public String getAuthType() {
		return null;
	}

	/**
	 * @return always {@code null}, not being queried
	 */
	@Override
	public long getDateHeader(String name) {
		return 0;
	}

	/**
	 * @return always {@code null}, not being queried
	 */
	@Override
	public int getIntHeader(String name) {
		return 0;
	}

	/**
	 * @return always {@code null}, not being queried
	 */
	@Override
	public String getPathInfo() {
		return null;
	}

	/**
	 * @return always {@code null}, not being queried
	 */
	@Override
	public String getPathTranslated() {
		return null;
	}

	/**
	 * @return always {@code null}, not being queried
	 */
	@Override
	public String getContextPath() {
		return null;
	}

	/**
	 * @return always {@code null}, not being queried
	 */
	@Override
	public String getRemoteUser() {
		return null;
	}

	/**
	 * @return always {@code false}, not being queried
	 */
	@Override
	public boolean isUserInRole(String role) {
		return false;
	}

	/**
	 * @return always {@code null}, not being queried
	 */
	@Override
	public Principal getUserPrincipal() {
		return null;
	}

	/**
	 * @return always {@code null}, not being queried
	 */
	@Override
	public String getRequestedSessionId() {
		return null;
	}

	/**
	 * @return always {@code null}, not being queried
	 */
	@Override
	public String getServletPath() {
		return null;
	}

	/**
	 * @return always {@code null}, since sessions are not supported by vertx
	 */
	@Override
	public HttpSession getSession(boolean create) {
		return null;
	}

	/**
	 * @return always {@code null}, since sessions are not supported by vertx
	 */
	@Override
	public HttpSession getSession() {
		return null;
	}

	/**
	 * @return always {@code false}, not being queried
	 */
	@Override
	public boolean isRequestedSessionIdValid() {
		return false;
	}

	/**
	 * @return always {@code false}, not being queried
	 */
	@Override
	public boolean isRequestedSessionIdFromCookie() {
		return false;
	}

	/**
	 * @return always {@code false}, not being queried
	 */
	@Override
	public boolean isRequestedSessionIdFromURL() {
		return false;
	}

	/**
	 * @return always {@code false}, not being queried
	 */
	@Override
	public boolean isRequestedSessionIdFromUrl() {
		return false;
	}

	/**
	 * @return always {@code false}, not being queried
	 */
	@Override
	public boolean authenticate(HttpServletResponse response)
			throws IOException, ServletException {
		return false;
	}

	/**
	 * ignored
	 */
	@Override
	public void login(String username, String password) throws ServletException {
	}

	/**
	 * ignored
	 */
	@Override
	public void logout() throws ServletException {
	}

	/**
	 * @return always {@code null}, not being queried
	 */
	@Override
	public Collection<Part> getParts() throws IOException, ServletException {
		return null;
	}

	/**
	 * @return always {@code null}, not being queried
	 */
	@Override
	public Part getPart(String name) throws IOException, ServletException {
		return null;
	}

	/**
	 * @return always {@code 0}, not being queried
	 */
	@Override
	public long getContentLengthLong() {
		return 0;
	}

	/**
	 * @return always {@code null}, not being queried
	 */
	@Override
	public String changeSessionId() {
		return null;
	}

	/**
	 * @return always {@code null}, not being queried
	 */
	@Override
	public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass)
			throws IOException, ServletException {
		return null;
	}

}
