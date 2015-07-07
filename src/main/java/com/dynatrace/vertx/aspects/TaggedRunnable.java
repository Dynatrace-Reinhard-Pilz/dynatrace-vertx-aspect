package com.dynatrace.vertx.aspects;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.vertx.java.core.impl.DefaultContext;

import com.dynatrace.adk.Tagging;

/**
 * <p>
 * A wrapper around {@link Runnable} objects in case there are operations being
 * passed to {@link DefaultContext#execute(Runnable)}.
 * </p><p>
 * This ensures that when transactions are breaching thread borders the
 * dynaTrace Agent will still be able to create a single PurePath.
 * </p>
 * 
 * @author reinhard.pilz@dynatrace.com
 *
 */
public class TaggedRunnable implements Runnable, Taggable {
	
	private static final Logger LOGGER =
			Logger.getLogger(TaggedRunnable.class.getName());
	
	private static final Level LEVEL = Level.FINEST;
	
	/**
	 * The {@link Runnable} wrapped by this object
	 */
	private final Runnable runnable;
	
	/**
	 * Context information about the current PurePath.
	 */
	public String vertxTraceTag = null;
	
	/**
	 * c'tor
	 * 
	 * @param handler the {@link Runnable} to wrap
	 */
	public TaggedRunnable(Runnable runnable) {
//		Thread.dumpStack();
		this.runnable = runnable;
	}

	/**
	 * Delegates the execution to the {@link Runnable} this object is
	 * wrapping, but encapsulates that execution into a Server Side PurePath
	 * in case there is context information about an ongoing PurePath stored
	 * on this object.
	 */
	@Override
	public void run() {
		if (runnable == null) {
			return;
		}
		LOGGER.log(LEVEL, Utils.toString(runnable) + ".run()");
		
		Tagging tagging = Utils.initTagging(getVertxTraceTag());
		
		if (tagging == null) {
			Runnable r = getRunnable();
			if (r != null) {
				r.run();
			}
			return;
		}
		tagging.startServerPurePath();
		Runnable r = getRunnable();
		if (r != null) {
			r.run();
		}
		tagging.endServerPurePath();
	}
	
	/**
	 * @return the {@link Runnable} wrapped by this object
	 */
	public Runnable getRunnable() {
		if (runnable instanceof TaggedRunnable) {
			return ((TaggedRunnable) runnable).getRunnable();
		}
		return runnable;
	}

	/**
	 * @return the context information about the current PurePath or
	 * 		{@code null} if there has no such information been stored on this
	 * 		object.
	 * 
	 * @see #setVertxTraceTag(String)
	 */
	public String getVertxTraceTag() {
		synchronized (this) {
			return vertxTraceTag;
		}
	}

	/**
	 * Sets the context information about the current PurePath or clears it
	 * if the given value is {@code null}.
	 * 
	 * @param value the context information about the current PurePath.
	 */
	public void setVertxTraceTag(String value) {
		synchronized (this) {
			LOGGER.log(LEVEL, this.toString() + ".setVertxTraceTag(" + value + ")");
			this.vertxTraceTag = value;
		}
	}

}
