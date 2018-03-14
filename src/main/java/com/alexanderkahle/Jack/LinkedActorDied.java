package com.alexanderkahle.Jack;

public class LinkedActorDied extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public final Actor actor;
	public final Throwable reason;
	
	public LinkedActorDied(Actor actor, Throwable reason) {
		this.actor = actor;
		this.reason = reason;
	}
	
	public boolean equals(Object o) {
		if (!(o instanceof LinkedActorDied)) return false;
		LinkedActorDied t = (LinkedActorDied) o;
		return (t.actor == actor) && t.reason.equals(reason);
	}
}
