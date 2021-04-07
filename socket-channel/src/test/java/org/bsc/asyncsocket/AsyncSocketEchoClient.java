
package org.bsc.asyncsocket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;

/**
 *
 * @link https://gist.github.com/ochinchina/72cc23220dc8a933fc46#file-echoclient-java
 */
public class AsyncSocketEchoClient implements java.io.Closeable {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AsyncSocketEchoClient.class);

    final AsynchronousSocketChannel sockChannel;

    public AsyncSocketEchoClient(String host, int port, final String message, final AtomicInteger messageWritten, final AtomicInteger messageRead, final AtomicInteger messageError ) throws IOException {
        //create a socket channel
        sockChannel = AsynchronousSocketChannel.open();

        final CompletableFuture<Void> connectFuture =

        connect( sockChannel, new InetSocketAddress(host, port), null).thenCompose( nil ->
            //write an message to server side
            startWrite( sockChannel, message, null )
                    .thenApply( v -> messageWritten.getAndIncrement() )
                    .thenCompose( v ->
                            startRead( sockChannel, null )
                                    .thenApply( value  -> {
                                        log.info( "message: {}", value);
                                        return messageRead.getAndIncrement();
                                    })
                    )
                    .exceptionally( ex -> {
                        log.warn( "error reading/writing", ex );
                        return messageError.getAndIncrement();
                    })
        ).thenAccept( (nil) -> {
            try {
                sockChannel.close();
            } catch (IOException e) {
                log.warn( "error closing channel", e );
            }
        });

        connectFuture.join();
    }

    @Override
    public void close() throws IOException {
        sockChannel.close();
    }

    private String bufferToString(ByteBuffer buffer, int length ) {

        if(buffer.hasArray()) {
            return new String(buffer.array(), 0, length, StandardCharsets.UTF_8);
        }

        byte[] bytes = new byte[length];
        buffer.get(bytes);

        return new String(bytes, StandardCharsets.UTF_8);
    }

    private <Attach> CompletableFuture<String> startRead( final AsynchronousSocketChannel sockChannel, Attach attachment) {
        final ByteBuffer buf = ByteBuffer.allocate(2048);
        final CompletableFuture<String> futureResult = new CompletableFuture<>();

        sockChannel.read( buf, attachment, new CompletionHandler<Integer, Attach>(){

            @Override
            public void completed(Integer result, Attach attachment) {
                futureResult.complete( bufferToString(buf, result) );
            }

            @Override
            public void failed(Throwable exc, Attach attachment) {
                futureResult.completeExceptionally(exc);
            }
            
        });

        return futureResult;
        
    }

    private <Attach> CompletableFuture<Integer> startWrite(final AsynchronousSocketChannel sockChannel, final String message, Attach attachment ) {

        final ByteBuffer buf = ByteBuffer.allocate(2048);
        buf.put(message.getBytes());
        buf.flip();

        final CompletableFuture<Integer> futureResult = new CompletableFuture<>();

        sockChannel.write(buf, attachment, new CompletionHandler<Integer, Attach>() {
            @Override
            public void completed(Integer result, Attach attachment ) {
                //after message written
                //NOTHING TO DO
                futureResult.complete(result);
            }

            @Override
            public void failed(Throwable exc, Attach attachment) {
                futureResult.completeExceptionally(exc);
            }
        });

        return futureResult;
    }

    /**
     *
     * @param socketChannel
     * @param address
     * @param attachment
     * @param <Attach>
     * @return
     */
    private <Attach> CompletableFuture<Void> connect(AsynchronousSocketChannel socketChannel, InetSocketAddress address, final Attach attachment ) {

        final CompletableFuture<Void> futureResult = new CompletableFuture<>();

        sockChannel.connect( address, attachment, new CompletionHandler<Void, Attach>() {

            @Override
            public void completed(Void result, Attach attachment) {
                futureResult.complete(null);
            }

            @Override
            public void failed(Throwable exc, Attach attachment) {
                futureResult.completeExceptionally(exc);
            }
        });

        return futureResult;
    }

    public static void main( String...args ) {
        try {

            int steps = 1000;
            AtomicInteger messageWritten = new AtomicInteger( 0 );
            AtomicInteger messageRead = new AtomicInteger( 0 );
            AtomicInteger messageError = new AtomicInteger( 0 );

            for( int i = 0; i < steps; i++ ) {
                new AsyncSocketEchoClient(
                        "127.0.0.1",
                        3575,
                        format("echo test %04d", i ),
                        messageWritten,
                        messageRead,
                        messageError );

            }
            
            while( messageRead.get() + messageError.get() < steps ) {
                Thread.sleep( 1000 );
            }
            log.info( "message write: {}", messageWritten );
            log.info( "message read: {}", messageRead );
        } catch (Exception ex) {
            log.error( "error in main", ex);
        }
    }
}
