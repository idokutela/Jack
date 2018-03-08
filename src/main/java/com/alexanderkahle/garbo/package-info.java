/**
 * <code>Actors</code> is a small, flexible framework for Actor based concurrency
 * in Java. Actors is deliberately target at Java 7, without any dependencies.
 * Its approach is very strongly inspired by {@linkplain https://www.erlang.org/ 
 * Erlang}.
 *  
 * Actors are all managed by the {@link Director}. An actor is defined by its {@link 
 * Behaviour}: and more particularly, by the behaviour’s <code>run</code> function. 
 * This is called on messages received by the actor, and returns the behaviour to 
 * call on the next message. The run function also has an {@link ExecutionContext}
 * associated with it, that it uses to communicate with its environment. It is 
 * <em>highly recommended</em> that behaviours exclusively use the execution environment
 * to communicate with their surroundings, and do not share state (for example, by
 * behaviour reuse).
 * 
 * Messages are just non-null objects. There are only two inter-message concurrency
 * guarantees:
 * 
 * <ol>
 *  <li>A message will be delivered after it is sent,</li>
 *  <li>Messages sent from one actor to another will be delivered in order.</li>
 * </ol>
 * 
 * Messages sent to non-existent actors will fail silently.
 * 
 * Actors should fail fast. One can kill actors with {@link 
 * ExecutionContext#kill(long, Throwable) kill}. This will kill the actor as soon
 * as possible. Any messages in the actor's message box at the time of death will
 * not be delivered.
 * 
 * Unfortunately, Java does not possess a (non-deprecated) method to force a thread
 * to terminate. One must rely on {@link Thread#interrupt()}. Thus, in long-running
 * non-io bound behaviours, it is crucial to check {@link Thread#isInterrupted()}
 * frequently, and abort if it is set. (Question: add an isInterrupted to the
 * context?)
 * 
 * Actors monitor others in two ways: via links, or as watchers. Links are
 * bidirectional and univalent: linking one actor to another automatically reverses
 * the link. Linked actors die when either dies, unless the living actor has set
 * {@link ExecutionContext#trapExit(boolean) trapExit} to true, in which case it
 * receives a message that the other actor has died. Links are meant to tie together
 * coupled groups of actors.
 * 
 * Watches are unidirectional and multivalent: one actor may watch another many times,
 * without the other knowing about it. The watcher is informed every time the watched
 * actor dies.
 * 
 * A note on concurrency guarantees: there are no guarantees as to the speed and
 * the ordering of killing of linked actors, just that linked actors will be killed
 * when either dies. This should happen quickly, but it will not happen
 * instantaneously. Messages notifying of death are placed in an actor’s message
 * box in the normal manner. The only guarantee is that after receiving notification
 * of an actor's death, no more messages will be received from that actor.
 * 
 * The precise scheduling strategy for an actor is flexible. It is accomplished by
 * {@link Scheduler}. Please refer to the interface documentation if you wish to
 * implement your own scheduler.
 * 
 * @author Alexander Kahle
 */
package com.alexanderkahle.garbo;