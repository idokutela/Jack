package com.alexanderkahle.garbo;

/**
 * Created by alexanderkahle on 04.03.18.
 */

public interface Scheduler {
	void scheduleActor(Actor a);
	void relayMessage(long id, Object message);
	void kill(long id);
}
