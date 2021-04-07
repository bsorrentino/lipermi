package org.bsc.asyncsocket;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CompletableFuture;

public class AsyncSocketEchoServer2 implements Closeable  {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AsyncSocketEchoServer2.class);

    final AsynchronousServerSocketChannel serverSock;


    public AsyncSocketEchoServer2(String bindAddr, int bindPort ) throws IOException {
        final InetSocketAddress sockAddr = new InetSocketAddress(bindAddr, bindPort);

        //create a socket channel and bind to local bind address
        serverSock = AsynchronousServerSocketChannel.open().bind(sockAddr);

        accept(serverSock);
    }


    @Override
    public void close() throws IOException {
        log.trace( "server.close");
        serverSock.close();
    }

    CompletableFuture<Void> accept(AsynchronousServerSocketChannel serverSock  ) {

        return acceptChannel(serverSock)
                .thenCompose( this::startRead )
                .thenAccept( r -> accept( serverSock) );


    }

    /**
     *
     * @param channel
     * @return
     */
    CompletableFuture<Void> startRead( AsynchronousSocketChannel channel ) {
        final ByteBuffer buf = ByteBuffer.allocate(2 * 1024);

        return readFromChannel( channel, buf)
                .thenCompose( result -> writeToChannel(channel, buf.flip()) )
                .thenAccept( result -> startRead(channel) )
                ;
    }

    /**
     *
     * @param serverSock
     * @return
     */
    private CompletableFuture<AsynchronousSocketChannel> acceptChannel(AsynchronousServerSocketChannel serverSock ) {

        final CompletableFuture<AsynchronousSocketChannel> resultFuture = new CompletableFuture<>();

        if( !serverSock.isOpen() ) {
            log.trace( "serverSock is closed!");
            resultFuture.completeExceptionally( new IllegalStateException("serverSock is closed!"));

        }
        else {

            //start to accept the connection from client
            serverSock.accept(null, new CompletionHandler<AsynchronousSocketChannel, Object>() {

                @Override
                public void completed(AsynchronousSocketChannel sockChannel, Object attachment) {
                    log.debug("accept.completed");
                    resultFuture.complete(sockChannel);
                }

                @Override
                public void failed(Throwable exc, Object attachment) {
                    if (log.isTraceEnabled()) log.warn("fail to accept a connection", exc);
                    resultFuture.completeExceptionally(exc);
                }

            });
        }

        return resultFuture;
    }

    private CompletableFuture<Integer> readFromChannel(AsynchronousSocketChannel sockChannel, ByteBuffer buf ) {

        if( !sockChannel.isOpen() ) {
            log.trace( "sockChannel is closed!");
            return CompletableFuture.completedFuture(-1);
        }

        final CompletableFuture<Integer> resultFuture = new CompletableFuture<>();

        //read message from client
        sockChannel.read( buf, null, new CompletionHandler<Integer, Object>() {

            /**
             * some message is read from client, this callback will be called
             */
            @Override
            public void completed(Integer result, Object attachment  ) {

                if( result == - 1 ) {
                    try {
                        sockChannel.close();
                    } catch (IOException e) {
                        log.warn( "error closing channel");
                    }
                }

                log.trace("read.completed: {} ", result);
                resultFuture.complete(result);
            }

            @Override
            public void failed(Throwable exc, Object object ) {
                if( log.isTraceEnabled() ) log.warn( "Fail to read message from client", exc );

                resultFuture.completeExceptionally(exc);
            }
        });

        return resultFuture;
    }

    private CompletableFuture<Integer> writeToChannel(AsynchronousSocketChannel sockChannel, ByteBuffer buf) {

        if( !sockChannel.isOpen() ) {
            log.trace( "sockChannel is closed!");
            return CompletableFuture.completedFuture(-1);
        }

        final CompletableFuture<Integer> resultFuture = new CompletableFuture<>();

        sockChannel.write(buf, null, new CompletionHandler<Integer, Object>() {

            @Override
            public void completed(Integer result, Object attachment) {
                if( result == - 1 ) {
                    try {
                        sockChannel.close();
                    } catch (IOException e) {
                        log.warn( "error closing channel");
                    }
                }

                log.trace("write.completed: {} ", result);

                resultFuture.complete(result);
            }

            @Override
            public void failed(Throwable exc, Object attachment) {
                if( log.isTraceEnabled() ) log.warn( "Fail to write message to client", exc );

                //fail to write message to client
                resultFuture.completeExceptionally(exc);

            }

        });

        return resultFuture;
    }

    public static void main( String[] args ) {

        log.info( "starting server ....");
        try( final AsyncSocketEchoServer2 server = new AsyncSocketEchoServer2( "localhost", 3575 )) {
            log.info( "server started");
            Thread.currentThread().join();
        } catch (Exception ex) {
            log.error( "error in main", ex);
        }
    }

}
