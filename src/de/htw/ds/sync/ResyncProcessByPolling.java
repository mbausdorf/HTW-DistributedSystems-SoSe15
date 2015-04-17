package de.htw.ds.sync;

import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.Callable;
import de.sb.java.Threads;
import de.sb.java.TypeMetadata;
import de.sb.java.io.Streams;


/**
 * Demonstrates child process initiation and resynchronization inside the parent process, using the
 * polling method. Additionally demonstrates process I/O redirection, and a situationally correct
 * way of handling {@linkplain InterruptedException}. Note that there is no "golden" way to handle
 * thread interruption which is always correct, i.e. the handling must always take circumstances
 * into account! However, an interruption MUST always be handled immediately, in one or more of the
 * following ways:
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
 * this lack of thought subsequently caused a lot of problems. Finally, note that this class is
 * declared final because it provides an application entry point, and therefore not supposed to be
 * extended.
 */
@TypeMetadata(copyright = "2008-2015 Sascha Baumeister, all rights reserved", version = "0.3.0", authors = "Sascha Baumeister")
public final class ResyncProcessByPolling {

	/**
	 * Application entry point. The single parameters must be a command line suitable to start a
	 * program/process.
	 * @param args the arguments
	 * @throws IndexOutOfBoundsException if no argument is passed
	 * @throws IOException if there's an I/O related problem
	 */
	static public void main (final String[] args) throws IOException {
		System.out.println("Starting process... ");
		final Process process = Runtime.getRuntime().exec(args[0]);

		System.out.println("Connecting process I/O streams with current Java process... ");
		final Callable<?> systemInputTransporter = () -> Streams.copy(System.in, new PrintStream(process.getOutputStream()), 0x10);
		final Callable<?> systemOutputTransporter = () -> Streams.copy(process.getInputStream(), System.out, 0x10);
		final Callable<?> systemErrorTransporter = () -> Streams.copy(process.getErrorStream(), System.err, 0x10);

		// System.in transporter must be started as a daemon thread, otherwise read-block prevents termination!
		final Thread systemInputThread = new Thread(Threads.newRunnable(systemInputTransporter));
		systemInputThread.setDaemon(true);
		systemInputThread.start();
		new Thread(Threads.newRunnable(systemOutputTransporter)).start();
		new Thread(Threads.newRunnable(systemErrorTransporter)).start();

		System.out.println("Resynchronising process... ");
		final long timestamp = System.currentTimeMillis();

		int exitCode;
		while (true) {
			try {
				exitCode = process.exitValue();
				break;
			} catch (final IllegalThreadStateException exception) {
				// MUST try to sleep at least a bit to prevent CPU from running at 100% while
				// polling. Even with a short 1ms nap, the thread has a good chance to check
				// the child process's status with pretty much every time slice it get's from
				// the systems task scheduler, without starving the system's other tasks for
				// CPU time and needlessly heating up the CPU!
				try { Thread.sleep(1); } catch (final InterruptedException nestedException) {}
			}
		}

		System.out.format("Process ended with exit code %s after running %sms.\n", exitCode, System.currentTimeMillis() - timestamp);
	}
}