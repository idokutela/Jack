package com.alexanderkahle.garbo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * The Director is responsible for managing all of the actors, and marshalling messages
 * between them. It is a singleton, obtained by {@link Director#getTheDirector()}.
 *  
 * Actors should never refer to the director directly, instead using their 
 * {@link ExecutionContext} to communicate with other actors. Generally, one uses
 * {@link Director#createActor(Behaviour) createActor} to create the bootstrap
 * actor externally, and from that point on never refers to the director again,
 * relying on the actors to run inside their execution contexts.
 * 
 * The director has an overview of the entire landscape of actors. (TODO) 
 * One may hook up a logger to monitor everything (at various levels of detail).
 * 
 * Directors schedule execution of actors using {@link Scheduler Schedulers}.
 * These are registered with {@link Director#registerScheduler(String, Scheduler)}.
 * A director must have at least one scheduler registered before any actors are
 * created! If only one scheduler is registered, this is automatically the default
 * scheduler. If more are registered, one must explicitly set the default scheduler
 * with {@link Director#setDefaultScheduler(String) setDefaultScheduler}.
 * 
 * Implementation note: at the moment a lot of ops are synchronized together. Longer
 * term, this needs to be fine-grained.
 * 
 * Created by Alexander Kahle on 04.03.18.
 */

public class Director {
	public static final String DEFAULT_SCHEDULER = "com_kahle_alexander_Director_DEFAULT_SCHEDULER";
	public static final long NONEXISTANT_ID = 0;
	public static final int MAX_QUEUE_SIZE = 10000000;
	
	/**
	 * Gets the only instance of the director.
	 * 
	 * @return The Director singleton.
	 */
    public static Director getTheDirector() {
    		if (THE_DIRECTOR == null) {
    			THE_DIRECTOR = new Director();
    		}
        return THE_DIRECTOR;
    }
    
    public static long generateID() {
    		long id;
    		while ((id = generator.nextLong()) != NONEXISTANT_ID);
    		return id;
    }

    /**
     * Alias for <code>createActor(initialBehaviour, null, null)</code>.
     * 
     * @param initialBehaviour The initial behaviour of the actor to be created.
     * @return the actor's id, used to refer to the actor externally.
     * @see Director#createActor(Behaviour, String, String)
     */
    public long createActor(Behaviour initialBehaviour) {
        return createActor(initialBehaviour, null);
    }
    
    /**
     * Alias for <code>createActor(initialBehaviour, description, null)</code>.
     * 
     * @param initialBehaviour The initial behaviour of the actor to be created.
     * @param description A string describing the actor, for use in logging.
     * @return the actor's id, used to refer to the actor externally.
     * @see Director#createActor(Behaviour, String, String)
     */
    public long createActor(Behaviour initialBehaviour, String description) {
        return createActor(initialBehaviour, description, null);
    }
    
    /**
     * Creates an actor, and schedules it with the given scheduler. The actor may
     * have a description associated with it, which will be echoed in logging.
     * 
     * @param initialBehaviour The initial behaviour of the actor to be created.
     * @param description A string describing the actor, for use in logging.
     * @param scheduler The name of the scheduler to use for the actor. If null,
     *                  the default scheduler will be used.
     * @return the actor's id, used to refer to the actor externally. 
     * @throws NullPointerException if the initialBehaviour is null
     * @throws IllegalArgumentException if the scheduler is unknown
     */
    public long createActor(Behaviour initialBehaviour, String description, String scheduler) {
    		if (initialBehaviour == null) throw new NullPointerException("An actor must have an initial behaviour");
    		Scheduler theScheduler = getScheduler(scheduler);
    		if (theScheduler == null) throw new IllegalArgumentException("Unknown scheduler");
    		
    		Actor newActor = new Actor(initialBehaviour, description);   		
		ActorRecord newRecord = new ActorRecord(theScheduler);
		ActorRecord result = actors.putIfAbsent(newActor.ID, newRecord);
		if (newRecord == result) { // the likely case
			theScheduler.scheduleActor(newActor);
			return newActor.ID;
		}

		// The extremely unlikely case of a collision!
		return createActor(initialBehaviour, description, scheduler);
    }
       
    /**
     * Sends a message to the given actor. Fails silently if the actor does not
     * exist.
     * 
     * @param id The id of the recipient.
     * @param message The message to send to the recipient.
     */
    public void sendMessage(long id, Object message) {
    		if (message == null) throw new NullPointerException("Cannot send a null message!");
    		ActorRecord actorRecord = actors.get(id);
    		if (actorRecord == null) return;
    		actorRecord.scheduler.relayMessage(id, message);
    }
    
    /**
     * Registers a scheduler for use by the director. If it is the only scheduler,
     * it will be default. Otherwise, the default scheduler is that with the name
     * {@link Director#DEFAULT_SCHEDULER}. Once a scheduler is set, it cannot be
     * unset.
     * 
     * @param name The name of the scheduler.
     * @param scheduler The scheduler to register.
     * @throws NullPointerException if either argument is null.
     * @throws RuntimeException if the scheduler is already registered.
     */
    public void registerScheduler(String name, Scheduler scheduler) {
    		if (scheduler == null) throw new NullPointerException("The scheduler must not be null");
    		if (name == null) throw new NullPointerException("The name must not be null.");
    		
    		if (schedulers.putIfAbsent(name, scheduler) != scheduler) {
    			throw new RuntimeException("The scheduler " + name + " is already registered!");
    		}
    }
    
    void setExitTrapping(long id, boolean trap) {
    		ActorRecord record = actors.get(id);
    		if (record == null) return;
    		record.trapsExit = trap;
    }

    synchronized void kill(long id, Throwable reason) {	
    		ActorRecord record = actors.remove(id);
    		if (record == null) return;

    		record.scheduler.kill(id); // This must be here to avoid the race of kill reasons
	    	for (long key: record.watches.keySet()) {
	    		long watcher = record.watches.get(key);
	    		sendMessage(watcher, new TerminationMessage(key, reason));
	    	}
	    	for (long key: record.linkages) {
	    		ActorRecord linkedActor = actors.get(key);
	    		if (linkedActor == null) continue;
	    		if (linkedActor.trapsExit) {
	    			sendMessage(key, new LinkTerminationMessage(reason));
	    		} else {
	    			kill(key, reason);
	    		}	
	    	}
    }

    
    synchronized long addWatch(long subject, long object) {
		long monitorID = generateID();
		    		
		ActorRecord subjectRecord = actors.get(subject);
		if (subjectRecord == null) {
			// This should never happen!
			throw new RuntimeException("Attempting to watch from a nonexistant actor."
					+ " This is likely an implementation error. Please report!");
		}

		ActorRecord record = actors.get(object);
		if (record == null) {
			sendMessage(subject, new TerminationMessage(monitorID, null));
		} else if (record.watches.containsKey(monitorID)) { // should hopefully almost
															// never happen!
			return addWatch(subject, object);
		} else {
			record.watches.put(monitorID, subject);
		}

        return monitorID;
    }

	synchronized void removeWatch(long id, long ref) {
		ActorRecord record = actors.get(id);
		if (record == null)
			return;
		record.watches.remove(ref);
	}

	synchronized void bind(long id1, long id2) {
		ActorRecord r1 = actors.get(id1);
		ActorRecord r2 = actors.get(id2);

		if (r1 == null && r2 == null) {
			throw new RuntimeException(
					"Attempting to add a link between two nonexistant actors! "
							+ "This almost certainly indicates an unintended race. Please report!");
		}

		if (r1 == null) {
			sendMessage(id2, new LinkTerminationMessage(null));
			return;
		}

		if (r2 == null) {
			sendMessage(id1, new LinkTerminationMessage(null));
			return;
		}

		r1.linkages.add(id2);
		r2.linkages.add(id2);

	}

    synchronized void unbind(long id1, long id2) {
		ActorRecord r1 = actors.get(id1);
		ActorRecord r2 = actors.get(id2);

		if (r1 != null) r1.linkages.remove(id2);
		if (r2 != null) r2.linkages.remove(id1);	
    }


    long registerAlias(String alias, long id) {
    		return aliases.putIfAbsent(alias, id);
    }
    
    boolean replaceAlias(String alias, long oldID, long newID) {
    		return aliases.replace(alias, oldID, newID);
    }
    
    Long retrieveAlias(String alias) {
        return aliases.get(alias);
    }
    
    void deregisterAlias(String alias) {
    		aliases.remove(alias);
    }

    void sendMessage(long from, long to, Object message)  {
    		sendMessage(to, message); // TODO add logging
    }
    
    private Scheduler getScheduler(String name) {
    		Scheduler result = null;
    		if (name == null || name == DEFAULT_SCHEDULER) {
    			result = schedulers.get(DEFAULT_SCHEDULER);
    			if (result == null) {
    				synchronized (this) {
    					if (schedulers.size() == 1)
    						result = schedulers.values().iterator().next();
    				}
    			}
    			return result;
    		}
    		return schedulers.get(name);
    }

    
    private static Director THE_DIRECTOR;
	private static Random generator = new Random();

    private Director() {}
    
    private ConcurrentMap<String, Scheduler> schedulers = new ConcurrentHashMap<>();
    private ConcurrentMap<Long, ActorRecord> actors = new ConcurrentHashMap<>(); 
    private ConcurrentMap<String, Long> aliases = new ConcurrentHashMap<>(); 
    
    private class ActorRecord {
    		volatile boolean trapsExit = false;
    		final Scheduler scheduler;
    		final Set<Long> linkages = new HashSet<>();
    		final Map<Long, Long> watches = new HashMap<>();
    		
    		ActorRecord(Scheduler scheduler) {
    		  this.scheduler = scheduler;
    		}
    }
  }
