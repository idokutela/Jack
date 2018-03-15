package com.alexanderkahle.Jack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * An actor-building factory; one can use an instance repeatedly to build actors.
 * Typically used as follows <code>builder.setInitialBehaviour(b).setExecutor(e).build()</code>.
 * 
 * @author alexanderkahle
 *
 */
public class ActorBuilder {
	public ActorBuilder() {
		defaultExecutor = makeDefaultExecutor();
		
		reset();
	}
	
	/**
	 * Builds the actor with the current settings. Resets the settings to their initial
	 * values. Note, <code>setInitialBehaviour</code> must be called before building.
	 * 
	 * By default, an actor has a really dumb executor: it creates a new thread to 
	 * execute on each run. One can change the default executor with setDefaultExecutor.
	 * Unless you're just testing, I _highly_ recommend doing so.
	 * 
	 * The default id generator generates a new random ID each time it's called.
	 * 
	 * Note, this is not threadsafe. Make a new instance per thread (or per usage).
	 * 
	 * @return A new actor.
	 * @throws IllegalArgumentException If the mailbox size is not positive.
	 * @throws NullPointerException If any of the fields have been set null.
	 */
	public Actor build() {
		if (mailboxSize <= 0) throw new IllegalArgumentException("The mailbox must have a positive size");
		if (theExecutor == null) throw new NullPointerException("Expected an executor.");
		if (theBehaviour == null) throw new NullPointerException("Expected a behaviour.");
		if (observers == null) throw new NullPointerException("Expected observers");
		if (links == null) throw new NullPointerException("Expected links.");
		
		BlockingQueue<Object> messages = new ArrayBlockingQueue<>(mailboxSize);
		reset();
		return new Actor(theBehaviour, theExecutor, observers, links, messages, idGenerator, trapExit);
	}
	
	public ActorBuilder initialBehaviour(Behaviour b) {
		theBehaviour = b;
		return this;
	}
	
	public ActorBuilder executor(Executor e) {
		theExecutor = e;
		return this;
	}
	
	public ActorBuilder mailboxSize(int size) {
		mailboxSize = size;
		return this;
	}
	
	public ActorBuilder observers(Map<Integer, Actor> o) {
		observers = o;
		return this;
	}
	
	public ActorBuilder links(Set<Actor> links) {
		this.links = links;
		return this;
	}

	public ActorBuilder idGenerator(IDGenerator idGenerator) {
		this.idGenerator = idGenerator;
		return this;
	}
	
	public ActorBuilder trapExit(boolean flag) {
		trapExit = flag;
		return this;
	}
	
	public void setDefaultExecutor(Executor e) {
		this.defaultExecutor = e;
	}
	
	private void reset() {
		theBehaviour = null;
		theExecutor = defaultExecutor;
		observers = new HashMap<>();
		links = new HashSet<>();
		mailboxSize = DEFAULT_MAILBOX_SIZE;
		idGenerator = defaultIDGenerator;
		trapExit = false;
	}
	
	private static Executor makeDefaultExecutor() {
		return Executors.newCachedThreadPool();
	}
	
	private static final IDGenerator defaultIDGenerator = new IDGenerator() {
		private Random randomizer = new Random();
		
		@Override
		public int generateID() {
			return randomizer.nextInt();
		}

	};
	
	private Executor defaultExecutor;
	
	private Behaviour theBehaviour;
	private Executor theExecutor;
	private Map<Integer, Actor> observers;
	private Set<Actor> links;
	private IDGenerator idGenerator;
	private int mailboxSize;
	private boolean trapExit;
	
	public static int DEFAULT_MAILBOX_SIZE = 10000000;
}
