package de.htw.ds.sync;

import de.sb.java.Reference;
import de.sb.java.TypeMetadata;


/**
 * Demonstrates thread processing and thread re-synchronization using a runnable in conjunction with
 * (out-dated) monitor signaling. Note the need to take care of {@linkplain InterruptedException}.
 * Also note that any Object can be used as a synchronization monitor between threads!<br />
 * Especially note the problem arising from monitors not behaving symmetrically: If one of the child
 * threads calls {@linkplain #notify()} of it's monitor before the parent thread can send the
 * corresponding {@linkplain #wait()} message, the parent thread will deadlock. This can already
 * happen while resynchronizing a single thread, but is almost guaranteed to happen with more than
 * one.<br />
 * Monitors are extremely efficient for synchronization purposes. However, they're pretty cumbersome
 * and error-prone for re-synchronization purposes, and therefore primarily used to implement other
 * types of re-synchronization mechanisms, like semaphores. Finally, note that this class is
 * declared final because it provides an application entry point, and therefore not supposed to be
 * extended.
 */
@TypeMetadata(copyright = "2008-2015 Sascha Baumeister, all rights reserved", version = "0.3.0", authors = "Sascha Baumeister")
public final class ResyncThreadByMonitor {

	/**
	 * Application entry point. The arguments must be a child thread count, and the maximum number
	 * of seconds the child threads should take for processing.
	 * @param args the arguments
	 * @throws IndexOutOfBoundsException if less than two arguments are passed
	 * @throws NumberFormatException if any of the given arguments is not an integral number
	 * @throws IllegalArgumentException if any of the arguments is negative
	 * @throws RuntimeException if there is a runtime exception during asynchronous work
	 * @throws ExampleWorkerException if there is a checked exception during asynchronous work
	 */
	static public void main (final String[] args) throws ExampleWorkerException {
		final int childThreadCount = Integer.parseInt(args[0]);
		final int maximumWorkDuration = Integer.parseInt(args[1]);
		resync(childThreadCount, maximumWorkDuration);
	}


	/**
	 * Starts child threads and resynchronizes them, displaying the time it took for the longest
	 * running child to end.
	 * @param childThreadCount the number of child threads
	 * @param maximumWorkDuration the maximum work duration is seconds
	 * @throws IllegalArgumentException if any of the given arguments is negative
	 * @throws Error if there is an error during asynchronous work
	 * @throws RuntimeException if there is a runtime exception during asynchronous work
	 * @throws ExampleWorkerException if there is a checked exception during asynchronous work
	 * @throws IllegalArgumentException if any of the arguments is negative
	 */
	static private void resync (final int childThreadCount, final int maximumWorkDuration) throws ExampleWorkerException {
		if (childThreadCount < 0 | maximumWorkDuration < 0) throw new IllegalArgumentException();
		final long timestamp = System.currentTimeMillis();

		System.out.format("Starting %s Java thread(s)...\n", childThreadCount);
		final Object monitor = new Object();
		final Reference<Throwable> exceptionReference = new Reference<>();

		// It is essential that synchronization of the monitor starts here, and extends to AFTER
		// resynchronization has ended. Note that this thread will implicitly give up the synch-lock
		// while it is blocking during wait(), and automatically re-acquire it afterwards! This
		// solves most of the dead-lock Problems associated with monitors, except one:
		//   The second of two quasi-simultaneous occurring monitor.signal() calls may be lost whenever
		// it's thread is faster in acquiring the monitor's sync-lock than this thread is in re-acquiring
		// it after unblocking to perform another monitor.wait(); one signal effectively "fizzles", and
		// therefore the last wait() will dead-lock.
		synchronized (monitor) {
			for (int index = 0; index < childThreadCount; ++index) {
				final Runnable runnable = new Runnable() {
					public void run () {
						try {
							ExampleWorker.work(maximumWorkDuration);
						} catch (final Throwable exception) {
							exceptionReference.put(exception);
						} finally {
							synchronized (monitor) {
								monitor.notify();
							}
						}
					}
				};

				new Thread(runnable).start();
			}

			System.out.println("Resynchronising Java thread(s)... ");
			// CHOOSING to abort resynchronization, and as a natural consequence of a
			// broken before-after relationship terminate this thread - see class comment!
			// Note that interrupting a resynchronization is always a delicate affair, as the remainder
			// of the interrupted thread implicitly depends on the resynchronization to have taken place!
			for (int index = 0; index < childThreadCount; ++index) {
				try {
					monitor.wait();
				} catch (final InterruptedException interrupt) {
					throw new ThreadDeath();
				}
			}
		}

		final Throwable exception = exceptionReference.get();
		if (exception instanceof Error) throw (Error) exception;
		if (exception instanceof RuntimeException) throw (RuntimeException) exception;
		if (exception instanceof ExampleWorkerException) throw (ExampleWorkerException) exception;
		assert exception == null;

		System.out.format("Java thread(s) resynchronized after %sms.\n", System.currentTimeMillis() - timestamp);
	}
}