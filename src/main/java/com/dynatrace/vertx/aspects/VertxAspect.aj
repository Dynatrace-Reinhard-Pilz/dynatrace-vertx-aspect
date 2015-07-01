package com.dynatrace.vertx.aspects;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.eventbus.impl.BaseMessage;
import org.vertx.java.core.http.impl.DefaultHttpServerRequest;
import org.vertx.java.core.http.impl.DefaultHttpServerResponse;
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
public aspect VertxAspect {
	
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
		if (task == null) {
			proceed(task);
			return;
		}
		Tagging tagging = DynaTraceADKFactory.createTagging();
		String traceTag = tagging.getTagAsString();
		
		if ((tagging == null) || !tagging.isTagValid(traceTag)) {
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
		if (handler == null) {
			proceed(handler);
			return;
		}
		Tagging tagging = DynaTraceADKFactory.createTagging();
		String traceTag = tagging.getTagAsString();
		
		if ((tagging == null) || !tagging.isTagValid(traceTag)) {
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
		
		if ((handler == null) || (tagging == null) || !tagging.isTagValid(traceTag)) {
			proceed(handler);
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
		
		if ((handler == null) || (tagging == null) || !tagging.isTagValid(traceTag)) {
			proceed(handler);
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
		
		if ((handler == null) || (tagging == null) || !tagging.isTagValid(traceTag)) {
			proceed(handler);
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
		
		if ((handler == null) || (tagging == null) || !tagging.isTagValid(traceTag)) {
			proceed(handler);
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
		
		if ((handler == null) || (tagging == null) || !tagging.isTagValid(traceTag)) {
			proceed(handler);
		}
		
		HandlerWrapper wrapper = new HandlerWrapper(handler);
		LOGGER.log(LEVEL, "---- created wrapper and tagged with " + traceTag);
		wrapper.setVertxTraceTag(traceTag);
		proceed(wrapper);
		tagging.linkClientPurePath(true, traceTag);
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
	
}
