package com.alexanderkahle.garbo;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * An Actor runs a {@link Behaviour} on a message. This does the actual work.
 * Schedulers should call run repeatedly as long as it is alive.
 * Schedulers kill actors by calling interrupt on the thread the actor is running
 * on, or, if the actor is currently idle, never restarting it.
 * 
 * Actors are created using one's execution context, or the {@link Director#createActor}.
 *
 * Created by Alexander Kahle on 04.03.18.
 */

public class Actor implements Runnable {
    /**
     * The ID identifying the actor.
     */
    public final long ID;
    
    /**
     * A human readable description or name for the actor.
     */
    public final String description;


    /**
     * Creates an actor with the given behaviour. Note, it does not actually start the actor.
     *
     * @param initialBehaviour the behaviour used to process the first message.
     * @param description The human-readable description of the new actor
     */
    Actor(Behaviour initialBehaviour, String description) {
        if (initialBehaviour == null) {
            throw new NullPointerException("The initial behaviour must not be null.");
        }
        ID = Director.generateID();
        this.description = description;
        currentBehaviour = initialBehaviour;
    }

    /**
     * Queues a message up to be processed. Throws if there are too many messages in the queue.
     *
     * @param message The message to add to the queue
     * @throws NullPointerException if the message to queue is null.
     */
    public void queueMessage(Object message) {
    		if (message == null) throw new NullPointerException("Messages must not be null");
    		boolean accepted = messageQueue.offer(message);
        if (!accepted) {
        		terminateSelf(new BlockedException());
        }
    }
    
    /**
     * Attempts to stop the actor.
     * 
     * NB Schedulers must be careful to avoid a race. They must wait for stop to
     * return before attempting to schedule a new task on that thread.
     */
    public synchronized void kill() {
    		shouldDie = true;
    		Thread runningThread = this.runningThread;
    		if (runningThread != null) runningThread.interrupt();
    }
    
    /**
     * Runs the current behaviour once, eating a message in the queue. If no messages are present,
     * is a NOOP.
     */
    @Override
    public void run() {
		// Get rid of any interrupted flags before starting.
		Thread.interrupted();
    		synchronized (this) {
    			runningThread = Thread.currentThread();
    		}
    		try {
    			if (shouldDie) {
    				if (!messageQueue.isEmpty()) {
        				while (messageQueue.poll() != null) {} // empty the message queue    					
    				}
    				return;
    			}
    			
    			if (currentBehaviour == null) {
    				// This should never occur. If it does, the scheduler is in error!
    				throw(new RuntimeException("No behaviour for actor!"));
    			}

    			final Object message = messageQueue.poll();
    			if (message == null) {
    				return;
    			}

            currentBehaviour = currentBehaviour.run(context, message);
            if (currentBehaviour == null) {
            		terminateSelf(null);
            		return;
            }
    		} catch (Throwable t) {
    			terminateSelf(t);
    		} finally {
    			synchronized (this) {
    				runningThread = null;
    			}
    		}
    }




    /*-************************* IMPLEMENTATION ******************************-*/

    private final BlockingQueue<Object> messageQueue = new ArrayBlockingQueue<Object>(Director.MAX_QUEUE_SIZE);
    private ActorExecutionContext context = new ActorExecutionContext();
    private Behaviour currentBehaviour;
    private final Director theDirector = Director.getTheDirector();
    private volatile Thread runningThread;
    private volatile boolean shouldDie = false;
    

    private void terminateSelf(Throwable reason) {
        theDirector.kill(ID, reason);
    }

    private class ActorExecutionContext implements ExecutionContext {
        @Override
        public long self() {
            return ID;
        }

        @Override
        public void trapExit(boolean accept) {
            theDirector.setExitTrapping(ID, accept);
        }

		@Override
		public Object receive() throws InterruptedException {
			return messageQueue.take();
		}

		@Override
		public boolean shouldDie() {
			return shouldDie;
		}

		@Override
        public void sendMessageTo(long recipient, Object message) {
        		if (message == null) throw new NullPointerException("The message must not be null.");
            theDirector.sendMessage(ID, recipient, message);
        }


        @Override
        public long createActor(Behaviour initialBehaviour, String description) {
            return this.createActor(initialBehaviour, description, null);
        }

        @Override
        public long createActor(Behaviour initialBehaviour) {
            return this.createActor(initialBehaviour, null, null);
        }

		@Override
		public long createActor(Behaviour initialBehaviour, String description,
				String scheduler) {
            if (initialBehaviour == null) {
                throw new NullPointerException("The initial behaviour must not be null.");
            }
            return theDirector.createActor(initialBehaviour, description, scheduler);
		}


        @Override
        public void kill(long id) {
            theDirector.kill(id, null);
        }

        @Override
        public void kill(long id, Throwable reason) {
            theDirector.kill(id, reason);
        }

        @Override
        public long watch(long id) {
            return theDirector.addWatch(ID, id);
        }

        @Override
        public void stopWatching(long id, long watchID) {
            theDirector.removeWatch(id, watchID);
        }

        @Override
        public void bind(long id) {
            this.bindPair(ID, id);
        }

        @Override
        public void unbind(long id) {
            this.unbindPair(ID, id);
        }

		@Override
		public void bindPair(long id1, long id2) {
			theDirector.bind(id1, id2);
		}

		@Override
		public void unbindPair(long id1, long id2) {
			theDirector.unbind(id1, id2);
		}
		
        @Override
        public boolean registerAlias(String alias, long id) {
            return theDirector.registerAlias(alias, id) == id;
        }

        @Override
        public long retrieveAlias(String alias) {
            return theDirector.retrieveAlias(alias);
        }

        @Override
        public boolean replaceAlias(String alias, long oldID, long newID) {
            return theDirector.replaceAlias(alias, oldID, newID);
        }

        @Override
        public void deregisterAlias(String alias) {
            theDirector.deregisterAlias(alias);
        }
    }
}
