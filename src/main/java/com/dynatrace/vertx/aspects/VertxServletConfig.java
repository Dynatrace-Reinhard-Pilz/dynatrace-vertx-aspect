package com.dynatrace.vertx.aspects;

import java.util.Collections;
import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

/**
 * An artificial {@link ServletConfig} object in order to mimic
 * Servlet invocations
 * 
 * @author reinhard.pilz@dynatrace.com
 *
 */
public class VertxServletConfig implements ServletConfig {
	
	public static final VertxServletConfig INSTANCE = new VertxServletConfig();
	
	/**
	 * c'tor is private to prevent instantiations from outside.
	 * A single instance is offered publicly.
	 */
	private VertxServletConfig() {
		// prevent instantiation
	}
	
	/**
	 * @return the name of the Servlet whichs invocation is getting mimicked
	 */
	@Override
	public String getServletName() {
		return VertxServlet.class.getSimpleName();
	}
	
	/**
	 * @return always {@code null} as this information is not necessarily
	 * 		required by the dynaTrace Servlet Sensor. 
	 */
	@Override
	public ServletContext getServletContext() {
		return null;
	}
	
	/**
	 * @return always an empty {@link Enumeration} as there is nothing to
	 * 		configure for an artificial Servlet
	 */
	@Override
	public Enumeration<String> getInitParameterNames() {
		return Collections.emptyEnumeration();
	}
	
	/**
	 * @return always {@code null} as there is nothing to configure for an
	 * 		artificial Servlet
	 */
	@Override
	public String getInitParameter(String name) {
		return null;
	}

}
