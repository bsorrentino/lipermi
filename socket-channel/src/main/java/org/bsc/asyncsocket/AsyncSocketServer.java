package org.bsc.asyncsocket;

import net.sf.lipermi.handler.CallHandler;
import net.sf.lipermi.handler.filter.IProtocolFilter;
import net.sf.lipermi.net.IServer;
import org.bsc.asyncsocket.handler.AsyncSocketConnectionHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class AsyncSocketServer implements IServer {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AsyncSocketServer.class);

  AsynchronousServerSocketChannel serverSock;

  AsyncSocketConnectionHandler handler;

  @Override
  public void bind(int port, CallHandler callHandler, IProtocolFilter filter) throws IOException {

    final InetSocketAddress sockAddr = new InetSocketAddress(port);

    //create a socket channel and bind to local bind address
    serverSock =  AsynchronousServerSocketChannel.open().bind(sockAddr);
    //start to accept the connection from client

    serverSock.accept(serverSock, new CompletionHandler<AsynchronousSocketChannel, AsynchronousServerSocketChannel>() {

      @Override
      public void completed(AsynchronousSocketChannel sockChannel, AsynchronousServerSocketChannel serverSock) {
        log.trace( "accept.completed");
        //a connection is accepted, start to accept next connection
        serverSock.accept(serverSock, this);

        handler = new AsyncSocketConnectionHandler(sockChannel, callHandler, filter);

        //start to read message from the client
        handler.startRead(sockChannel);

      }

      @Override
      public void failed(Throwable exc, AsynchronousServerSocketChannel serverSock) {
        log.warn("fail to accept a connection", exc);
      }

    });

  }

  @Override
  public void close() throws IOException {
    serverSock.close();
  }
}
