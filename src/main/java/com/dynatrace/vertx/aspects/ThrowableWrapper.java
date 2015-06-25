package com.dynatrace.vertx.aspects;

public class ThrowableWrapper extends Throwable {

	private static final long serialVersionUID = 1L;

	public ThrowableWrapper(Throwable t) {
		super(t);
	}
}
