package com.alexanderkahle.Jack;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;

public class Actor implements Runnable {
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
	
	public Integer addObserver(Actor a) {
		if (a == null) throw new NullPointerException("Cannot observe null.");
		
		Map<Integer, Actor> observers;
		Integer observationID;
		
		synchronized (this) { 
			observers = this.observers;
			
			if (observers != null) {
				// Make sure the observationID is unique
				while (observers.get(
						observationID = randomizer.nextInt()
						) != null) {} 
				observers.put(observationID, a);
				return observationID;
			}
		}
		return null;
	}
	
	public void removeObserver(int watchId) {
		Map<Integer, Actor> observers;
		synchronized (this) {
			observers = this.observers;
			if (observers == null) return;
			observers.remove(watchId);
		}
	}
	
	public void kill(Throwable reason) {
		Thread currentThread;
		Map<Integer, Actor> observers;
		
		synchronized (this) {
			currentThread = this.currentThread;
			observers = this.observers;
			
			this.theBehaviour = null;
			this.currentThread = null;
			this.observers = null;
			this.messages = null;
			this.isRunning = false;
		}
		
		
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
			BlockingQueue<Object> messages, Random randomizer) {
		theBehaviour = initialBehaviour;
		theExecutor = executor;
		this.observers = observers;
		this.messages = messages;
		this.randomizer = randomizer;
		isRunning = false;
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
	
	private void interruptCurrentThread(Thread currentThread) {
		if (currentThread == null) return;
		currentThread.interrupt();
	}
		
	
	private final Executor theExecutor;
	private final Random randomizer;

	private volatile Behaviour theBehaviour;
	private volatile Map<Integer, Actor> observers;
	private volatile BlockingQueue<Object> messages;
	private volatile boolean isRunning = false;
	private volatile Thread currentThread = null;
}
