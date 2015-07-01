package com.dynatrace.vertx.aspects;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.vertx.java.core.Handler;

/**
 * <p>
 * A wrapper around a {@link Handler} which simply delegates the event handling
 * to the wrapped handler.</p>
 * <p>
 * In addition it allows for storing context information about the current
 * PurePath, which is required in order to the dynaTrace Agent to trace the
 * execution of a transaction across thread boundaries.</p>
 * <p>This implementation is taking advantage of the fact that generic
 * information about the event type a {@link Handler} is supposed to take care
 * of has been erased during compilation time.
 * </p>
 * 
 * @author reinhard.pilz@dynatrace.com
 *
 */
@SuppressWarnings({"unchecked", "rawtypes" })
public class HandlerWrapper implements Handler<Object>, Taggable {
	
	private static final Logger LOGGER =
			Logger.getLogger(HandlerWrapper.class.getName());
	
	private static final Level LEVEL = Level.FINEST;
	
	/**
	 * The {@link Handler} wrapped by this object
	 */
	private final Handler handler;
	
	/**
	 * Context information about the current PurePath.
	 */
	public String vertxTraceTag = null;
	
	/**
	 * c'tor
	 * 
	 * @param handler the {@link Handler} to wrap
	 */
	public HandlerWrapper(Handler handler) {
		this.handler = handler;
	}

	/**
	 * Delegates handling the given event to the {@link Handler} this object is
	 * wrapping
	 * 
	 * @param event the event to handle
	 */
	@Override
	public void handle(Object event) {
		LOGGER.log(LEVEL, Utils.toString(handler) + ".handle(" + Utils.toString(event) + ")");
		handler.handle(event);
	}
	
	/**
	 * @return the {@link Handler} wrapped by this object
	 */
	public <E> Handler<E> getHandler() {
		return (Handler<E>) handler;
	}

	/**
	 * @return the context information about the current PurePath or
	 * 		{@code null} if there has no such information been stored on this
	 * 		object.
	 * 
	 * @see #setVertxTraceTag(String)
	 */
	public String getVertxTraceTag() {
		return vertxTraceTag;
	}

	/**
	 * Sets the context information about the current PurePath or clears it
	 * if the given value is {@code null}.
	 * 
	 * @param value the context information about the current PurePath.
	 */
	public void setVertxTraceTag(String value) {
		LOGGER.log(LEVEL, this.toString() + ".setVertxTraceTag(" + value + ")");
		this.vertxTraceTag = value;
	}

}
