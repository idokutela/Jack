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
        ActorBuilder.setDefaultExecutor(new ThreadPoolExecutor(/* args */));
        
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

## License
MIT. See LICENSE.txt .

