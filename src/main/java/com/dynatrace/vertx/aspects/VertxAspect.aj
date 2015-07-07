package com.dynatrace.vertx.aspects;

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.eventbus.impl.BaseMessage;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.impl.DefaultHttpClient;
import org.vertx.java.core.http.impl.DefaultHttpClientRequest;
import org.vertx.java.core.http.impl.DefaultHttpServerRequest;
import org.vertx.java.core.http.impl.DefaultHttpServerResponse;
import org.vertx.java.core.impl.DefaultContext;
import org.vertx.java.core.net.impl.ServerID;

import com.dynatrace.adk.DynaTraceADKFactory;
import com.dynatrace.adk.Tagging;

/**
 * Ensures that calls to Vert-x also performing the necessary calls in order
 * for the Dynatrace Servlet Sensor to pick up these calls out of the box
 * without having to define custom entry points.<br />
 * <br />
 * Furthermore, since the nature of request handling is being dealt with when
 * using Vert-x asynchronously, the Dynatrace Agent SDK is being used to create
 * proper sub paths.
 * 
 * @author reinhard.pilz@dynatrace.com
 *
 */
@SuppressWarnings("rawtypes")
public privileged aspect VertxAspect {
	
	private static final Logger LOGGER =
			Logger.getLogger(VertxAspect.class.getName());
	
	private static final Level LEVEL = Level.FINEST;
	
	static {
		LOGGER.setLevel(Level.INFO);
		DynaTraceADKFactory.initialize();
	}
	
	public String Taggable.vertxTraceTag = null;
	
	public String Taggable.getVertxTraceTag() {
		synchronized (this) {
			return this.vertxTraceTag;
		}
	}
	
	public void Taggable.setVertxTraceTag(String value) {
		synchronized (this) {
			this.vertxTraceTag = value;
		}
	}
	
	declare parents:
		org.vertx.java.core.eventbus.impl.BaseMessage implements Taggable;
	
	void around(ServerID replyDest, BaseMessage message, Handler replyHandler, Handler asyncResultHandler, long timeout):
		execution(void org.vertx.java.core.eventbus.impl.DefaultEventBus.sendOrPub(ServerID, BaseMessage, Handler, Handler, long))
		&& args(replyDest, message, replyHandler, asyncResultHandler, timeout) {

		Tagging tagging = DynaTraceADKFactory.createTagging();
		String traceTag = tagging.getTagAsString();
		
		if ((tagging == null) || !tagging.isTagValid(traceTag)) {
			proceed(replyDest, message, replyHandler, asyncResultHandler, timeout);
			return;
		}
		((Taggable) message).setVertxTraceTag(traceTag);
		tagging.linkClientPurePath(true, traceTag);
		proceed(replyDest, message, replyHandler, asyncResultHandler, timeout);
	}
	
	after() returning(Message c): call(protected Message org.vertx.java.core.eventbus.impl.BaseMessage+.copy()) {
		BaseMessage<?> orig = Utils.cast(thisJoinPoint.getTarget());
		BaseMessage<?> copy = Utils.cast(c);
		((Taggable) copy).setVertxTraceTag(((Taggable) orig).getVertxTraceTag());
	}
	
	void around(Runnable task): execution(void org.vertx.java.core.impl.DefaultContext+.executeOnOrderedWorkerExec(Runnable)) && args(task) {
		Tagging tagging = DynaTraceADKFactory.createTagging();
		String traceTag = tagging.getTagAsString();
		
		if (!canGetTagged(task, tagging, traceTag)) {
			proceed(task);
			return;
		}
		LOGGER.log(LEVEL, "DefaultContext.executeOnOrderedWorkerExec(" + Utils.toString(task) + ")");
		TaggedRunnable wrapper = new TaggedRunnable(task);
		wrapper.setVertxTraceTag(traceTag);
		tagging.linkClientPurePath(true, traceTag);
		proceed(wrapper);
	}

	void around(Runnable handler): execution(void org.vertx.java.core.impl.DefaultContext+.execute(Runnable)) && args(handler) {
		Tagging tagging = DynaTraceADKFactory.createTagging();
		String traceTag = tagging.getTagAsString();
		
		if (!canGetTagged(handler, tagging, traceTag)) {
			proceed(handler);
			return;
		}
		LOGGER.log(LEVEL, "DefaultContext.execute(" + handler + ")");
		TaggedRunnable wrapper = new TaggedRunnable(handler);
		LOGGER.log(LEVEL, "  traceTag: " + traceTag);
		wrapper.setVertxTraceTag(traceTag);
		tagging.linkClientPurePath(true, traceTag);
		proceed(wrapper);
	}
	
	private static boolean canGetTagged(Runnable runnable) {
		if (runnable == null) {
			return true;
		}
		return false;
	}
	
	void around(Object event): execution(void Handler+.handle(Object)) && args(event) {
		LOGGER.log(LEVEL, Utils.toString(thisJoinPoint.getThis()) + ".handle(" + Utils.toString(event) + ")");

		Taggable taggable = null;
		Object target = thisJoinPoint.getThis();
		if (target instanceof Taggable) {
			taggable = Utils.cast(target);
		} else if (event instanceof Taggable) {
			taggable = Utils.cast(event);
		}
		
		Tagging tagging = null;
		
		if (taggable != null) {
			tagging = Utils.initTagging(taggable.getVertxTraceTag());
		}
		
		if (tagging == null) {
			proceed(event);
			return;
		}
		tagging.startServerPurePath();
		proceed(event);
		tagging.endServerPurePath();
	}
	
	Object around(Handler handler): execution(Object org.vertx.java.core.streams.ExceptionSupport+.exceptionHandler(Handler)) && args(handler) {
		Tagging tagging = DynaTraceADKFactory.createTagging();
		String traceTag = tagging.getTagAsString();
		
		if (!canGetTagged(handler, tagging, traceTag)) {
			return proceed(handler);
		}
		
		HandlerWrapper wrapper = new HandlerWrapper(handler);
		wrapper.setVertxTraceTag(traceTag);
		Object result = proceed(wrapper);
		tagging.linkClientPurePath(true, traceTag);
		return result;
	}
	
	Object around(Handler handler): execution(Object org.vertx.java.core.streams.ReadSupport+.dataHandler(Handler)) && args(handler) {
		Tagging tagging = DynaTraceADKFactory.createTagging();
		String traceTag = tagging.getTagAsString();
		
		if (!canGetTagged(handler, tagging, traceTag)) {
			return proceed(handler);
		}
		
		HandlerWrapper wrapper = new HandlerWrapper(handler);
		wrapper.setVertxTraceTag(traceTag);
		Object result = proceed(wrapper);
		tagging.linkClientPurePath(true, traceTag);
		return result;
	}

	Object around(Handler handler): execution(Object org.vertx.java.core.streams.ReadStream+.endHandler(Handler)) && args(handler) {
		Tagging tagging = DynaTraceADKFactory.createTagging();
		String traceTag = tagging.getTagAsString();
		
		if (!canGetTagged(handler, tagging, traceTag)) {
			return proceed(handler);
		}
		
		HandlerWrapper wrapper = new HandlerWrapper(handler);
		wrapper.setVertxTraceTag(traceTag);
		Object result = proceed(wrapper);
		tagging.linkClientPurePath(true, traceTag);
		return result;
	}
	
	Object around(Handler handler): execution(Object org.vertx.java.core.streams.DrainSupport+.drainHandler(Handler)) && args(handler) {
		Tagging tagging = DynaTraceADKFactory.createTagging();
		String traceTag = tagging.getTagAsString();
		
		if (!canGetTagged(handler, tagging, traceTag)) {
			return proceed(handler);
		}
		
		HandlerWrapper wrapper = new HandlerWrapper(handler);
		wrapper.setVertxTraceTag(traceTag);
		Object result = proceed(wrapper);
		tagging.linkClientPurePath(true, traceTag);
		return result;
	}
	
	void around(Handler handler):
		execution(void org.vertx.java.core.file.AsyncFile+.close(Handler))
		&& args(handler)
	{
		Tagging tagging = DynaTraceADKFactory.createTagging();
		String traceTag = tagging.getTagAsString();
		
		if (!canGetTagged(handler, tagging, traceTag)) {
			proceed(handler);
			return;
		}
		
		HandlerWrapper wrapper = new HandlerWrapper(handler);
		wrapper.setVertxTraceTag(traceTag);
		proceed(wrapper);
		tagging.linkClientPurePath(true, traceTag);
	}
	
	private static boolean canGetTagged(Object o, Tagging tagging, String traceTag) {
		if (o == null) {
			return false;
		}
		if (tagging == null) {
			return false;
		}
		return tagging.isTagValid(traceTag);
	}
	
	
	/**
	 * Around executions of {@link ServerConnection.handleRequest} we are
	 * creating an artificial call to a Servlet, which allows the
	 * Dynatrace Servlet Sensor to pick up the request as a Pure Path
	 *   
	 * @param req the request object
	 * @param resp the response object
	 */
	void around(
		final DefaultHttpServerRequest req,
		final DefaultHttpServerResponse resp
	):
		execution(
			void org.vertx.java.core.http.impl.ServerConnection.handleRequest(
				DefaultHttpServerRequest,
				DefaultHttpServerResponse
			)
		)
		&&
		args(req, resp)
	{
		final Runnable proceedRunnable = new Runnable() {
			@Override
			public void run() {
				proceed(req, resp);
			}
		};
		try {
			new VertxServlet(req, resp, proceedRunnable).execute();
		} catch (final Throwable t) {
			LOGGER.log(Level.WARNING, "Servlet Invocation failed",	t);
		}
	}
	
	after(DefaultHttpClient client, String method, String uri, Handler<HttpClientResponse> respHandler, DefaultContext context) :
		initialization(org.vertx.java.core.http.impl.DefaultHttpClientRequest.new(DefaultHttpClient, String, String, Handler, DefaultContext)
	) && args(client, method, uri, respHandler, context) {
		DefaultHttpClientRequest request = Utils.cast(thisJoinPoint.getTarget());
		Tagging tagging = DynaTraceADKFactory.createTagging();
		if (tagging != null) {
			String traceTag = tagging.getTagAsString();
			if (tagging.isTagValid(traceTag)) {
				request.headers().add("X-dynaTrace", traceTag);
				if (respHandler != null) {
					HandlerWrapper wrapper = new HandlerWrapper(respHandler);
					wrapper.setVertxTraceTag(traceTag);
					try {
						Field f = DefaultHttpClientRequest.class.getDeclaredField("respHandler");
						f.setAccessible(true);
						f.set(request, wrapper);
						tagging.linkClientPurePath(true, traceTag);
					} catch (Throwable t) {
						LOGGER.log(Level.WARNING, "Unable to replace response handler",	t);
					}
				}
			}
		}
    }
	
	after() : call(void org.vertx.java.core.http.HttpClientRequest.end()) {
		Tagging tagging = DynaTraceADKFactory.createTagging();
		if (tagging != null) {
			String traceTag = tagging.getTagAsString();
			if (tagging.isTagValid(traceTag)) {
				tagging.linkClientPurePath(true, traceTag);
			}
		}
	}
}
