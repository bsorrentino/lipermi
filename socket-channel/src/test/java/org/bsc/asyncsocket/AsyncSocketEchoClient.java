
package org.bsc.asyncsocket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @link https://gist.github.com/ochinchina/72cc23220dc8a933fc46#file-echoclient-java
 */
public class AsyncSocketEchoClient implements java.io.Closeable {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AsyncSocketEchoClient.class);

    final AsynchronousSocketChannel sockChannel;

    public AsyncSocketEchoClient(String host, int port, final String message, final AtomicInteger messageWritten, final AtomicInteger messageRead ) throws IOException {
        //create a socket channel
        sockChannel = AsynchronousSocketChannel.open();
        
        //try to connect to the server side
        sockChannel.connect( new InetSocketAddress(host, port), sockChannel, new CompletionHandler<Void, AsynchronousSocketChannel >() {
            @Override
            public void completed(Void result, AsynchronousSocketChannel channel ) {
                //start to read message
                startRead( channel,messageRead );
                
                //write an message to server side
                startWrite( channel, message, messageWritten );
            }

            @Override
            public void failed(Throwable exc, AsynchronousSocketChannel channel) {
                log.warn( "fail to connect to server", exc);
            }
            
        });
    }

    @Override
    public void close() throws IOException {
        sockChannel.close();
    }

    private void startRead( final AsynchronousSocketChannel sockChannel, final AtomicInteger messageRead ) {
        final ByteBuffer buf = ByteBuffer.allocate(2048);
        
        sockChannel.read( buf, sockChannel, new CompletionHandler<Integer, AsynchronousSocketChannel>(){

            @Override
            public void completed(Integer result, AsynchronousSocketChannel channel) {   
                //message is read from server
                messageRead.getAndIncrement();
                
                //print the message
                log.trace( "Read message: {}",  new String(buf.array()) );
            }

            @Override
            public void failed(Throwable exc, AsynchronousSocketChannel channel) {
                log.warn( "fail to read message from server", exc);
            }
            
        });
        
    }
    private void startWrite( final AsynchronousSocketChannel sockChannel, final String message, final AtomicInteger messageWritten ) {
        ByteBuffer buf = ByteBuffer.allocate(2048);
        buf.put(message.getBytes());
        buf.flip();
        messageWritten.getAndIncrement();
        sockChannel.write(buf, sockChannel, new CompletionHandler<Integer, AsynchronousSocketChannel >() {
            @Override
            public void completed(Integer result, AsynchronousSocketChannel channel ) {
                //after message written
                //NOTHING TO DO
            }

            @Override
            public void failed(Throwable exc, AsynchronousSocketChannel channel) {
                log.warn( "Fail to write the message to server", exc);
            }
        });
    }
    
    public static void main( String...args ) {
        try {
            AtomicInteger messageWritten = new AtomicInteger( 0 );
            AtomicInteger messageRead = new AtomicInteger( 0 );
            
            for( int i = 0; i < 1000; i++ ) {
                new AsyncSocketEchoClient( "127.0.0.1", 3575, "echo test", messageWritten, messageRead );

            }
            
            while( messageRead.get() != 1000 ) {
                Thread.sleep( 1000 );
            }
            log.info( "message write: {}", messageWritten );
            log.info( "message read: {}", messageRead );
        } catch (Exception ex) {
            log.error( "error in main", ex);
        }
    }
}
