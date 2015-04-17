package de.htw.ds.sync;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import de.sb.java.Reference;
import de.sb.java.TypeMetadata;


/**
 * Demonstrates thread processing and thread re-synchronization using a runnable in conjunction with
 * polling, and a situationally correct way of handling {@linkplain InterruptedException}. Note that
 * there is no "golden" way to handle thread interruption which is always correct, i.e. the handling
 * must always take circumstances into account! However, an interruption MUST always be handled
 * immediately, in one or more of the following ways:
 * <ul>
 * <li>Ignore or just log it: Good choice if the interrupted thread or it's process will end soon
 * anyways, OR if the interrupted thread is a service/monitor thread that other threads depend on,
 * and which must therefore not be stopped before it's natural end.</li>
 * <li>Re-interrupt the interrupted thread or it's parent at a later time using
 * {@linkplain Thread#interrupt()}: Suitable only if the interruption doesn't reappear immediately
 * (interruptible loops), AND if interruptible operations follow later (otherwise the thread will
 * not be interrupted again before it's natural end).</li>
 * <li>Terminate the interrupted process using {@linkplain System#exit(int)}: Possible if process
 * termination is safe with regard to other processes, AND such behavior would be acceptable to
 * process users.</li>
 * <li>Terminate the interrupted thread by throwing {@linkplain ThreadDeath}: Safe if it is
 * guaranteed that no method caller catches {@linkplain ThreadDeath} without rethrowing it (not
 * recommended but can happen), thereby preventing thread termination, AND if thread termination is
 * safe with regard to open resources, AND if thread termination is safe with regard to other
 * threads, i.e. other threads must not depend on the interrupted thread running until it's natural
 * end in this case.</li>
 * <li>Simply declare {@linkplain InterruptedException} in the methods throws clause: ONLY allowed
 * if the method provides system level functionality, or is the {@code main(String[])} method of a
 * process whose other threads don't depend on the main thread to end naturally! The reason is that
 * higher call stacks have less and less chance to determine a suitable way of handling this
 * exception type, which in effect would cause the exception declaration to spread like cancer
 * throughout APIs without any benefit! If you want to terminate the thread or process, throw
 * {@linkplain ThreadDeath} or use {@linkplain System#exit(int)} instead.</li>
 * </ul>
 * Note that the main reason {@linkplain InterruptedException} was introduced into Java was to force
 * the immediate (sic!) caller of a blocking method to CHOOSE one of the possibilities described
 * above. Before it's introduction, system level methods threw {@linkplain ThreadDeath}, which did
 * not force programmers to THINK first about the implications of premature thread termination, and
 * this lack of thought subsequently caused a lot of problems.<br />
 * Also note the need of a short sleep phase during polling in order to avoid the CPU-core to go to
 * 100% workload just for cycling the polls, and it's implications for latency! Finally, note that
 * this class is declared final because it provides an application entry point, and therefore not
 * supposed to be extended.
 */
@TypeMetadata(copyright = "2008-2015 Sascha Baumeister, all rights reserved", version = "0.3.0", authors = "Sascha Baumeister")
public final class ResyncThreadByPolling {

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
		final Set<Thread> threads = new HashSet<>();
		final Reference<Throwable> exceptionReference = new Reference<>();

		for (int index = 0; index < childThreadCount; ++index) {
			final Runnable runnable = new Runnable() {
				public void run () {
					try {
						ExampleWorker.work(maximumWorkDuration);
					} catch (final Throwable exception) {
						exceptionReference.put(exception);
					}
				}
			};

			final Thread thread = new Thread(runnable);
			threads.add(thread);
			thread.start();
		}

		System.out.println("Resynchronising Java thread(s)... ");
		while (!threads.isEmpty()) {
			final Iterator<Thread> iterator = threads.iterator();
			while (iterator.hasNext()) {
				if (!iterator.next().isAlive()) iterator.remove();
			}

			// MUST try to sleep at least a bit to prevent CPU from running at 100% while
			// polling. Even with a short 1ms nap, the thread has a good chance to
			// check the child thread status with pretty much every time slice
			// it get's from the systems task scheduler, while still not starving
			// the system's other tasks for CPU time, apart from needlessly heating
			// up the CPU!
			try { Thread.sleep(1); } catch (final InterruptedException interrupt) {}
		}

		final Throwable exception = exceptionReference.get();
		if (exception instanceof Error) throw (Error) exception;
		if (exception instanceof RuntimeException) throw (RuntimeException) exception;
		if (exception instanceof ExampleWorkerException) throw (ExampleWorkerException) exception;
		assert exception == null;

		System.out.format("Java thread(s) resynchronized after %sms.\n", System.currentTimeMillis() - timestamp);
	}
}