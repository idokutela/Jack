package com.alexanderkahle.garbo;

public class TerminationMessage {
	public final long watchID;
	public final Throwable reason;
	
	public TerminationMessage(long watchID, Throwable reason) {
		this.watchID = watchID;
		this.reason = reason;
	}
}
