package net.sf.lipermi.socketchannel;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class EchoClient implements Closeable {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EchoClient.class);

    private final SocketChannel client;
    private ByteBuffer buffer = ByteBuffer.allocate(256);

    public EchoClient() throws IOException {
        client = SocketChannel.open(new InetSocketAddress("localhost", 5454));
    }

    @Override
    public void close() throws IOException {
        client.close();
        buffer = null;
    }

    public String sendMessage(String msg) {
        buffer = ByteBuffer.wrap(msg.getBytes());
        String response = null;
        try {
            client.write(buffer);
            buffer.clear();
            client.read(buffer);
            response = new String(buffer.array()).trim();
            System.out.println("response=" + response);
            buffer.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;

    }

    public static void main(String[] args) throws Exception {

        for( int instance = 0 ; instance < 20 ; ++instance ) {
            Thread.sleep( 1000 );
            new Thread( () -> {
                try (final EchoClient echo = new EchoClient()) {

                    for (int i = 0; i < 100; ++i) {
                        final String msg = String.format("message(%d) - %d", Thread.currentThread().getId(), i);
                        echo.sendMessage(msg);
                        Thread.sleep( 500 );
                    }
                }
                catch( Exception ex ) {
                    log.warn( "error creting client ", ex);
                }

            }, String.format( "Client[%d]", instance )).start();
        }
    }

}