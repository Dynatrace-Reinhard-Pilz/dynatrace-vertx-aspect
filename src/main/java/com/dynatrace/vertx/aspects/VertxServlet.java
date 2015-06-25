package com.dynatrace.vertx.aspects;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.vertx.java.core.http.impl.DefaultHttpServerRequest;
import org.vertx.java.core.http.impl.DefaultHttpServerResponse;

import com.dynatrace.adk.DynaTraceADKFactory;
import com.dynatrace.adk.Tagging;

/**
 * An artificial Servlet which ensures that an incoming request is being
 * mimicked by a call to a Servlet.
 * 
 * @author reinhard.pilz@dynatrace.com
 *
 */
public class VertxServlet extends HttpServlet {
	
	private static final String TRACE_TAG_FIELD_NAME = "vertxTraceTag";
	
	private static final Logger LOGGER =
			Logger.getLogger(VertxServlet.class.getName());

	private static final long serialVersionUID = 1L;
	
	protected final DefaultHttpServerRequest request;
	protected final DefaultHttpServerResponse response;
	protected final Runnable proceedRunnable;
	
	private static final Map<Class<?>, Field> TRACE_TAG_FIELDS = resolveTraceTagFields();
	
	private static void storeTraceTag(Object o, String traceTag) {
		if (o == null) {
			LOGGER.log(Level.WARNING, "Cannot store trace tag information. No object given");
			return;
		}
		final Class<? extends Object> clazz = o.getClass();
		final Field traceTagField = TRACE_TAG_FIELDS.get(clazz);
		if (traceTagField == null) {
			LOGGER.log(Level.WARNING, "The class '" + clazz.getSimpleName() + "' does not contain a field to store the trace tag information.");
			return;
		}
		try {
			traceTagField.set(o, traceTag);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			LOGGER.log(Level.WARNING, "Unable to store trace tag information on " + clazz.getSimpleName() + ": " + e.getMessage());
		}
	}
	
	private static Map<Class<?>, Field> resolveTraceTagFields() {
		final Map<Class<?>, Field> fieldMap = new HashMap<>();
		fieldMap.put(DefaultHttpServerRequest.class, resolveTraceTagField(DefaultHttpServerRequest.class));
		fieldMap.put(DefaultHttpServerResponse.class, resolveTraceTagField(DefaultHttpServerResponse.class));
		return fieldMap;
	}
	
	private static Field resolveTraceTagField(Class<?> clazz) {
		if (clazz == null) {
			LOGGER.log(Level.WARNING, "Cannot resolve field '" + TRACE_TAG_FIELD_NAME + "'. Parameter clazz was null");
			return null;
		}
		try {
			Field traceTagField = clazz.getDeclaredField(TRACE_TAG_FIELD_NAME);
			if (traceTagField == null) {
				LOGGER.log(Level.WARNING, "No field '" + TRACE_TAG_FIELD_NAME + "' found within class '" + clazz.getSimpleName() + "'");
				return null;
			}
			traceTagField.setAccessible(true);
			return traceTagField;
		} catch (NoSuchFieldException e) {
			LOGGER.log(Level.WARNING, "No field '" + TRACE_TAG_FIELD_NAME + "' found within class '" + clazz.getSimpleName() + "'");
			return null;
		}
	}
	
	public VertxServlet(
			DefaultHttpServerRequest request,
			DefaultHttpServerResponse response,
			Runnable proceedRunnable
	) {
		this.request = request;
		this.response = response;
		this.proceedRunnable = proceedRunnable;
	}
	
	public void handleRequest() {
		Tagging tagging = null;
		String traceTag = null;
		try {
			tagging = DynaTraceADKFactory.createTagging();
			traceTag = tagging.getTagAsString();
			if (traceTag != null) {
				storeTraceTag(this.request, traceTag);
				storeTraceTag(this.response, traceTag);
			}
		} catch (Throwable t) {
			LOGGER.log(Level.WARNING, "Unable to query for tag", t);
		}
		proceedRunnable.run();
		try {
			if ((tagging != null) && (traceTag != null)) {
				tagging.linkClientPurePath(true, traceTag);
			}
		} catch (Throwable t) {
			LOGGER.log(Level.WARNING, "Unable to insert link node", t);
		}
	}
	
	
	@Override
	public ServletConfig getServletConfig() {
		return VertxServletConfig.INSTANCE;
	}
	
	@Override
	public final String getServletName() {
		return VertxServlet.class.getSimpleName();
	}
	
	/**
	 * Any execution of {@link ServerConnection.handleRequest} is being
	 * wrapped by the calling this method, which in turn then invokes
	 * this artificial Servlet's {@code service} method.<br />
	 * <br />
	 * This ensures that the Dynatrace Servlet Sensor can pick up the
	 * request as PurePath
	 * 
	 * @param request the request object
	 * @param runnable a {@link Runnable} which is able to invoke the
	 * 		original business logic to be executed during this request
	 * 		cycle.
	 */
	public final void execute() {
		LOGGER.log(Level.INFO, "execute for " + request.uri());
		final VertxServletRequest req =
				new VertxServletRequest(request);
		final VertxServletResponse res =
				new VertxServletResponse(response);
		try {
			service(req, res);
		} catch (final Throwable t) {
			LOGGER.log(
				Level.WARNING,
				"Unable to simulate Servlet invocation successfully",
				t
			);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse res) {
		handleRequest();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse res) {
		handleRequest();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void doPut(HttpServletRequest req, HttpServletResponse res) {
		handleRequest();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void doDelete(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException
	{
		handleRequest();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void doHead(HttpServletRequest req, HttpServletResponse res) {
		handleRequest();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void doTrace(HttpServletRequest req, HttpServletResponse res) {
		handleRequest();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void doOptions(HttpServletRequest req, HttpServletResponse res) {
		handleRequest();
	}

}

