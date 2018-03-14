package com.alexanderkahle.Jack;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;

/**
 * Actors are individual units of concurrently. They have an inbox, with messages
 * being processed in turn by their {@Behaviour behaviour}.
 *  
 * @author alexanderkahle
 *
 */
public class Actor implements Runnable {
	/**
	 * Sends a message to the actor. If the actor is dead, the message will not
	 * arrive. If the actor's mailbox is full, will kill the actor with a 
	 * {@link BlockedException}.
	 * 
	 * @param message
	 */
	public void send(Object message) {
		if (message == null) throw new NullPointerException("Cannot send a null message.");
		
		BlockingQueue<Object> messages = this.messages;
		if (messages == null) return;
		
		if (!messages.offer(message)) { // I don't care if I'm already dead
			kill(new BlockedException());
			return;
		}
		
		theExecutor.execute(this); // No need for locks: isRunning makes sure only one execution happens at a time
	}
	
	/**
	 * Adds an observer to the actor.
	 * 
	 * @param a The actor to observe.
	 * @return The observation id, needed to remove the observer. Null if the actor is dead.
	 */
	public Integer addObserver(Actor a) {
		if (a == null) throw new NullPointerException("Cannot observe null.");
		
		Map<Integer, Actor> observers;
		Integer observationID;
		
		synchronized (this) { 
			observers = this.observers;
			
			if (observers != null) {
				// Make sure the observationID is unique
				while (observers.get(
						observationID = generator.generateID()
						) != null) {} 
				observers.put(observationID, a);
				return observationID;
			}
		}
		return null;
	}
	
	/**
	 * Removes the observer.
	 * 
	 * @param watchId The id of the observation.
	 */
	public void removeObserver(int watchId) {
		Map<Integer, Actor> observers;
		synchronized (this) {
			observers = this.observers;
			if (observers == null) return;
			observers.remove(watchId);
		}
	}
	
	/**
	 * Links to another actor. If the link could not be established,
	 * returns false. Note, it is possible for a kill to occur while the link
	 * is being established. In this case, depending on how far the link has
	 * got to being established, either the other actor will die, or link 
	 * will return false.
	 * 
	 * It is an excellent idea to set up links _before_ an actor
	 * receives its first message.
	 * 
	 * @param a the actor to link to.
	 */
	public boolean link(Actor a) {
		boolean result = linkInternal(a);
		if (!result) return result;
		result = result && a.linkInternal(this);
		if (!result) removeLinkInternal(a);
		return result;
	}
	
	/**
	 * Unlinks this actor from another. Note, this may race: creation of links
	 * and death are explicitly racy.
	 * 
	 * @param a The actor to link to.
	 */
	public void unlink(Actor a) {
		removeLinkInternal(a);
		a.removeLinkInternal(this);
	}
	
	/**
	 * Kills the actor with the given reason. If the actor is already dead,
	 * does nothing.
	 * 
	 * @param reason The reason to kill the actor.
	 */
	public void kill(Throwable reason) {
		if (isSystem && 
				(reason instanceof LinkedActorDied)) {
			removeLinkInternal(((LinkedActorDied) reason).actor);
			send(reason);
			return;
		}
		
		Thread currentThread;
		Map<Integer, Actor> observers;
		Set<Actor> links;
		
		synchronized (this) {
			currentThread = this.currentThread;
			observers = this.observers;
			links = this.links;
						
			this.theBehaviour = null;
			this.currentThread = null;
			this.observers = null;
			this.messages = null;
			this.links = null;
			this.isRunning = false;
		}
		
		notifyLinks(links, reason);
		notifyObservers(observers, reason);
		interruptCurrentThread(currentThread);
	}
	
	/**
	 * Takes the next message from the queue.
	 * 
	 * Warning: this removes the message from the queue completely!
	 * Warning: this blocks if the queue is empty!
	 * 
	 * @return the next message in the queue, or null if the actor is dead.
	 */
	public Object takeNextMessage() throws InterruptedException {
		BlockingQueue<Object> messages = this.messages;
		if (messages == null) return null;
		return messages.take();
	}
	
	public boolean isAlive() {
		return this.theBehaviour != null;
	}
	
	@Override
	public void run() {
		Object message;
		Behaviour behaviour; // So that it can't change under our feet

		synchronized (this) {
			if (isRunning) return;
			
			behaviour = theBehaviour; 
			if (behaviour == null) { // we've been killed
				return;
			}

			message = messages.poll();
			if (message == null) return; // mailbox empty
			
			isRunning = true;
			currentThread = Thread.currentThread();
		}

		try {
			theBehaviour = behaviour.run(this, message);
		} catch (Throwable t) {
			currentThread = null;
			// An exception was thrown by the behaviour
			if (theBehaviour != null) { 
				kill(t);
			}		
			return;
		}
		
		currentThread = null;
		if (theBehaviour == null) { // the actor terminated
			kill(null);
			return;
		}

		isRunning = false;		
		theExecutor.execute(this);
	}

	Actor(Behaviour initialBehaviour, Executor executor,
			Map<Integer, Actor> observers,
			Set<Actor> links,
			BlockingQueue<Object> messages, IDGenerator generator, boolean isSystem) {
		theBehaviour = initialBehaviour;
		theExecutor = executor;
		this.observers = observers;
		this.messages = messages;
		this.generator = generator;
		this.links = links;
		isRunning = false;
		this.isSystem = isSystem;
	}
	
	private synchronized boolean linkInternal(Actor a) {
		if (links == null) return false;
		links.add(a);
		return true;
	}

	private synchronized boolean removeLinkInternal(Actor a) {
		if (links == null) return false;
		links.remove(a);
		return true;
	}

	private void notifyObservers(Map<Integer, Actor> observers, Throwable reason) {
		if (observers == null) return;
		
		for (Integer id: observers.keySet()) {
			Actor observer = observers.get(id);
			if (observer != null) {
				observer.send(new ObservedDied(id, reason));
			}
		}		
	}

	private void notifyLinks(Set<Actor> links, Throwable reason) {
		if (links == null) return;
		
		LinkedActorDied notification = new LinkedActorDied(this, reason);
		
		for (Actor a: links) {
			a.kill(notification);
		}		
	}

	private void interruptCurrentThread(Thread currentThread) {
		if (currentThread == null) return;
		currentThread.interrupt();
	}
	
	private final Executor theExecutor;
	private final IDGenerator generator;

	private volatile Behaviour theBehaviour;
	private volatile Map<Integer, Actor> observers;
	private volatile Set<Actor> links;
	private volatile BlockingQueue<Object> messages;
	private volatile boolean isRunning = false;
	private volatile Thread currentThread = null;
	private volatile boolean isSystem;
}
