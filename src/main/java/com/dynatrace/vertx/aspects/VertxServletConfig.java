package com.dynatrace.vertx.aspects;

import java.util.Collections;
import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

public class VertxServletConfig implements ServletConfig {
	
	public static final VertxServletConfig INSTANCE = new VertxServletConfig();
	
	private VertxServletConfig() {
		// prevent instantiation
	}
	
	@Override
	public String getServletName() {
		return VertxServlet.class.getSimpleName();
	}
	
	@Override
	public ServletContext getServletContext() {
		return null;
	}
	
	@Override
	public Enumeration<String> getInitParameterNames() {
		return Collections.emptyEnumeration();
	}
	
	@Override
	public String getInitParameter(String name) {
		return null;
	}

}
