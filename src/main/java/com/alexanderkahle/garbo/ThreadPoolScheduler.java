package com.alexanderkahle.garbo;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * A very simple implementation of a schedule. It relies on a {@link ThreadPoolExecutor}
 * to do the actual work.
 * 
 * @author Alexander Kahle
 */
public class ThreadPoolScheduler implements Scheduler {
	public ThreadPoolScheduler(ThreadPoolExecutor e) {
		theExecutor = e;
	}

	@Override
	public void scheduleActor(Actor a) {
		if (a == null) throw new NullPointerException("Attempted to schedule a non-null accent.");
		if (actors.putIfAbsent(a.ID, a) != a)
			throw new RuntimeException("ID collision: two actors share an id.");
	}

	@Override
	public void relayMessage(long id, Object message) {
		final Actor receiver = actors.get(id);
		if (message == null) throw new NullPointerException("Cannot deliver null message.");
		if (receiver == null) return;
		receiver.queueMessage(message);
		
		// This makes sure the actor is run at least once for every message
		// received.
		theExecutor.execute(receiver);
	}

	@Override
	public synchronized void kill(long id) {
		Actor toDie = actors.remove(id);
		if (toDie != null) toDie.kill();
	}

	private final ThreadPoolExecutor theExecutor;
	private final ConcurrentMap<Long, Actor> actors = new ConcurrentHashMap<>();	
}
