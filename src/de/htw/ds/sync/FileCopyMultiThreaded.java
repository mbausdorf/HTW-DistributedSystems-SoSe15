package de.htw.ds.sync;

import de.sb.java.TypeMetadata;

import java.io.*;
import java.nio.channels.Pipe;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * Demonstrates copying a file using a single thread. Note that this class is declared final because
 * it provides an application entry point, and therefore not supposed to be extended.
 */
@TypeMetadata(copyright = "2008-2015 Sascha Baumeister, all rights reserved", version = "0.3.0", authors = "Sascha Baumeister")
public final class FileCopyMultiThreaded {

	/**
	 * Copies a file. The first argument is expected to be a qualified source file name, the second
	 * a qualified target file name.
	 * @param args the VM arguments
	 * @throws IOException if there's an I/O related problem
	 */
	static public void main (final String[] args) throws IOException {
		final Path sourcePath = Paths.get(args[0]);
		if (!Files.isReadable(sourcePath)) throw new IllegalArgumentException(sourcePath.toString());

		final Path sinkPath = Paths.get(args[1]);
		if (sinkPath.getParent() != null && !Files.isDirectory(sinkPath.getParent())) throw new IllegalArgumentException(sinkPath.toString());

		// Files.copy(sourcePath, sinkPath, StandardCopyOption.REPLACE_EXISTING);
		
		try(PipedInputStream pInputStream = new PipedInputStream(4*1024))
		{
			try(PipedOutputStream pOutputStream = new PipedOutputStream(pInputStream))
			{
				Thread firstThread = new Thread(new Transporter(Files.newInputStream(sourcePath), pOutputStream));
				Thread secondThread = new Thread(new Transporter(pInputStream, Files.newOutputStream(sinkPath)));
				
				firstThread.start();
				secondThread.start();
			}
		}
		
		System.out.println("done.");
	}

	private static class Transporter implements Runnable{
		InputStream inputStream;
		OutputStream outputStream;

		public Transporter(InputStream is, OutputStream os)
		{
			this.inputStream = is;
			this.outputStream = os;
		}

		public void run()
		{
			try{
				final byte[] buffer = new byte[0x10000];
				for (int bytesRead = this.inputStream.read(buffer); bytesRead != -1; bytesRead = this.inputStream.read(buffer)) {
					this.outputStream.write(buffer, 0, bytesRead);
				}
			} catch (IOException e)
			{
				//bad luck
			}
			
		}
	}
}