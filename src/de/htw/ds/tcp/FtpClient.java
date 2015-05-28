package de.htw.ds.tcp;

import de.sb.java.TypeMetadata;
import de.sb.java.net.SocketAddress;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;


/**
 * This class implements a simple FTP client. It demonstrates the use of TCP connections, and the
 * Java Logging API. Note that this class is declared final because it provides an application entry
 * point, and therefore not supposed to be extended.
 */
@TypeMetadata(copyright = "2011-2015 Sascha Baumeister, all rights reserved", version = "0.3.0", authors = "Sascha Baumeister")
public final class FtpClient implements AutoCloseable {
    static private final Charset ASCII = Charset.forName("US-ASCII");

    /**
     * Application entry point. The given runtime parameters must be a server address, an alias, a
     * password, a boolean indicating binary or ASCII transfer mode, STORE or RETRIEVE transfer
     * direction, a source file path, and a target directory path.
     *
     * @param args the given runtime arguments
     * @throws IOException if the given port is already in use
     */
    static public void main(final String[] args) throws IOException {
        LogManager.getLogManager();

        final InetSocketAddress serverAddress = new SocketAddress(args[0]).toInetSocketAddress();
        final String alias = args[1];
        final String password = args[2];
        final boolean binaryMode = Boolean.parseBoolean(args[3]);
        final String transferDirection = args[4];
        final Path sourcePath = Paths.get(args[5]).normalize();
        final Path targetPath = Paths.get(args[6]).normalize();

        try (FtpClient client = new FtpClient(serverAddress)) {
            client.open(alias, password, binaryMode);

            if (transferDirection.equals("STORE")) {
                client.sendFile(sourcePath, targetPath);
            } else if (transferDirection.equals("RETRIEVE")) {
                client.receiveFile(sourcePath, targetPath);
            } else {
                throw new IllegalArgumentException(transferDirection);
            }
        }
    }

    private final InetSocketAddress serverAddress;
    private volatile Socket controlConnection;
    private volatile BufferedWriter controlConnectionSink;


    private volatile BufferedReader controlConnectionSource;


    /**
     * Creates a new instance able to connect to the given FTP server address.
     *
     * @param serverAddress the TCP socket-address of an FTP server
     * @throws IOException if there is an I/O related problem
     */
    public FtpClient(final InetSocketAddress serverAddress) throws IOException {
        if (serverAddress == null) throw new NullPointerException();

        this.serverAddress = serverAddress;
    }


    /**
     * Closes an FTP control connection.
     *
     * @throws IOException if there is an I/O related problem
     */
    public synchronized void close() throws IOException {
        if (this.isClosed()) return;

        try {
            final FtpResponse response = this.sendRequest("QUIT");
            if (response.getCode() != 221) throw new ProtocolException(response.toString());
        } finally {
            try {
                this.controlConnection.close();
            } catch (final IOException exception) {
            }

            this.controlConnection = null;
            this.controlConnectionSink = null;
            this.controlConnectionSource = null;
        }
    }


    /**
     * Returns the server address used for TCP control connections.
     *
     * @return the server address
     */
    public InetSocketAddress getServerAddress() {
        return this.serverAddress;
    }


    /**
     * Returns whether or not this client is closed.
     *
     * @return {@code true} if this client is closed, {@code false} otherwise
     */
    public boolean isClosed() {
        return this.controlConnection == null;
    }


    /**
     * Opens the FTP control connection.
     *
     * @param alias      the user-ID
     * @param password   the password
     * @param binaryMode true for binary transmission, false for ASCII
     * @throws IllegalStateException if this client is already open
     * @throws SecurityException     if the given alias or password is invalid
     * @throws IOException           if there is an I/O related problem
     */
    public synchronized void open(final String alias, final String password, final boolean binaryMode) throws IOException {
        if (!this.isClosed()) throw new IllegalStateException();

        try {
            this.controlConnection = new Socket(this.serverAddress.getHostName(), this.serverAddress.getPort());
            this.controlConnectionSink = new BufferedWriter(new OutputStreamWriter(this.controlConnection.getOutputStream(), ASCII));
            this.controlConnectionSource = new BufferedReader(new InputStreamReader(this.controlConnection.getInputStream(), ASCII));

            FtpResponse response = this.receiveResponse();
            if (response.getCode() != 220) throw new ProtocolException(response.toString());

            response = this.sendRequest("USER " + (alias == null ? "guest" : alias));
            if (response.getCode() == 331) {
                response = this.sendRequest("PASS " + (password == null ? "" : password));
            }
            if (response.getCode() != 230) throw new SecurityException(response.toString());

            response = this.sendRequest("TYPE " + (binaryMode ? "I" : "A"));
            if (response.getCode() != 200) throw new ProtocolException(response.toString());
        } catch (final Exception exception) {
            try {
                this.close();
            } catch (final Exception nestedException) {
                exception.addSuppressed(nestedException);
            }
            throw exception;
        }
    }

    //==================================================================================================================
    // THIS IS WHERE OUR CODE STARTS
    //==================================================================================================================

    /**
     * Stores the given file on the FTP client side using a separate data connection. Note that the
     * source file resides on the server side and must therefore be a relative path (relative to the
     * FTP server context directory), while the target directory resides on the client side and can
     * be a global path.
     *
     * @param sourceFile    the source file (server side)
     * @param sinkDirectory the sink directory (client side)
     * @throws NullPointerException  if the target directory is {@code null}
     * @throws IllegalStateException if this client is closed
     * @throws NotDirectoryException if the source or target directory does not exist
     * @throws NoSuchFileException   if the source file does not exist
     * @throws AccessDeniedException if the source file cannot be read, or the sink directory cannot
     *                               be written
     * @throws IOException           if there is an I/O related problem
     */
    public synchronized void receiveFile(final Path sourceFile, final Path sinkDirectory) throws IOException {
        if (this.isClosed()) throw new IllegalStateException();
        if (!Files.isDirectory(sinkDirectory)) throw new NotDirectoryException(sinkDirectory.toString());

        try {
            // get data connection socket
            try (Socket newSocket = obtainDataConnection(sourceFile.getParent())) {
                // download file
                downloadAction(sourceFile, sinkDirectory, newSocket);
                this.receiveResponse();
            }
        } catch (final Exception exception) {
            try {
                this.close();
            } catch (final Exception nestedException) {
                exception.addSuppressed(nestedException);
            }
            throw exception;
        }
        finally {
            this.close();
        }
    }

    /**
     * downloads a file from the remote server to the local system
     *
     * @param sourceFile path to file on the remote server
     * @param sinkDirectory path to target folder on local system
     * @param newSocket data connection socket
     * @throws IOException
     */
    private synchronized void downloadAction(Path sourceFile, Path sinkDirectory, Socket newSocket) throws IOException {
        // send RETR request
        FtpResponse response = this.sendRequest("RETR " + sourceFile.getFileName());
        if (response.getCode() == 150) {
            // if successful, copy stream content
            try (OutputStream outputFile = Files.newOutputStream(Paths.get(sinkDirectory.toString(), sourceFile.getFileName().toString()))) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = newSocket.getInputStream().read(buffer)) != -1) {
                    outputFile.write(buffer, 0, len);
                }
            }
        }
        else{
            throw new NoSuchFileException(sourceFile.toString());
        }
    }

    /**
     * Stores the given file on the FTP server side using a separate data connection. Note that the
     * source file resides on the client side and can therefore be a global path, while the target
     * directory resides on the server side and must be a relative path (relative to the FTP server
     * context directory), or {@code null}.
     *
     * @param sourceFile    the source file (client side)
     * @param sinkDirectory the sink directory (server side), may be empty
     * @throws NullPointerException  if the source file is {@code null}
     * @throws IllegalStateException if this client is closed
     * @throws NotDirectoryException if the sink directory does not exist
     * @throws AccessDeniedException if the source file cannot be read, or the sink directory cannot
     *                               be written
     * @throws IOException           if there is an I/O related problem
     */
    public synchronized void sendFile(final Path sourceFile, final Path sinkDirectory) throws IOException {
        if (this.isClosed()) throw new IllegalStateException();
        if (!Files.isReadable(sourceFile)) throw new NoSuchFileException(sourceFile.toString());

        try {
            // get data connection socket
            try (Socket newSocket = obtainDataConnection(sinkDirectory)) {
                // upload file
                uploadAction(sourceFile, newSocket);
            }
            this.receiveResponse();
        } catch (final Exception exception) {
            try {
                this.close();
            } catch (final Exception nestedException) {
                exception.addSuppressed(nestedException);
            }
            throw exception;
        }
        finally {
            this.close();
        }
    }

    /***
     * uploads a file to the remote server by transferring the stream contents
     *
     * @param sourceFile local path to the uploaded file
     * @param newSocket data connection socket
     * @throws IOException
     */
    private synchronized void uploadAction(Path sourceFile, Socket newSocket) throws IOException {
        FtpResponse response;
        // send STOR request
        response = this.sendRequest("STOR " + sourceFile.getFileName());
        if (response.getCode() == 150) {
            // if successful, upload file
            try (InputStream inputFile = Files.newInputStream(sourceFile)) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = inputFile.read(buffer)) != -1) {
                    newSocket.getOutputStream().write(buffer, 0, len);
                }
            }
        }
        else{
            throw new NoSuchFileException(sourceFile.toString());
        }
    }

    /**
     * Since there was a lot of code duplication, this method will handle the CWD and the PASV requests, to return the
     * data connection socket. This is so that we save a few lines.
     *
     * @param filePath path on the remote server
     * @return data connection socket
     * @throws IOException
     */
    private synchronized Socket obtainDataConnection(Path filePath) throws IOException {
        // kinda sorta global response for the end
        FtpResponse response;

        // does the file exist?
        if (filePath.getParent() != null) {
            // set cwd to parent of the file
            String cwd = filePath.toString().replace('\\', '/');
            response = this.sendRequest("CWD " + cwd);

            // if successful, obtain host and port for data connection socket
            if (response.getCode() == 250) {
                response = this.sendRequest("PASV");
            }

            if (response.getCode() == 227) {
                // open and return transfer socket
                return new Socket(response.getSocketAddress().getHostString(), response.getSocketAddress().getPort());
            }
        }
        // this is only here so there is no warning in the IDE
        return null;
    }

    //==================================================================================================================
    // THIS IS WHERE OUR CODE ENDS
    //==================================================================================================================

    /**
     * Sends an FTP request and returns it's initial response. Note that some kinds of FTP requests
     * (like {@code PORT} and {@code PASV}) will cause multiple FTP responses over time, therefore
     * all but the first need to be received separately using {@linkplain #receiveResponse()}.
     *
     * @param request the FTP request
     * @return an FTP response
     * @throws NullPointerException  if the given request is {@code null}
     * @throws IllegalStateException if this client is closed
     * @throws IOException           if there is an I/O related problem
     */
    protected synchronized FtpResponse sendRequest(final String request) throws IOException {
        if (this.isClosed()) throw new IllegalStateException();

        Logger.getGlobal().log(Level.INFO, request.startsWith("PASS") ? "PASS xxxxxxxx" : request);
        this.controlConnectionSink.write(request);
        this.controlConnectionSink.newLine();
        this.controlConnectionSink.flush();

        return this.receiveResponse();
    }

    /**
     * Parses a single FTP response from the control connection. Note that some kinds of FTP
     * requests will cause multiple FTP responses over time.
     *
     * @return an FTP response
     * @throws IllegalStateException if this client is closed
     * @throws IOException           if there is an I/O related problem
     */
    protected synchronized FtpResponse receiveResponse() throws IOException {
        if (this.isClosed()) throw new IllegalStateException();

        final FtpResponse response = FtpResponse.parse(this.controlConnectionSource);
        Logger.getGlobal().log(Level.INFO, response.toString());
        return response;
    }


}