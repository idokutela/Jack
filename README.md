# Actors

A tiny actor library for Java 7 and up. No dependencies, no complications.

**WARNING: This is still very young. It is likely to change quite a bit!**

## Gist

```java
/*
 * This gist is written in Java 8 because lambdas.
 */
 
import com.alexanderkahle.Jack.ActorBuilder;
import com.alexanderkahle.Jack.Actor;
import com.alexanderkahle.Jack.Behaviour;
import java.util.concurrent.ThreadPoolExecutor;

class MainClass {
    public static void main(String[] args) {
        ActorBuilder buildActor = new ActorBuilder();
        
        class HelloMessage {
            public String greeting;
            public Actor sender;
            
            HelloMessage(Actor sender, String greeting) {
                this.sender = sender;
                this.greeting = greeting;
            }
        }
        
        final Actor receiver = buildActor
            .initialBehaviour(
                (Actor self, Object message) -> {
                    if (!(message instanceof HelloMessage)) {
                        // die quickly: if a behaviour throws, it dies with
                        // the death reason being that exception
                        throw new RuntimeException("Unexpected message received by receiver!"); 
                    }
                
                    HelloMessage m = (HelloMessage) message;
                    System.out.println("Received " + m.greeting);
                    m.sender.send(new HelloMessage(self, m.greeting + " back at ya!"));
                    
                    // The return value of a behaviour is the behaviour
                    // the actor becomes for the next message.
                    // In this case, we want to keep looping with
                    // the same behaviour
                    return this; 
                })
                .build();
            
        final String initialise = "START!";
        
        // Builders can be reused: they clear themselves after every build
        // They are, however, NOT thread safe!
        final Actor sender = buildActor 
            .initialBehaviour((Actor self, Object message) -> {                
                if (initialise.equals(message)) {
                    receiver.link(this); // link yourself to the receiver
                    receiver.send(new HelloMessage(self, "Hello mate"));
                    System.out.println("Initialised sender");
                    
                    // become a behaviour to deal with the reply
                    return (Actor self, Object message) -> {
                        if (!(message instanceof HelloMessage)) {
                            // die quickly: if a behaviour throws, it dies with
                            // the death reason being that exception
                            throw new RuntimeException("Unexpected message received by receiver!"); 
                        }
                        
                        System.out.println("Received a reply of " + 
                            ((HelloMessage) message).greeting);
                        receiver.die(null); // kill regularly
                        return this;  // keep going
                    }); 
                }
                throw new RuntimeException("Unexpected message received by the sender!");
            })
            .build();
        
        // A monitor can monitor other actors lives
        // This one watches for both the receiver and the sender's deaths.
        // When both are dead, it kills itself.
        
        final Actor monitor = buildActor
            .isSystem(true) // won't die when linked actors die
            .initialBehaviour((Actor self, Object message) -> {
                if (intialise.equals(message)) {
                    self.link(receiver);
                    self.link(sender);
                    sender.send(initialise);
                    System.out.println("Initialised monitor");
                    
                    return (Actor self, Message message) -> {
                        if (!(message instanceof LinkedActorDied))
                            throw new RuntimeException("Unexpected message received by the monitor!");
                            
                        System.out.println("Received first death report");
                        if (message.actor == sender) System.out.println("Sender died");
                        else System.out.println("Receiver died");
                        
                        return (Actor self, Message message) -> { // one can encode state through behaviour changes
                            if (!(message instanceof LinkedActorDied))
                                throw new RuntimeException("Unexpected message received by the monitor!");
                            
                            System.out.println("Received second death report.");
                            if (message.actor == sender) System.out.println("Sender died");
                            else System.out.println("Receiver died");
                            return null;
                        };
                    };
                }
                
                throw new RuntimeException("Unexpected message received by the monitor!");                
            });
    
        monitor.send(initialise);
    }
}

/*
 * Possible output:
 * Initialised monitor
 * Received Hello, mate
 * Initialised sender
 * Received a reply of Hello, mate back at ya!
 * Received first death report
 * Sender died
 * Received second death report
 * Receiver died
 */
```

## Background
This library is a tiny library implementing Actor based concurrency, strongly
inspired by Erlang.

Actors can be thought of autonomous concurrent units, that communicate
with each other via message passing. Each actor has two pieces of state:
its current *behaviour*, and its *message box*. Messages are queued in the
message box, and are processed in turn by the behaviour. There are two
basic operations that actors should support:

  - *become*: this changes the actor’s behaviour to a new behaviour, and
  - *send*: which sends a message to another actor.
  
Erlang based its entire language on actors. Its primary goal was to afford
programmers with an environment to build highly reliable systems. In order
to do so, it introduced three other primitives to the actor model:

  - *kill*: this terminates an actor,
  - *link*: this binds two actors together. The nature of the link depends
    on whether an actor traps exits or not. By default, actors do not: if
    so, and a linked actor termintes, they also do. If an actor does trap
    exits, it receives a message when a linked actor has terminated. Links
    are univalent and symmetrical – only one link may exist between a pair 
    of actors, and both actors that are linked react to the termination
    of the other.
  - *monitor*: an actor may monitor another actor. If the monitored actor
    dies, the actor monitoring it receives a message informing it of
    the actor's death. Unlike links, the monitor relationship is polyvalent
    and directed: an actor may monitor another without the other monitoring
    it, and an actor may monitor another multiple times.
    
*Jack* implements this “Erlang” actor model.

## API
This is meant as an overview of how to use the API. Please refer to the 
Javadoc for precise details.

Actors are represented by the `Actor` class. One creates actors with
an `ActorBuilder`. All one really needs is the initial `Behaviour` of
the actor: this is an `Object` satisfying the `Behaviour` interface,
which means it must have a function `run: Actor x Object -> Behaviour`.
This function is called on the next message, and has the following semantics:

 - the first argument is the `Actor` running the behaviour,
 - the second argument is the message being passed to the behaviour,
 - the return value is the `Behaviour` that should be called on the next
   message received.
 - the behaviour terminates the actor if it throws an exception,
 - the behaviour terminates the actor if it returns null.
   
A simple example should suffice:

```java
class Doubler implements Behaviour {
    public Doubler(Actor target) {
        this.target = target;
    }
    
    public Behaviour run(Actor self, Object message) {
        Integer number = (Integer) message; // If the cast fails, the actor terminates
        target.send(2 * number); // similarly, if target is null, the actor terminates
        return this; // the behaviour just loops
    }
    
    private final Actor target;
}

class Printer implements Behaviour {
    public Behaviour run(Actor self, Object message) {
        System.out.println(message);
        return this;
    }
}

ActorBuilder builder = new ActorBuilder();

Actor target = builder
    .initialBehaviour(new Printer())
    .build();

Actor doubler = builder
    .initialBehaviour(new Doubler(target))
    .build();
  
doubler.send(5); // -> 10 is printed to console
doubler.send(6); // -> 12 is printed to console
```

Actors have four methods that together implement the basic actor model:

 - `send`: places a message on the actor’s mailbox for processing. One
    cannot send `null` messages.
 - `die`: kills the actor. One can supply a `Throwable` as the reason
    for the death.
 - `addObserver`: adds an actor to the set of actors monitoring the given
   actor, and returns a reference to the monitor relationship. Calling
   `removeObserver` with this reference undoes the relationship. Observers
   receive `ObservedDied` messages when the subject of the observation
   died, containing the reference to the observation, and the reason
   of death.
 - `link`: links the actor with another one. `unlink` undoes the linkage.
   By default, an actor terminates with the reason `LinkedActorDied`
   if any linked actor terminates. If the actor has `trapExit` set when
   built, it does not terminate, instead receives the `LinkedActorDied`
   in its inbox. A `LinkedActorDied` contains a reference to the dead actor,
   and the reason that actor died.
 
In addition, Actors have a `takeNextMessage` method. This should only be
called in the context of an actor’s behaviour: it takes the next message
from the inbox, and blocks until one is available. Due to its blocking nature,
it should be used sparingly: it hogs the thread the behaviour is running
on until a message is available.

Actors are created using instances `ActorBuilder`. These factories are 
*not* thread safe, but can be reused. Generally, there are three properties
one might set on an Actor to be built:

  - `initialBehaviour`: this *must* be set, and specifies the behaviour 
    to be used on the first message received.
  - `trapExit`: when set true, the actor will not die when linked actors
    die, but will instead receive a `LinkedActorDied` message.
  - `executor`: Actor’s run their behaviour with an `Executor`. The builder
    creates an executor when constructed using `Executors.newCachedThreadPool`,
    and shares this over every actor it creates. This is fine for testing,
    but for production you probably want to share thread pools across many
    builders, and likely want different executors for different types
    of actors (IO bound, compute, etc.).
    
In addition, one may set a default executor to use on all Actors created
subsequently, using `setDefaultExecutor`. Newly build actors are inactive
until they receive a message.

Here’s a simple example:

```java
Executor appActorExecutor = new Executors.newCachedThreadPool();

ActorBuilder theBuilder = new ActorBuilder();

// An actor that prints "Hello, world!" on receipt of its first message,
// then terminates. 
Actor helloWorldActor = theBuilder
    .initiialBehaviour((Actor self, Object message) -> {
        System.out.println("Hello, world!");
        return null;
    })
    .executor(appActorExecutor);

// Start the hello world by sending it a message.
helloWorldActor.send(new Object());
```

## Gotchas
At the moment, `link` and `unlink` contain races. If any of the actors
die before the function completes, notification might not occur of that
actors death. 

## Hints for effective use
### State

Shared mutable state is the enemy of sane concurrency. If an actor is stateful,
it is an excellent idea to encapsulte the state inside the actor’s behaviour,
creating a new behaviour each time the state changes. For example, one may
implement an actor that simply counts the number of messages received,
sending the result to some other actor, as follows:

```java
class CountBehaviour implements Behaviour {
    public final int count;
    public final Actor recipient;
    
    public CountBehaviour(Actor recipient) {
        this.count = 0;
        this.recipient = recipient;
    }
    
    public Behaviour run(Actor self, Object message) {
        recipient.send(count);
        return new CountBehaviour(this.count + 1);
    }
}
```

Naturally, libraries with performant immutable collections are an excellent
fit for Jack!

As we will see in the ‘Testing’ section, it is an excellent idea to make 
all Behaviour state public and final.

### Cleaning up on termination

Following the ‘fail fast’ philosophy, one should design actors so that 
they can be arbitrarily terminated or restarted, delegating any necessary
cleanup to observing actors. Thus, for example, one might have an actor
that does file IO, and another that watches it, releasing the file handles
when that actor terminates, before terminating itself.

### Long running behaviours

Java does not possess the ability to terminate a thread cleanly. Jack is
forced to `interrupt` a thread running an actor in order to signal termination.
For IO-bound behaviours, or short running behaviours, this works excellently.
The only caveat is that one should catch for `InterruptedException`s
unless one explicitly wants to ignore termination signals, or one has
some state one needs to clean up before terminating. The latter, however,
is generally a bad idea. See [Cleaning up on termination] for more.

Behaviours that perform long-running computations, however, require 
special handling. To avoid hogging a thread, they should periodically
check `Thread.isInterrupted()`, and terminate early if this returns true.
Below, for example, is a behaviour that attempts to find all the factors
of a number (extremely inefficiently!) and sends these to another actor as
they are found.

```java
class FactorisationBehaviour {
    public FactorisationBehaviour(Actor recipient) {
        this.recipient = recipient;
    }
    
    public Behaviour run(Actor self, Object message) {
        // Note, this will terminate if the message cannot be cast to
        // an integer, or if the resulting integer is non-positive.
        
        int toFactorise = (Integer) message;
        if (toFactorise <= 0) {
            throw new IllegalArgumentException("Can only factorise positive numbers");
        }
            
        for (int i = 1; i <= toFactorise; i++) { // 1 and toFactorise are factors!
            if (toFactorise % i == 0) {
                recipient.send(i);
                recipient.send(toFactorise / i);
            }
            
            // make sure we can continue
            if (Thread.isInterrupted()) return null; 
        }
        
        return this; // If we get here, we've factorised teh num
    }
    
    public final Actor recipient;
}
```

### Creating links

As mentioned in “Gotchas”, the creation and removal of a link has a race: 
if either actor involved dies while the link is being created, it may or
may not happen that the other linked actor finds out about the death. It
is thus a *very* good idea to create any links directly after one of the
actors is created, and *prior* to it receiving any messages, and to design
ones system in such a way that links do not need to be removed explicitly.

### Using `takeNextMessage`

The Actor method `takeNextMessage` removes the next message from an Actor’s 
inbox, blocking if the inbox is empty. It should not be used outside of
the actor’s currently running behaviour, unless you want a system that
is horrible to reason about. I plan to enforce this in the future.

It should also be used very sparingly inside an actor’s behaviour: it 
blocks if there is nothing in the inbox, necessarily holding onto the 
thread that the behaviour is running in. By contrast, only taking messages
as arguments to behaviours allow the threads to be released back to the
executor after each run of the behaviour, for use by other actors. 

A good idea, if one finds one is tempted to use `takeNextMessage`, is
to go ahead and do so initially. Then, when one has the behaviour working
as expected, one should refactor the behaviour into multiple smaller 
behaviours, each running from a given `takeNextMessage` to the next one.

For example, the following collects pairs of incoming messages into a 
stream of pairs, which it sends to some recipient:

```java
class CollectTwoBehaviour {
    public final Actor recipient;
    
    public CollectTwoBehaviour(Actor recipient) {
        this.recipient = recipient;
    }
    
    public Behaviour run(Actor self, Object message) {
        Object[] pair = new Object[2];
        pair[0] = message;
        // Bad: it blocks here until the next message comes
        pair[1] = self.takeNextMessage(); 
        recipient.send(pair);
        return this;
    }
}
```

One may refactor `CollectTwoBehaviour` to avoid the blocking as follows:

```java
class CollectTwoBehaviourInitial {
    public final Actor recipient;
    
    public AddTwoBehaviourInitial(Actor recipient) {
        this.recipient = recipient;
    }
    
    public Behaviour run(Actor self, Object message) {
        return new CollectTwoBehaviourFinal(recipient, message);
    }
}

class CollectTwoBehaviourPartFinal {
    public final Actor recipient;
    public final Object first;
    
    public CollectTwoBehaviourFinal(Actor recipient, Object first) {
        this.recipient = recipient;
        this.first = first;
    }
    
    public Behaviour run(Actor self, Object message) {
        recipient.send([first, message]);
        return new CollectTwoBehaviourInitial(recipient);
    }
}
```

### Testing

Behaviours become extremely testable if all of their state is immutable,
public and final and one uses changes the actor behaviour to reflect 
state change. Compare the following two implementations of a counting behaviour:

```java
class CountImmutable implements Behaviour {
    public final int count;
    public final Actor recipient;
    
    public CountBehaviour(int count, Actor recipient) {
        this.count = count;
        this.recipient = recipient;
    }
    
    public Behaviour run(Actor self, Object message) {
        recipient.send(count);
        return new CountBehaviour(count + 1, recipient);
    }
}

class CountMutable implements Behaviour {
    private int count;
    private Actor recipient;
    
    public CountBehaviour(Actor recipient) {
        this.count = 0;
        this.recipient = recipient;
    }
    
    public Behaviour run(Actor self, Object message) {
        recipient.send(count);
        count ++;
        return this;
    }
}
```

Both have the same functionality, but the first is much more testable:
one can instantiate it on a given count and Actor stub, and call run,
verifying that the new behaviour has the correct state, and the send
method of the actor stub has been called with the correct value.
The second implementation has to be tested by calling it at least twice:
once to verify the actor has been called with the current value, and again
to verify that the internal state has changed. Of course, there is not much
of a difference in this simple example, but as behaviours get more complicated,
the difference becomes marked.

### Mixing concurrency models

It is almost inevitable that one will end up mixing concurrency models 
when using *Jack*: the dominant models of concurrency in Java are not 
actor based. One needs to be very careful when doing so: it is easy to end
up entangling state between different calls to behaviours or different actors.
As much as possible, one should attempt to push the interface of ones 
Actor-based code with the rest of the system to a small and clear boundary,
and spending effort making sure this boundary does not result in leaks or
entanglements.

## Android

Jack was designed specifically to be easy to use when developing for
Android. It has no dependencies, and is tiny. When using Jack for mobile
development, it is highly recommended that one customises the executor
used, so that, for example, different actors have different thread priorities
assigned to them. 

## License
MIT. See LICENSE.txt .

