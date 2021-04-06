package net.sf.lipermi.socket.handler;

import net.sf.lipermi.call.IRemoteMessage;
import net.sf.lipermi.handler.AbstractConnectionHandler;
import net.sf.lipermi.handler.CallHandler;
import net.sf.lipermi.handler.filter.IProtocolFilter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import static java.lang.String.format;

/**
 *
 */
public class SocketConnectionHandler extends AbstractConnectionHandler implements Runnable {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SocketConnectionHandler.class);

  /**
   *
   * @param socket
   * @param callHandler
   * @param filter
   * @return
   */
  public static SocketConnectionHandler start(Socket socket, CallHandler callHandler, IProtocolFilter filter) {
    SocketConnectionHandler connectionHandler = new SocketConnectionHandler(socket, callHandler, filter);

    String threadName = format("ConnectionHandler (%s:%d)", socket.getInetAddress().getHostAddress(), socket.getPort());
    Thread connectionHandlerThread = new Thread(connectionHandler, threadName);
    connectionHandlerThread.setDaemon(true);
    connectionHandlerThread.start();

    return connectionHandler;
  }

  /**
   *
   * @param socket
   * @param callHandler
   * @param filter
   */
  public SocketConnectionHandler(Socket socket, CallHandler callHandler, IProtocolFilter filter) {
    super(callHandler, filter);
    this.socket = socket;
  }

  private final Socket socket;
  private ObjectOutputStream output;

  /**
   *
   * @param remoteMessage
   * @throws IOException
   */
  @Override
  protected void writeMessage(IRemoteMessage remoteMessage) throws IOException {

    synchronized (socket) {
      if (output == null)
        output = new ObjectOutputStream(socket.getOutputStream());

      final Object objToWrite = filter.prepareWrite(remoteMessage);
      output.reset();
      output.writeUnshared(objToWrite);
      output.flush();
    }

  }

  /**
   *
   * @return
   */
  @Override
  protected boolean isConnected() {
    return socket.isConnected();
  }

  /**
   *
   */
  @Override
  public void run() {
    try (final ObjectInputStream input = new ObjectInputStream(socket.getInputStream())) {

      while (socket.isConnected()) {

        final Object objectFromSteam = input.readUnshared();

        final IRemoteMessage remoteMessage = filter.readObject(objectFromSteam);

        readMessage( remoteMessage );
      }
    }
    catch( java.io.EOFException eof ) {
      log.warn("ConnectionHandler EOFException!");
    }
    catch (Exception e) {
      log.error("ConnectionHandler exception. Closing tcpStream!", e);
      try {
        socket.close();
      } catch (IOException ex) {
        log.warn("error closing tcpStream: {}", ex.getMessage());
      }
      synchronized (remoteReturns) {
        remoteReturns.notifyAll();
      }
    }
  }

  /**
   *
   * @throws IOException
   */
  @Override
  public void close() throws IOException {
    socket.close();
  }
}
