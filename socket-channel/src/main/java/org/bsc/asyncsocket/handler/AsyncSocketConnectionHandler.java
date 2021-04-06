package org.bsc.asyncsocket.handler;

import net.sf.lipermi.call.IRemoteMessage;
import net.sf.lipermi.handler.AbstractConnectionHandler;
import net.sf.lipermi.handler.CallHandler;
import net.sf.lipermi.handler.filter.IProtocolFilter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class AsyncSocketConnectionHandler extends AbstractConnectionHandler  {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AsyncSocketConnectionHandler.class);

  final AsynchronousSocketChannel sockChannel;

  public AsyncSocketConnectionHandler(AsynchronousSocketChannel sockChannel, CallHandler callHandler, IProtocolFilter filter) {
    super(callHandler, filter);

    this.sockChannel = sockChannel;
  }

  @Override
  protected void writeMessage(IRemoteMessage remoteMessage) throws IOException {

    final Object objToWrite = filter.prepareWrite(remoteMessage);

    final ByteArrayOutputStream bout  = new ByteArrayOutputStream();
    final ObjectOutputStream output   = new ObjectOutputStream(bout);

    output.reset();
    output.writeUnshared(objToWrite);
    output.flush();

    final ByteBuffer buf = ByteBuffer.wrap( bout.toByteArray() );

    sockChannel.write(buf, sockChannel, new CompletionHandler<Integer, AsynchronousSocketChannel>() {

      @Override
      public void completed(Integer result, AsynchronousSocketChannel channel) {
        //finish to write message to client, nothing to do
        log.trace("write.completed: {} - buf. {remaining:{}} ", result, buf.hasRemaining());
      }

      @Override
      public void failed(Throwable exc, AsynchronousSocketChannel channel) {
        //fail to write message to client
        log.warn( "Fail to write message to client", exc );
      }

    });

  }

  @Override
  protected boolean isConnected() {
    return sockChannel.isOpen();
  }

  @Override
  public void close() throws IOException {
    sockChannel.close();
  }

  /**
   *
   * @param sockChannel
   */
  public void startRead( AsynchronousSocketChannel sockChannel ) {

    final ByteBuffer buf = ByteBuffer.allocate(2048);

    //read message from client
    sockChannel.read( buf, sockChannel, new CompletionHandler<Integer, AsynchronousSocketChannel >() {

      /**
       * some message is read from client, this callback will be called
       */
      @Override
      public void completed(Integer result, AsynchronousSocketChannel channel  ) {
        log.trace("read.completed: {} - buf. {remaining:{}} ", result, buf.hasRemaining());

        buf.flip();

        //start to read next message again
        startRead( channel );
      }

      @Override
      public void failed(Throwable exc, AsynchronousSocketChannel channel ) {
        log.warn( "fail to read message from client", exc);
      }
    });
  }


}
