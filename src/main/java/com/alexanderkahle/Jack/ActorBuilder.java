package com.alexanderkahle.Jack;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;

public class ActorBuilder {
	public ActorBuilder() {
		reset();
	}
	
	public Actor build() {
		if (mailboxSize <= 0) throw new IllegalArgumentException("The mailbox must have a positive size");
		if (theExecutor == null) throw new NullPointerException("Expected an executor.");
		if (theBehaviour == null) throw new NullPointerException("Expected a behaviour.");
		BlockingQueue<Object> messages = new ArrayBlockingQueue<>(mailboxSize);
		
		return new Actor(theBehaviour, theExecutor, observers, messages, randomizer);
	}
	
	public ActorBuilder setInitialBehaviour(Behaviour b) {
		theBehaviour = b;
		return this;
	}
	
	public ActorBuilder setExecutor(Executor e) {
		theExecutor = e;
		return this;
	}
	
	private void reset() {
		theBehaviour = null;
		theExecutor = null;
		observers = new HashMap<>();
		mailboxSize = DEFAULT_MAILBOX_SIZE;
		randomizer = new Random();
	}
	
	private Behaviour theBehaviour;
	private Executor theExecutor;
	private Map<Integer, Actor> observers;
	private Random randomizer;
	private int mailboxSize;
	
	public static int DEFAULT_MAILBOX_SIZE = 10000000;
}
