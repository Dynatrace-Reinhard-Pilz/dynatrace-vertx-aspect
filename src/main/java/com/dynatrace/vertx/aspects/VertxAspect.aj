package com.dynatrace.vertx.aspects;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.eventbus.impl.BaseMessage;
import org.vertx.java.core.eventbus.impl.DefaultEventBus;
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
public aspect VertxAspect {
	
//	public static final ThreadLocal<Boolean> isRemoteInstrumented = new ThreadLocal<Boolean>();
//	public static final Buffer PONG = resolvePong();
	
	private static final Logger LOGGER =
			Logger.getLogger(VertxAspect.class.getName());
	
	/**
	 * The ADK needs to be initialized before it is being used for tagging
	 */
	static {
		DynaTraceADKFactory.initialize();
	}
	
//	private static Buffer resolvePong() {
//		try {
//			Field f = DefaultEventBus.class.getDeclaredField("PONG");
//			f.setAccessible(true);
//			return cast(f.get(null));
//		} catch (Throwable t) {
//			throw new InternalError("unable to resolve PONG");
//		}
//	}

	/**
	 * Introduces an additional field {@code vertxTraceTag} into class
	 * {@link DefaultHttpServerRequest} which holds the information about the
	 * current PurePath.
	 */
	public String DefaultHttpServerRequest.vertxTraceTag = null;
	public String DefaultHttpServerResponse.vertxTraceTag = null;
	public String BaseMessage.vertxTraceTag = null;
	
	void around(Object event): call(void Handler+.handle(*)) && args(event) {
		final Tagging tagging = resolveTag(event);
		if (tagging != null) {			
			tagging.startServerPurePath();
 		}
		if (event instanceof ThrowableWrapper) {
			proceed(((ThrowableWrapper) event).getCause());
		} else {
			proceed(event);
		}
		if (tagging != null) {
			tagging.endServerPurePath();
		}
	}
	
	Object around(): call(protected abstract * BaseMessage+.copy()) {
		Message<?> c = cast(proceed());
		BaseMessage<?> orig = cast(thisJoinPoint.getTarget());
		if (!(c instanceof BaseMessage)) {
			return c;
		}
		BaseMessage<?> copy = cast(c);
		LOGGER.log(Level.FINE, "copy.vertxTraceTag = " + orig.vertxTraceTag);
		copy.vertxTraceTag = orig.vertxTraceTag;
		return copy;
	}
	
	void around(ServerID replyDest, BaseMessage<?> message, Handler<?> replyHandler, Handler<?> asyncResultHandler, long timeout):
		execution(void DefaultEventBus.sendOrPub(ServerID, BaseMessage, Handler, Handler, long))
		&& args(replyDest, message, replyHandler, asyncResultHandler, timeout) {

		final Tagging tagging = DynaTraceADKFactory.createTagging();
		final String traceTag = tagging.getTagAsString();
		if (message != null && tagging.isTagValid(traceTag)) {
			message.vertxTraceTag = traceTag;
		}
		proceed(replyDest, message, replyHandler, asyncResultHandler, timeout);
		if (message != null && tagging.isTagValid(traceTag)) {
			tagging.linkClientPurePath(true, traceTag);
		}
	}

/*	

	private void salvageVertxTraceTag(BaseMessage<?> origin, BaseMessage<?> reply) {
		if (origin == null) {
			return;
		}
		if (reply == null) {
			return;
		}
		if (origin.vertxTraceTag == null) {
			return;
		}
		reply.vertxTraceTag = origin.vertxTraceTag;
	}

	@SuppressWarnings("rawtypes")
	before(BaseMessage msg, long timeout, Handler replyHandler): call(void BaseMessage.sendReplyWithTimeout(BaseMessage, long, Handler)) && args(msg, timeout, replyHandler) {
		
	}
	
*/	
	
//	before(BaseMessage<?> msg, Handler<?> replyHandler): call(void BaseMessage.sendReply(BaseMessage<?>, Handler<?>)) && args(msg, replyHandler) {
//		Object thisMsg = thisJoinPoint.getThis();
//		if (thisMsg == null) {
//			return;
//		}
//		final Tagging tagging = DynaTraceADKFactory.createTagging();
//		final String traceTag = tagging.getTagAsString();
//		if (msg != null && tagging.isTagValid(traceTag)) {
//			msg.vertxTraceTag = traceTag;
//		}
//		if (msg != null && tagging.isTagValid(traceTag)) {
//			tagging.linkClientPurePath(true, traceTag);
//		}
//	}
	
	/**
	 * After the initial request handling further data coming in for a request
	 * is being handled asynchronously on a different thread.<br />
	 * <br />
	 * Therefore these executions need to be handled as asynchronous
	 * sub paths using the Dynatrace ADK.<br />
	 * <br />
	 * The information about which Pure Path to stitch the Sub Paths to is being
	 * held on the additional field {@code vertxTraceTag} on class
	 * {@link DefaultHttpServerRequest} which remains the same during the whole
	 * transaction
	 * 
	 * @param data the {@link Buffer} to handle
	 */
	void around(Buffer data):
		execution(void DefaultHttpServerRequest.handleData(Buffer))
		&&
		args(data)
	{
		final Tagging tagging = 
				resolveTag((DefaultHttpServerRequest) thisJoinPoint.getThis());
		if (tagging != null) {			
			tagging.startServerPurePath();
 		}
		proceed(data);
		if (tagging != null) {
			tagging.endServerPurePath();
		}
	}
	
	void around(): execution(void DefaultHttpServerRequest.handleEnd()) {
		final Tagging tagging =
				resolveTag((DefaultHttpServerRequest) thisJoinPoint.getThis());
		if (tagging != null) {			
			tagging.startServerPurePath();
 		}
		proceed();
		if (tagging != null) {
			tagging.endServerPurePath();
			((DefaultHttpServerRequest) thisJoinPoint.getThis()).vertxTraceTag = null;
		}
	}
	
	void around(Throwable throwable):
		execution(void DefaultHttpServerRequest.handleException(Throwable))
		&& args(throwable)
	{
		proceed(new ThrowableWrapper(throwable));
	}
	
	DefaultHttpServerResponse around(Buffer chunk): execution(DefaultHttpServerResponse DefaultHttpServerResponse.write(Buffer)) && args(chunk) {
		final Tagging tagging =
				resolveTag((DefaultHttpServerResponse) thisJoinPoint.getThis());
		if (tagging != null) {			
			tagging.startServerPurePath();
 		}
		Object result = proceed(chunk);
		if (tagging != null) {
			tagging.endServerPurePath();
			((DefaultHttpServerResponse) thisJoinPoint.getThis()).vertxTraceTag = null;
		}
		return (DefaultHttpServerResponse) result;
	}

	DefaultHttpServerResponse around(String chunk): execution(DefaultHttpServerResponse DefaultHttpServerResponse.write(String)) && args(chunk) {
		final Tagging tagging =
				resolveTag((DefaultHttpServerResponse) thisJoinPoint.getThis());
		if (tagging != null) {			
			tagging.startServerPurePath();
 		}
		Object result = proceed(chunk);
		if (tagging != null) {
			tagging.endServerPurePath();
			((DefaultHttpServerResponse) thisJoinPoint.getThis()).vertxTraceTag = null;
		}
		return (DefaultHttpServerResponse) result;
	}

	DefaultHttpServerResponse around(String chunk, String enc): execution(DefaultHttpServerResponse DefaultHttpServerResponse.write(String, String)) && args(chunk, enc) {
		DefaultHttpServerResponse res = cast(thisJoinPoint.getThis());
		final Tagging tagging =	resolveTag(res);
		if (tagging != null) {			
			tagging.startServerPurePath();
 		}
		Object result = proceed(chunk, enc);
		if (tagging != null) {
			tagging.endServerPurePath();
			((DefaultHttpServerResponse) thisJoinPoint.getThis()).vertxTraceTag = null;
		}
		return (DefaultHttpServerResponse) result;
	}
	
//	@SuppressWarnings({ "unchecked", "rawtypes" })
//	void around(BaseMessage msg, Object holder): execution(void DefaultEventBus.doReceive(BaseMessage, *)) && args(msg, holder) {
//		final Tagging tagging = DynaTraceADKFactory.createTagging();
//		final String traceTag = tagging.getTagAsString();
//		if (msg != null && tagging.isTagValid(traceTag)) {
//			msg.vertxTraceTag = traceTag;
//		}
//		if (msg != null && tagging.isTagValid(traceTag)) {
//			tagging.linkClientPurePath(true, traceTag);
//		}
//		proceed(msg, holder);
//	}


//	BaseMessage<?> around(Buffer buf): call(BaseMessage org.vertx.java.core.eventbus.impl.MessageFactory.read(Buffer)) && args(buf) {
//		BaseMessage<?> result = proceed(buf);
//		if (result instanceof PingMessage) {
//			PingMessage pm = cast(result);
//			isRemoteInstrumented.set("ping".equals(pm.body()));
//		}
//		return result;
//	}
//	
//	NetSocket around(Buffer buf): execution(public NetSocket NetSocket.write(Buffer)) && args(buf) {
//		if (buf == PONG) {
//			return proceed(null);
//		}
//		return proceed(buf);
//	}
	
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
		
	/**
	 * <p>
	 * Initializes a {@link Tagging} object with the information about an
	 * ongoing PurePath based on the information stored within the given
	 * {@link DefaultHttpServerRequest}.
	 * </p>
	 * <p>
	 * Handing over {@code null} for the {@link DefaultHttpServerRequest} will
	 * result in getting returned {@code null}. Otherwise the same Sanity Checks
	 * will are being performed as by {@link #initTagging(String)}
	 * </p>
	 * 
	 * @param res the object to query for PurePath information
	 * 
	 * @return an initialized {@link Tagging} object, which can be used to
	 * 		start a <em>Server Side PurePath</em>, or {@code null} if it is
	 * 		impossible or not advisable to start a <em>Server Side PurePath</em>
	 * 
	 * @see #initTagging(String)
	 * @see Tagging#startServerPurePath()
	 */
	private static Tagging resolveTag(final DefaultHttpServerRequest req) {
		if (req == null) {
			return null;
		}
		return initTagging(req.vertxTraceTag);
	}
	
	/**
	 * <p>
	 * Initializes a {@link Tagging} object with the information about an
	 * ongoing PurePath based on the information stored within the given
	 * {@link DefaultHttpServerResponse}.
	 * </p>
	 * <p>
	 * Handing over {@code null} for the {@link DefaultHttpServerResponse} will
	 * result in getting returned {@code null}. Otherwise the same Sanity Checks
	 * will are being performed as by {@link #initTagging(String)}
	 * </p>
	 * 
	 * @param res the object to query for PurePath information
	 * 
	 * @return an initialized {@link Tagging} object, which can be used to
	 * 		start a <em>Server Side PurePath</em>, or {@code null} if it is
	 * 		impossible or not advisable to start a <em>Server Side PurePath</em>
	 * 
	 * @see #initTagging(String)
	 * @see Tagging#startServerPurePath()
	 */
	private static Tagging resolveTag(final DefaultHttpServerResponse res) {
		if (res == null) {
			return null;
		}
		return initTagging(res.vertxTraceTag);
	}
	
	/**
	 * <p>
	 * Initializes a {@link Tagging} object with the information about an
	 * ongoing PurePath based on the information stored within the given
	 * event object.
	 * </p>
	 * <p>
	 * In addition to the Sanity Checks performed by
	 * {@link #initTagging(String)} it is also required that the passed event
	 * object is an instance of {@link BaseMessage}. All other types of events
	 * handled by the event bus are unable to hold context information.
	 * </p>
	 * 
	 * @param event the object to query for PurePath information
	 * 
	 * @return an initialized {@link Tagging} object, which can be used to
	 * 		start a <em>Server Side PurePath</em>, or {@code null} if it is
	 * 		impossible or not advisable to start a <em>Server Side PurePath</em>
	 * 
	 * @see #initTagging(String)
	 * @see Tagging#startServerPurePath()
	 */
	private static Tagging resolveTag(final Object event) {
		/*
		 * Sanity Check
		 * Perhaps a "null" event has a specific internal meaning. We don't know
		 * that for sure. What we cannot do in that case is bridge any gaps
		 * within the PurePaths, because the object holding the required
		 * information is absent.
		 */
		if (event == null) {
			return null;
		}
		/*
		 * If it is not an instance of BaseMessage it we are sure that there is
		 * no information about a PurePath available.
		 */
		if (!(event instanceof BaseMessage)) {
			return null;
		}
		
		/*
		 * Recovering the stored information about the ongoing PurePath this
		 * message is part of.
		 */
		final BaseMessage<?> message = (BaseMessage<?>) event;
		
		return initTagging(message.vertxTraceTag);
	}
	
	/**
	 * <p>
	 * Initializes a {@link Tagging} object with the information about an
	 * ongoing PurePath based on the given {@code traceTag}.
	 * </p>
	 * <p>This method only returns non {@code null} if the following sanity
	 * checks have been passed:
	 * </p>
	 * <ul>
	 * 	<li>The passed {@code traceTag} is not {@code null}</li>
	 * 	<li>The passed {@code traceTag} contains indeed information about a
	 * 		PurePath encoded as a {@link String}</li>
	 * 	<li>The current {@link Thread} is not aware of an ongoing PurePath or
	 * 		Sub Path yet</li>
	 * 	<li>An instance of class {@link Tagging} could get obtained from the
	 * 		dynaTrace ADK</li>
	 * </ul>
	 * 
	 * @param traceTag encoded information about an ongoing PurePath
	 * 
	 * @return an initialized {@link Tagging} object or {@code null} if starting
	 * 		a <em>Server Side PurePath</em> is not possible or advisable.
	 */
	private static Tagging initTagging(String traceTag) {
		/*
		 * Future versions of vert-x might have different workflows. Therefore
		 * we cannot assume that the information has actually been stored
		 * properly
		 */
		if (traceTag == null) {
			return null;
		}
		
		final Tagging tagging = DynaTraceADKFactory.createTagging();
		/*
		 * Unlikely, but just in case
		 */
		if (tagging == null) {
			return null;
		}
		
		/*
		 * Sanity Check
		 * Unlikely, but somebody could have stored a really invalid piece of
		 * trash instead of encoded PurePath information.
		 */
		if (!tagging.isTagValid(traceTag)) {
			return null;
		}
		
		/*
		 * It is possible that another Sensor was able to connect this portion
		 * of the PurePath already.
		 * (e.g. Thread Start Tagging, Executor Tagging)
		 * In that case it is unwise to interfere.
		 */
		final byte[] currentTraceTag = tagging.getTag();
		if (tagging.isTagValid(currentTraceTag)) {
			return null;
		}

		/*
		 * We are sure now that the calling method is supposed to start a
		 * <em>Server Side PurePath</em>.
		 */
		tagging.setTagFromString(traceTag);
		return tagging;
	}
	
	/**
	 * Convenience method to cast an object. It results in a
	 * {@link ClassCastException} but it it is shorter syntax.
	 * 
	 * @param o the object to cast
	 * 
	 * @return the object, but cast to a different type.
	 */
	@SuppressWarnings("unchecked")
	private static <T> T cast(Object o) {
		return (T) o;
	}
	
}
