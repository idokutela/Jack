package com.alexanderkahle.Jack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;

import org.junit.Test;

public class ActorTest {

	@Test public void testActorRunsUntilMessagesEmpty() {
		
		BehaviourStub fakeBehaviour = new BehaviourStub(); 	
		ExecutorStub fakeExecutor = new ExecutorStub();
		
		Object message1 = new Object();
		Object message2 = new Object();
		
		List<Object> expectedMessageOrder = new ArrayList<>();
		expectedMessageOrder.add(message1);
		expectedMessageOrder.add(message2);

		ArrayBlockingQueue<Object> messages = new ArrayBlockingQueue<>(10);
		messages.addAll(expectedMessageOrder);


		Actor testActor = new Actor(fakeBehaviour, fakeExecutor, /* observers */ null, /* links */ null, 
				messages, /* generator */ null, /* isSystem */ false);
		
		
		testActor.run();
		
		assertEquals("The executor was called on the actor after a message was added",
				fakeExecutor.calledOn, testActor);
		assertEquals("The executor was called until the message queue was empty",
				fakeExecutor.callCount, 2);
		assertEquals("The behaviour called with the messages in order that was added",
				fakeBehaviour.messages, expectedMessageOrder);
		assertEquals("The behaviour was with the actor as self",
				fakeBehaviour.self, testActor);
		assertEquals("The behaviour was not called with each message",
				fakeBehaviour.callCount, 2);
	}	
	
	@Test public void testActorExecutesOnMessage( ) {
		BehaviourStub fakeBehaviour = new BehaviourStub(); 
		ExecutorStub fakeExecutor = new ExecutorStub();
		
		Object message = new Object();
		
		ArrayBlockingQueue<Object> messages = new ArrayBlockingQueue<>(10);
		
		Actor testActor = new Actor(fakeBehaviour, fakeExecutor, /* observers */ null, /* links */ null, 
				messages, /* generator */ null, /* isSystem */ false);
		
		assertNull("The executor was not called until a message was added",
				fakeExecutor.calledOn);
		assertEquals("The executor was not called until a message was added",
				fakeExecutor.callCount, 0);
		assertEquals("The behaviour was not called until a message was added",
				fakeBehaviour.messages.size(), 0);
		assertNull("The behaviour was not called until a message was added",
				fakeBehaviour.self);
		assertEquals("The behaviour was not called until a message was added",
				fakeBehaviour.callCount, 0);

		testActor.send(message);
		
		assertEquals("The executor was called on the actor after a message was added",
				fakeExecutor.calledOn, testActor);
		assertEquals("The executor was called twice after a message was added",
				fakeExecutor.callCount, 2); // Once to run, once to test there are no more messages.
		assertEquals("The behaviour called with the message that was added",
				fakeBehaviour.messages.get(0), message);
		assertEquals("The behaviour called once with the message that was added",
				fakeBehaviour.messages.size(), 1);		
		assertEquals("The behaviour was with the actor as self",
				fakeBehaviour.self, testActor);
		assertEquals("The behaviour was not called once",
				fakeBehaviour.callCount, 1);
	}
	
	@Test public void testActorAddsMessagesAtEnd() {
		
		BehaviourStub fakeBehaviour = new BehaviourStub(); 	
		ExecutorStub fakeExecutor = new ExecutorStub();
		
		Object message1 = new Object();
		Object message2 = new Object();
		Object addedMessage = new Object();
		
		List<Object> expectedMessageOrder = new ArrayList<>();
		expectedMessageOrder.add(message1);
		expectedMessageOrder.add(message2);

		ArrayBlockingQueue<Object> messages = new ArrayBlockingQueue<>(10);
		messages.addAll(expectedMessageOrder);

		expectedMessageOrder.add(addedMessage);

		Actor testActor = new Actor(fakeBehaviour, fakeExecutor, /* observers */ null, /* links */ null, 
				messages, /* generator */ null, /* isSystem */ false);
		
		
		testActor.send(addedMessage);
		
		assertEquals("The executor was called on the actor after a message was added",
				fakeExecutor.calledOn, testActor);
		assertEquals("The executor was called until the message queue was empty",
				fakeExecutor.callCount, 4);
		assertEquals("The behaviour called with the messages in order that was added",
				fakeBehaviour.messages, expectedMessageOrder);
		assertEquals("The behaviour was with the actor as self",
				fakeBehaviour.self, testActor);
		assertEquals("The behaviour was not called with each message",
				fakeBehaviour.callCount, 3);
	}	
	
	@Test public void testActorChangesBehavour() {
		BehaviourStub subsequentBehaviour = new BehaviourStub();
		BehaviourStub fakeBehaviour = new BehaviourStub(subsequentBehaviour); 	
		ExecutorStub fakeExecutor = new ExecutorStub();
		
		Object message1 = new Object();
		Object addedMessage = new Object();
		
		ArrayBlockingQueue<Object> messages = new ArrayBlockingQueue<>(10);
		messages.add(message1);
		
		Actor testActor = new Actor(fakeBehaviour, fakeExecutor, /* observers */ null, /* links */ null, 
				messages, /* generator */ null, /* isSystem */ false);
		
		testActor.send(addedMessage);
		
		assertEquals("The first behaviour was called with the first message",
				fakeBehaviour.messages.get(0), message1);
		assertEquals("The first behaviour was called only with the first message",
				fakeBehaviour.messages.size(), 1);
		assertEquals("The subsequent behaviour was called with the second message",
				subsequentBehaviour.messages.get(0), addedMessage);
		assertEquals("The subsequent behaviour was called only with the second message",
				subsequentBehaviour.messages.size(), 1);
	}
	
	@Test public void testActorOnlyExecutesBehaviourWhenMessagesAreAvailable() {
		BehaviourStub beh = new BehaviourStub();
		ExecutorStub ex = new ExecutorStub();
		ArrayBlockingQueue<Object> ms = new ArrayBlockingQueue<>(10);
		Actor a = new Actor(beh, ex, /* observers */ null, /* links */ null, 
				ms, /* generator */ null, /* isSystem */ false); 
		
		a.run();
		assertEquals("The behaviour was not called", beh.callCount, 0);
		assertEquals("The executor was not called", ex.callCount, 0);
	}
	
	@Test public void testActorDiesWhenThereIsNoNewBehaviour() {
		final BehaviourStub beh = new BehaviourStub(null);
		final ExecutorStub ex = new ExecutorStub();
		final ArrayBlockingQueue<Object> ms = new ArrayBlockingQueue<>(10);
		Object message = new Object();
		ms.add(message);
		
		ActorWithStubbedDie a = new ActorWithStubbedDie(beh, ex, null, null, ms, null, false);
		a.run();
		assertEquals("The behaviour was called once", beh.callCount, 1);
		assertEquals("The executor was not called", ex.callCount, 0);
		assertTrue("The Actor killed itself", a.killed);
		assertNull("The Actor killed itself with null", a.reason);
	}
	
	@Test public void testActorDiesWhenTheBehaviourThrows() {
		final Exception reason = new Exception() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			
		};
		final Behaviour beh = new Behaviour() {

			@Override
			public Behaviour run(Actor self, Object message) throws Throwable {
				// TODO Auto-generated method stub
				throw reason;
			}
			
		};
		
		final ExecutorStub ex = new ExecutorStub();
		final ArrayBlockingQueue<Object> ms = new ArrayBlockingQueue<>(10);
		Object message = new Object();
		ms.add(message);
		
		ActorWithStubbedDie a = new ActorWithStubbedDie(beh, ex, null, null, ms, null, false);
		a.run();
		assertEquals("The executor was not called", ex.callCount, 0);
		assertTrue("The Actor killed itself", a.killed);
		assertEquals("The Actor killed itself with the thrown exception", a.reason, reason);
	}
	
	@Test public void testActorDoesNotRunAfterKilled() {
		BehaviourStub beh = new BehaviourStub();
		ArrayBlockingQueue<Object> ms = new ArrayBlockingQueue<>(10);
		ms.add(new Object());
		Actor a = new Actor(beh, null, /* observers */ null, /* links */ null, 
				ms, /* generator */ null, /* isSystem */ false); 
		a.die(null);
		a.run();
		assertEquals(beh.callCount, 0);
	}
	
	@Test public void testActorSilentlyAcceptsMessagesAfterKilled() {
		BehaviourStub beh = new BehaviourStub();
		ArrayBlockingQueue<Object> ms = new ArrayBlockingQueue<>(10);
		
		Actor a = new Actor(beh, null, /* observers */ null, /* links */ null, 
				ms, /* generator */ null, /* isSystem */ false); 
		a.die(null);
		a.send(new Object());
		assertEquals(beh.callCount, 0);		
	}
	
	@Test public void testActorNotifiesObserversWhenKilled() {
		BehaviourStub beh = new BehaviourStub();
		ExecutorStub ex = new ExecutorStub();
		ArrayBlockingQueue<Object> ms = new ArrayBlockingQueue<>(10);
		Actor watcher = new Actor(beh, ex, null, null, ms, null, false);
		HashMap<Integer, Actor> observers = new HashMap<>();
		Actor toDie = new Actor(null, null, observers, null, null, 
				new DeterministicIDGenerator(), false);
		Throwable reason = new NullPointerException("Foo");
		
		int observationID = toDie.addObserver(watcher);
		toDie.die(reason);
		
		assertEquals("The behaviour was called once", beh.messages.size(), 1);
		assertEquals("The behaviour was called with an ObservedDied message", 
				beh.messages.get(0), new ObservedDied(observationID, reason));
	}
	
	@Test public void testActorNotifiesObserversOnce() {
		BehaviourStub beh = new BehaviourStub();
		ExecutorStub ex = new ExecutorStub();
		ArrayBlockingQueue<Object> ms = new ArrayBlockingQueue<>(10);
		Actor watcher = new Actor(beh, ex, null, null, ms, null, false);
		HashMap<Integer, Actor> observers = new HashMap<>();
		Actor toDie = new Actor(null, null, observers, null, null, 
				new DeterministicIDGenerator(), false);
		Throwable reason = new NullPointerException("Foo");
		
		int observationID = toDie.addObserver(watcher);
		toDie.die(reason);
		toDie.die(reason);
		
		assertEquals("The behaviour was called once", beh.messages.size(), 1);
		assertEquals("The behaviour was called with an ObservedDied message", 
				beh.messages.get(0), new ObservedDied(observationID, reason));
	}	
	
	@Test public void testCanDoubleObservations() {
		BehaviourStub beh = new BehaviourStub();
		ExecutorStub ex = new ExecutorStub();
		DeterministicIDGenerator idg = new DeterministicIDGenerator();
		ArrayBlockingQueue<Object> ms = new ArrayBlockingQueue<>(10);
		Actor watcher = new Actor(beh, ex, null, null, ms, null, false);
		HashMap<Integer, Actor> observers = new HashMap<>();
		Actor toDie = new Actor(null, null, observers, null, null, 
				idg, false);
		Throwable reason = new NullPointerException("Foo");
		
		idg.nextID = 0;
		int observationID1 = toDie.addObserver(watcher);
		idg.nextID = 1;
		int observationID2 = toDie.addObserver(watcher);
		toDie.die(reason);
		
		assertEquals("The behaviour was called twice", beh.messages.size(), 2);
		assertTrue("The behaviour was called with an ObservedDied message", 
				beh.messages.contains(new ObservedDied(observationID1, reason)));
		assertTrue("The behaviour was called with an ObservedDied message", 
				beh.messages.contains(new ObservedDied(observationID2, reason)));
	}	
	
	@Test public void testCanHaveMultipleObservers() {
		BehaviourStub beh1 = new BehaviourStub();
		ArrayBlockingQueue<Object> ms1 = new ArrayBlockingQueue<>(10);
		BehaviourStub beh2 = new BehaviourStub();
		ArrayBlockingQueue<Object> ms2 = new ArrayBlockingQueue<>(10);
		ExecutorStub ex = new ExecutorStub();

		DeterministicIDGenerator idg = new DeterministicIDGenerator();

		Actor w1 = new Actor(beh1, ex, null, null, ms1, null, false);
		Actor w2 = new Actor(beh2, ex, null, null, ms2, null, false);
		
		HashMap<Integer, Actor> observers = new HashMap<>();
		Actor toDie = new Actor(null, null, observers, null, null, 
				idg, false);
		Throwable reason = new NullPointerException("Foo");
		
		idg.nextID = 0;
		int observationID1 = toDie.addObserver(w1);
		idg.nextID = 1;
		int observationID2 = toDie.addObserver(w2);
		toDie.die(reason);
		
		assertEquals("The first watcher was called once", beh1.messages.size(), 1);
		assertEquals("The first was called with the correct ObservedDied message", 
				beh1.messages.get(0), new ObservedDied(observationID1, reason));
		assertEquals("The second watcher was called once", beh2.messages.size(), 1);
		assertEquals("The second was called with the correct ObservedDied message", 
				beh2.messages.get(0), new ObservedDied(observationID2, reason));
	}
	
	@Test public void testRemoveObservers() {
		BehaviourStub beh1 = new BehaviourStub();
		ArrayBlockingQueue<Object> ms1 = new ArrayBlockingQueue<>(10);
		BehaviourStub beh2 = new BehaviourStub();
		ArrayBlockingQueue<Object> ms2 = new ArrayBlockingQueue<>(10);
		ExecutorStub ex = new ExecutorStub();

		DeterministicIDGenerator idg = new DeterministicIDGenerator();

		Actor w1 = new Actor(beh1, ex, null, null, ms1, null, false);
		Actor w2 = new Actor(beh2, ex, null, null, ms2, null, false);
		
		HashMap<Integer, Actor> observers = new HashMap<>();
		Actor toDie = new Actor(null, null, observers, null, null, 
				idg, false);
		Throwable reason = new NullPointerException("Foo");
		
		idg.nextID = 0;
		int observationID1 = toDie.addObserver(w1);
		idg.nextID = 1;
		int observationID2 = toDie.addObserver(w2);
		toDie.removeObserver(observationID2);
		toDie.die(reason);
		
		assertEquals("The first watcher was called once", beh1.messages.size(), 1);
		assertEquals("The first was called with the correct ObservedDied message", 
				beh1.messages.get(0), new ObservedDied(observationID1, reason));
		assertEquals("The second watcher was not called", beh2.messages.size(), 0);
	}
	
	@Test public void testLinksDie() {
		HashSet<Actor> l1 = new HashSet<>();
		ActorWithStubbedDie stub = new ActorWithStubbedDie(null, null, null, l1, null, null, false);
		HashSet<Actor> l2 = new HashSet<>();
		Actor toDie = new Actor(null, null, null, l2, null, null, false);
		Throwable reason = new NullPointerException("Foo");

		toDie.link(stub);
		toDie.die(reason);

		assertTrue("The linked actor was killed.", stub.killed);
		assertEquals("The linked actor was killed with a LinkedActorDied", 
				stub.reason, new LinkedActorDied(toDie, reason));
	}	
	
	@Test public void testSystemActorsDontDieOnLinkDeaths() {
		BehaviourStub beh = new BehaviourStub();
		ExecutorStub ex = new ExecutorStub();
		ArrayBlockingQueue<Object> ms = new ArrayBlockingQueue<>(10);
		Throwable reason = new NullPointerException("foo");
		LinkedActorDied deathMessage = new LinkedActorDied(null, reason);
		Actor a = new Actor(beh, ex, null, null, ms, null, true);
		Object nextMessage = new Object();
		
		a.die(deathMessage);
		a.send(nextMessage);
		
		assertEquals("The actor kept going when killed with a LinkedActorDied",
				beh.messages.size(), 2);
		assertEquals("The actor executed the LinkedActorDied message",
				beh.messages.get(0), deathMessage);
		assertEquals("The actor executed the subsequent message",
				beh.messages.get(1), nextMessage);
	}
	
	@Test public void testSystemActorsRemoveLinksToDeadActors() {
		BehaviourStub beh = new BehaviourStub();
		ExecutorStub ex = new ExecutorStub();
		ArrayBlockingQueue<Object> ms = new ArrayBlockingQueue<>(10);
		HashSet<Actor> links = new HashSet<>();
		Throwable reason = new NullPointerException("foo");

		Actor subject = new Actor(beh, ex, null, links, ms, null, true);
		Actor linked = new Actor(null, null, null, null, null, null, false);
		links.add(linked);

		LinkedActorDied deathMessage = new LinkedActorDied(linked, reason);
		
		assertTrue("The subject is linked to the actor", links.contains(linked));
		subject.die(deathMessage);		
		assertFalse("The subject has removed the linked actor", links.contains(linked));
	}	
	
	@Test public void testUnlinking() {
		HashSet<Actor> l1 = new HashSet<>();
		ActorWithStubbedDie stub = new ActorWithStubbedDie(null, null, null, l1, null, null, false);
		HashSet<Actor> l2 = new HashSet<>();
		Actor toDie = new Actor(null, null, null, l2, null, null, false);
		Throwable reason = new NullPointerException("Foo");

		toDie.link(stub);
		toDie.unlink(stub);
		toDie.die(reason);

		assertFalse("The stub was unaffected by toDie's death.", stub.killed);		
	}
	
	@Test public void testKillingInterruptsABehaviour() {
		Behaviour waitBehaviour = new Behaviour() {

			@Override
			public Behaviour run(Actor self, Object message) throws Throwable {
				Thread.sleep(1000);
				return this;
			}
		};
		ThreadExecutor threadExecutor = new ThreadExecutor();
		ArrayBlockingQueue<Object> ms1 = new ArrayBlockingQueue<>(10);
		HashMap<Integer, Actor> observers = new HashMap<>();
		DeterministicIDGenerator idg = new DeterministicIDGenerator();
		Actor toDie = new Actor(waitBehaviour, threadExecutor, observers, null, ms1, idg, false);
		
		BehaviourStub obs = new BehaviourStub();
		ExecutorStub ex = new ExecutorStub();
		ArrayBlockingQueue<Object> ms2 = new ArrayBlockingQueue<>(10);
		Actor observer = new Actor(obs, ex, null, null, ms2, null, false);
		
		Throwable reason = new NullPointerException("A reason");
		
		int obsID = toDie.addObserver(observer);
		toDie.send(new Object());
		long startTime = System.currentTimeMillis();

		try {
			Thread.sleep(10);
			toDie.die(reason);
		} catch (InterruptedException e) {
			fail("The thread was interrupted on sleep");
		}
		
		try {
			threadExecutor.myThread.join();
		} catch (InterruptedException e) {
			fail("The thread was interrupted on join");
		}
		long endTime = System.currentTimeMillis();
		
		assertTrue("The killed actor was interrupted", endTime - startTime < 100);
		assertEquals("The observer was called once", obs.callCount, 1);
		assertEquals("The observer was called with the correct kill message",
				new ObservedDied(obsID, reason), obs.messages.get(0));
	}
	
	@Test public void testTakeNextMessageBlocks() {
		class NeedsTwoMessagesBehaviour implements Behaviour {
			private Object firstMessage = null;
			private Object secondMessage = null;

			@Override
			public Behaviour run(Actor self, Object message) throws Throwable {
				firstMessage = message;
				
				secondMessage = self.takeNextMessage();
				return null;
			}
			
		}
		NeedsTwoMessagesBehaviour blockingBehaviour = new NeedsTwoMessagesBehaviour();
		ThreadExecutor threadExecutor = new ThreadExecutor();
		ArrayBlockingQueue<Object> ms1 = new ArrayBlockingQueue<>(10);
		Actor a = new Actor(blockingBehaviour, threadExecutor, null, null, ms1, null, false);

		String firstMessage = "First message";
		String secondMessage = "Second Message";
		a.send(firstMessage);
		try {
			Thread.sleep(10);
			a.send(secondMessage);
		} catch (InterruptedException e) {
			fail("The thread was interrupted on sleep");
		}
				
		assertEquals("The behaviour obtained the first message", firstMessage, blockingBehaviour.firstMessage);
		assertEquals("The behaviour obtained the second message", secondMessage, blockingBehaviour.secondMessage);
		assertFalse("The behaviour ran to completion, killing the actor", a.isAlive());
	}	
	
	class BehaviourStub implements Behaviour {
		public List<Object> messages = new ArrayList<>();
		public Actor self = null;
		public int callCount = 0;
		public Behaviour nextBehaviour;
		
		BehaviourStub() {
			nextBehaviour = this;
		}
		
		BehaviourStub(Behaviour b) {
			nextBehaviour = b;
		}
		
		@Override
		public Behaviour run(Actor self, Object message) throws Throwable {
			this.self = self;
			this.messages.add(message);
			callCount += 1;
			return nextBehaviour;
		}
		
	}
	
	class ExecutorStub implements Executor {
		public Runnable calledOn = null;
		public int callCount = 0;
		
		public void execute(Runnable r) {
			calledOn = r;
			callCount += 1;
			r.run();
		}
	}
	
	class ActorWithStubbedDie extends Actor {
		public boolean killed = false;
		public Throwable reason = null;
		
		ActorWithStubbedDie(Behaviour beh, Executor ex, Map<Integer, Actor> os,  Set<Actor> ls, 
				BlockingQueue<Object> ms, IDGenerator idg, boolean sys) {
			super(beh, ex, os, ls, 
					ms, idg, sys); 
		}
		
		@Override
		public void die(Throwable reason) {
			killed = true;
			this.reason = reason;
		}
	}
	
	class ThreadExecutor implements Executor {
		public Thread myThread;

		@Override
		public void execute(Runnable command) {
			myThread = new Thread(command);
			myThread.start();
		}
	}
	
	class DeterministicIDGenerator implements IDGenerator {
		public int nextID = 0;
		@Override
		public int generateID() {
			return nextID;
		}
		
	}
}
