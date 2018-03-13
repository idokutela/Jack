package com.alexanderkahle.Jack;

public interface Behaviour {
	public Behaviour run(Actor self, Object message) throws Throwable;
}
