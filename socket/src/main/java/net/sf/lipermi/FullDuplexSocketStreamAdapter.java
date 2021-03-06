package net.sf.lipermi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class FullDuplexSocketStreamAdapter implements TCPFullDuplexStream {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TCPFullDuplexStream.class);

  final Socket socket;

  public FullDuplexSocketStreamAdapter(Socket socket) {
    if( socket == null ) throw new IllegalArgumentException("socket argument is null!");
    this.socket = socket;
  }

  public Socket getSocket() {
    return socket;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return socket.getInputStream();
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    return socket.getOutputStream();
  }

  @Override
  public boolean isConnected() {
    return socket.isConnected();
  }

  @Override
  public InetAddress getInetAddress() {
    return socket.getInetAddress();
  }

  @Override
  public int getPort() {
    return socket.getPort();
  }

  @Override
  public void close() throws IOException {
    log.trace( "close socket!");
    socket.close();
  }
}
