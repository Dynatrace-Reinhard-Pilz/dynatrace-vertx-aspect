package com.dynatrace.vertx.aspects;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.vertx.java.core.http.impl.DefaultHttpServerRequest;
import org.vertx.java.core.http.impl.DefaultHttpServerResponse;

/**
 * An artificial Servlet which ensures that an incoming request is being
 * mimicked by a call to a Servlet.
 * 
 * @author reinhard.pilz@dynatrace.com
 *
 */
public class VertxServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	
	private static final String ERRMSG_SERVICE =
			"Unable to simulate Servlet invocation successfully";
	
	private static final Logger LOGGER =
			Logger.getLogger(VertxServlet.class.getName());
	
	protected final DefaultHttpServerRequest request;
	protected final DefaultHttpServerResponse response;
	protected final Runnable proceedRunnable;
	
	public VertxServlet(
			DefaultHttpServerRequest request,
			DefaultHttpServerResponse response,
			Runnable proceedRunnable
	) {
		this.request = request;
		this.response = response;
		this.proceedRunnable = proceedRunnable;
	}

	/**
	 * @return always {@link VertxServletConfig#INSTANCE} because this Servlet
	 * 		has not really been configured in any ways
	 */
	@Override
	public ServletConfig getServletConfig() {
		return VertxServletConfig.INSTANCE;
	}

	/**
	 * @return always the simple class name of {@link VertxServlet}
	 */
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
		try {
			service(
				new VertxServletRequest(request),
				VertxServletResponse.INSTANCE
			);
		} catch (Throwable throwable) {
			LOGGER.log(Level.WARNING, ERRMSG_SERVICE, throwable);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse res) {
		proceedRunnable.run();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse res) {
		proceedRunnable.run();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void doPut(HttpServletRequest req, HttpServletResponse res) {
		proceedRunnable.run();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void doDelete(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException
	{
		proceedRunnable.run();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void doHead(HttpServletRequest req, HttpServletResponse res) {
		proceedRunnable.run();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void doTrace(HttpServletRequest req, HttpServletResponse res) {
		proceedRunnable.run();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void doOptions(HttpServletRequest req, HttpServletResponse res) {
		proceedRunnable.run();
	}

}

