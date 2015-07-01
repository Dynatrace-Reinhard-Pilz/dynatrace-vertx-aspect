package com.dynatrace.vertx.aspects;

import com.dynatrace.adk.DynaTraceADKFactory;
import com.dynatrace.adk.Tagging;

/**
 * Utility methods for casting an tagging
 * 
 * @author reinhard.pilz@dynatrace.com
 *
 */
public final class Utils {
	
	static {
		DynaTraceADKFactory.initialize();
	}

	/**
	 * An unchecked cast in order to avoid type checks for cases where through
	 * aspectj weaving the type of object is certain.
	 * 
	 * @param o the object to cast
	 * 
	 * @return the cast object
	 */
	@SuppressWarnings("unchecked")
	public static <T> T cast(Object o) {
		return (T) o;
	}
	
	/**
	 * Helper method for debugging which produces the class name of the given
	 * object and if applicable the simple name of the class.
	 * 
	 * @param o the object to get a string representation for
	 * 
	 * @return a string representation of the given object
	 */
	public static String toString(Object o) {
		if (o == null) {
			return "null";
		}
		String simpleName = o.getClass().getSimpleName();
		if ((simpleName != null) && (simpleName.length() > 0)) {
			return simpleName;
		}
		return o.getClass().getName();
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
	public static Tagging initTagging(String traceTag) {
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
	
}
