package de.htw.ds.sync;

import de.sb.java.TypeMetadata;

/**
 * Thread demo using explicit runnable class.
 */
@TypeMetadata(copyright = "2013-2015 Sascha Baumeister, all rights reserved", version = "0.3.0", authors = "Sascha Baumeister")
public class ThreadDemo1 {

	/**
	 * Application entry point. The runtime arguments must at least
	 * consist of a display text.
	 * @param args the runtime arguments
	 */
	static public void main (final String[] args) {
		final String text = args[0];

		final Runnable runnable1 = new DemoRunnable1(text);

		final Thread thread = new Thread(runnable1);
		thread.start();		// executes runnable asynchronously
		// thread.run();	// executes runnable synchronously!
	}
 }