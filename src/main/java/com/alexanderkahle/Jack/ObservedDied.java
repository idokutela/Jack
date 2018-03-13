package com.alexanderkahle.Jack;

public class ObservedDied {
	public final Throwable reason;
	public final int id;
	
	public ObservedDied(int id, Throwable reason) {
		this.reason = reason;
		this.id = id;
	}
}
