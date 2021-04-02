package net.sf.lipermi;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;

public interface TCPFullDuplexStream extends Closeable {

  java.io.InputStream getInputStream() throws IOException;

  java.io.OutputStream getOutputStream() throws IOException;

  boolean isConnected();

  InetAddress getInetAddress();

  int getPort();

  void close() throws IOException;
}
