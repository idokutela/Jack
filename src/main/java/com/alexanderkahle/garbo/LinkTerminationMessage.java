package com.alexanderkahle.garbo;

public class LinkTerminationMessage {
	public final Throwable reason;
	
	public LinkTerminationMessage(Throwable reason) {
		this.reason = reason;
	}
}
