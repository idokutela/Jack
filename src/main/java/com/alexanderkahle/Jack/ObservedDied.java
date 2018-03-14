package com.alexanderkahle.Jack;

public class ObservedDied {
	public final Throwable reason;
	public final int id;
	
	public ObservedDied(int id, Throwable reason) {
		this.reason = reason;
		this.id = id;
	}
	
	public boolean equals(Object o) {
		if (!(o instanceof ObservedDied)) {
			return false;
		}
		ObservedDied t = (ObservedDied) o;
		return t.reason.equals(reason) && t.id == id;
	}
}
