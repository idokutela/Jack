package com.alexanderkahle.garbo;

/**
 * Actors are defined by their behaviour. The <code>run</code> method is called
 * on each new message, and returns the behaviour to run on the next message. If
 * <code>null</code> is returned, or an exception is thrown, the Actor dies. 
 * 
 * Created by Alexander Kahle on 04.03.18.
 */

public interface Behaviour {
    /**
     * This processes the next message in the actor’s mailbox. It returns the
     * behaviour used to process the following message, or null. In the latter case
     * the actor terminates. The actor also terminates if an exception is thrown.
     * 
     * Java does not allow one to terminate threads without cooperation of that 
     * thread. In order to fail-fast, behaviours should treat InterruptedExceptions
     * as signals to terminate, and only catch them if cleanup is needed, rethrowing
     * them after the cleanup has been performed. Long-running computational processes
     * should periodically check {@link ExecutionContext#shouldDie()}, and if true,
     * terminate with an <code>InterruptedException</code>.
     * 
     * Recommendations: It is an highly recommended to write this as statelessly as possible.
     *
     * @param  context The execution context for the running behaviour. 
     *                Use this to communicate with the external environment.
     * @param  message The message to process
     * @return The new behaviour to run on the next message
     * @throws Throwable if thrown, causes the actor to terminate and the throwable to be sent to all monitors
     */
    Behaviour run(ExecutionContext context, Object message) throws Throwable;
}
