package org.bsc.asyncsocket;;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

/**
 *
 * @link https://gist.github.com/ochinchina/72cc23220dc8a933fc46#file-echoserver-java
 */
public class AsyncSocketEchoServer implements Closeable {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AsyncSocketEchoServer.class);

    final AsynchronousServerSocketChannel serverSock;


    public AsyncSocketEchoServer(String bindAddr, int bindPort ) throws IOException {
        final InetSocketAddress sockAddr = new InetSocketAddress(bindAddr, bindPort);
        
        //create a socket channel and bind to local bind address
        serverSock =  AsynchronousServerSocketChannel.open().bind(sockAddr);

        //start to accept the connection from client
        serverSock.accept(serverSock, new CompletionHandler<AsynchronousSocketChannel, AsynchronousServerSocketChannel>() {

            @Override
            public void completed(AsynchronousSocketChannel sockChannel, AsynchronousServerSocketChannel serverSock) {
                log.trace( "accept.completed");
                //a connection is accepted, start to accept next connection
                serverSock.accept(serverSock, this);
                //start to read message from the client
                startRead(sockChannel);

            }

            @Override
            public void failed(Throwable exc, AsynchronousServerSocketChannel serverSock) {
                log.warn("fail to accept a connection", exc);
            }

        });

    }


    @Override
    public void close() throws IOException {
        log.trace( "server.close");

        serverSock.close();
    }

    private void startRead( AsynchronousSocketChannel sockChannel ) {
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

                // echo the message
                startWrite(channel, buf);

                //start to read next message again
                startRead( channel );
            }

            @Override
            public void failed(Throwable exc, AsynchronousSocketChannel channel ) {
                log.warn( "fail to read message from client", exc);
            }
        });
    }
    
     private void startWrite( AsynchronousSocketChannel sockChannel, final ByteBuffer buf) {
         sockChannel.write(buf, sockChannel, new CompletionHandler<Integer, AsynchronousSocketChannel >() {

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
     
     public static void main( String[] args ) {
        try( final AsyncSocketEchoServer server = new AsyncSocketEchoServer( "127.0.0.1", 3575 )) {
            Thread.currentThread().join();
        } catch (Exception ex) {
            log.error( "error in main", ex);
        }
     }
}
