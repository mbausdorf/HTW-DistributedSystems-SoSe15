package de.htw.ds.tcp;

/**
 * TCP monitor watchers are registered with TCP monitors to act whenever communication data becomes
 * available, or a communication transmission causes exceptions. This callback design allows
 * different kinds of applications to profit from a single monitor implementation.
 */
public interface TcpMonitorWatcher {

	/**
	 * Called whenever a TCP monitor catches an exception while communication transmissions take
	 * place.
	 * @param exception the exception
	 */
	void exceptionCatched (final Exception exception);


	/**
	 * Called whenever a TCP monitor notices that two corresponding connections have finished
	 * exchanging data. The given record contains all the data exchanged, plus the open and close
	 * timestamps of the data exchange.
	 * @param record the monitor record
	 */
	void recordCreated (final TcpMonitorRecord record);
}