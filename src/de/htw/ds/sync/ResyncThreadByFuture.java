package de.htw.ds.sync;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import de.sb.java.TypeMetadata;


/**
 * Demonstrates thread processing and thread resynchronization using a future, and a situationally
 * correct way of handling {@linkplain InterruptedException}. Note that there is no "golden" way to
 * handle thread interruption which is always correct, i.e. the handling must always take
 * circumstances into account! However, an interruption MUST always be handled immediately, in one
 * or more of the following ways:
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
 * Also note that futures do not need any references to communicate a result back to the originator
 * thread. Finally, note the ability of futures to return any kind of exception to be originator
 * thread by catching and analyzing {@linkplain ExecutionException}. Finally, note that this class
 * is declared final because it provides an application entry point, and therefore not supposed to
 * be extended.
 */
@TypeMetadata(copyright = "2008-2015 Sascha Baumeister, all rights reserved", version = "0.3.0", authors = "Sascha Baumeister")
public final class ResyncThreadByFuture {
	@SuppressWarnings("all")
	static private final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());


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
		final Set<Future<Integer>> futures = new HashSet<>();

		for (int index = 0; index < childThreadCount; ++index) {
			final Callable<Integer> callable = new Callable<Integer>() {
				public Integer call () throws ExampleWorkerException {
					return ExampleWorker.work(maximumWorkDuration);
				}
			};

			// final Future<Integer> future = EXECUTOR_SERVICE.submit(callable);
			final RunnableFuture<Integer> future = new FutureTask<>(callable);
			new Thread(future).start();

			futures.add(future);
		}

		System.out.println("Resynchronising Java thread(s)... ");
		for (final Future<Integer> future : futures) {
			@SuppressWarnings("unused")
			Integer lastResult = null;
			try {
				final int duration = getUninterruptibly(future);
				System.out.format("%s worked %d ms.\n", future, duration);
			} catch (final ExecutionException exception) {
				final Throwable cause = exception.getCause();
				if (cause instanceof Error) throw (Error) cause;
				if (cause instanceof RuntimeException) throw (RuntimeException) cause;
				if (cause instanceof ExampleWorkerException) throw (ExampleWorkerException) cause;
				throw new AssertionError();
			}
		}
		System.out.format("Java thread(s) resynchronized after %sms.\n", System.currentTimeMillis() - timestamp);
	}


	/**
	* CHOOSING to silently ignore thread interruption - see class comment!
	* Note that interrupting a resynchronization is always a delicate affair, as the remainder
	* of the interrupted thread implicitly depends on the resynchronization to have taken place!
	* @param future the future to resynchronize into the current thread
	* @return the future's processing result
	* @throws ExecutionException if the future's callable threw an exception
	*/
	static private <T> T getUninterruptibly (final Future<T> future) throws ExecutionException {
		while (true) {
			try { return future.get(); } catch (final InterruptedException interrupt) {}
		}
	}
}