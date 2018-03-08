package com.alexanderkahle.garbo;

/**
 * The ExecutionContext is the means whereby a {@link Behaviour} can communicate with its
 * surroundings, and discover information about its self.
 * 
 * Created by Alexander Kahle on 04.03.18.
 */

public interface ExecutionContext {
	/**
	 * Gets the ID of the {@link Actor} running the {@link Behaviour}.
	 * @return the ID of the {@link Actor} running the {@link Behaviour}.
	 */
    long self();
    
    /**
     * Toggles whether the {@link Actor} survives a linked {@link Actor} terminating.
     *  
     * @param shouldAccept the actor accepts exit messages if set true, otherwise dies.
     */
    void trapExit(boolean shouldAccept);
    
    /**
     * Causes the actor to block until the next message is available on the queue.
     * Note, an actor "receiving" locks the thread it is on: this can be expensive!
     * If memory is tight, rewrite your behaviour to receive messages via parameters
     * passed.
     * 
     * @return The next message passed to the behaviour.
     */
    Object receive() throws InterruptedException;
    
    /**
     * Returns true if the actor has been requested to terminate. Long running 
     * actors should check this periodically, and abort with an 
     * {@link InterruptedException} if set true. 
     * 
     * @return true if the actor has been <code>kill</code>ed.
     */
    boolean shouldDie();
    
    /**
     * Sends a message from the {@link Actor} to the given recipient. If there
     * is no actor associated to the ID, then the message is silently lost. There is 
     * no guarantee on the ordering of messages sent to different actors, but messages
     * sent to a single actor are temporally ordered.
     * 
     * @param recipient the ID of the recipient.
     * @param message the message to send to the recipient.
     */
    void sendMessageTo(long recipient, Object message);

    /**
     * Shorthand for <code>createActor(initialBehaviour, null, null)</code>.
     * 
     * @param initialBehaviour The behaviour that the created actor first displays.
     * @return The ID of the created {@link Actor}
     * @see ExecutionContext#createActor(Behaviour, String)
     */
    long createActor(Behaviour initialBehaviour);
    
    /**
     * Shorthand for <code>createActor(initialBehaviour, description, null)</code>.
     * @param initialBehaviour he behaviour that the created actor first displays.
     * @param description The description of the created actor - reported in logging.
     * @return The ID of the created {@link Actor}
     */
    long createActor(Behaviour initialBehaviour, String description);
    
    /**
     * Creates an actor with the given initial {@link Behaviour} and description. The actor
     * is guaranteed to have started before the function returns. However, it may already have
     * died by the return of the next function! 
     * 
     * @param initialBehaviour The behaviour that the created actor first displays.
     * @param description The description of the created actor - reported in logging.
     * @param scheduler The name of the scheduler that manages the execution of the new actor.
     * @return The ID of the created {@link Actor}
     */
    long createActor(Behaviour initialBehaviour, String description, String scheduler);
    

    /**
     * Alias for <code>kill(id, null)</code>.
     * 
     * @param id The id of the actor to kill.
     * @see ExecutionContext#kill(long, Throwable)
     */
    void kill(long id);
    
    /**
     * Kills the given actor as soon as possible, with the given reason. 
     * Note – There is absolutely no temporal guarantee about the relationship between
     * killing and message processing. Killing killed (or nonexistent) actors is a noop.
     * 
     * @param id The ID of the actor to kill.
     * @param reason The reason to kill the actor.
     */
    void kill(long id, Throwable reason);

    /**
     * Watches the actor with the given id for death. If it dies, a {@link TerminationMessage}
     * is received. Each watch has an ID associated with it. Note, the same actor can be watched
     * more than once. Attempting to watch an actor that does not exist causes a TerminationMessage
     * with reason {@link DNE} to be sent. Watches are removed immediately upon the death of an
     * actor.
     * 
     * @param id The id of the actor to watch.
     * @return The watch id.
     */
    long watch(long id);
    
    /**
     * Removes the watch associated to the given actor with the given watch id.
     * Is a noop if the watcher is not registered on that actor. 
     * 
     * @param id The id of the actor to stop watching.
     * @param ref The name of the watch to remove
     */
    void stopWatching(long id, long ref);
    
    /**
     * Alias for <code>bindPair(self(), id)</code>.
     * 
     * @param id The id of the Actor to bind to.
     * @see ExecutionContext#bindPair(long, long)
     */
    void bind(long id);
    
    /**
     * Binds a pair of Actors. If either dies, the other is killed, unless it has set trapExit to
     * true. In the latter case, a LinkTerminationMessage is sent. Unlike watches, bindings are
     * univalent. In particular, bindPair is idempotent. Note, bindings are created asynchronously. Use
     * {@link createAndBind} to resolve the race. 
     * 
     * @param id1 The first actor to bind
     * @param id2 The second actor to bind
     */
    void bindPair(long id1, long id2);
    
    /**
     * Alias for <code>unbindPair(self(), id)</code>.
     * @param id The id of the actor to unbind.
     * @see ExecutionContext#unbindPair(long, long)
     */
    void unbind(long id);
    
    /**
     * Unbinds a pair of actors. The same concurrency caveats apply. A noop if either actor does not
     * exist.
     * @param id1 the id of an actor to bind
     * @param id2 the id of an actor to bind
     */
    void unbindPair(long id1, long id2);

    /**
     * Registers an alias for the given ID. If the alias is registered for a different ID, is a noop.
     * Idempotent. If unable to register an alias, returns false. Note, the ID need not be associated with 
     * an existing actor!
     * 
     * @param alias The alias to register
     * @param id The id to associate with the alias.
     * @return true on success
     */
    boolean registerAlias(String alias, long id);

    /**
     * Replaces the value associated with an alias, only if the alias is associated with the given previous
     * value.
     * 
     * @param alias The alias to replace the values for
     * @param prevId The expected id
     * @param nextId The new id
     * @return true if the action was successful.
     */
    boolean replaceAlias(String alias, long prevId, long nextId);

    /**
     * Removes the association between an alias and a process.
     * Idempotent.
     * 
     * @param alias The alias to clear.
     */
    void deregisterAlias(String alias);
    
    /**
     * Retrieves the process associated to a given alias.
     * 
     * @param alias The alias of the process to retrieve
     * @return the ID of the associated process, or NONEXISTANT_ID if there is no association.
     */
    long retrieveAlias(String alias);
}
