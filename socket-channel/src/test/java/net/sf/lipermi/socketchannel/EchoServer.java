package net.sf.lipermi.socketchannel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class EchoServer extends AbstractSocketChannelServer {
    static {
        System.setProperty( "org.slf4j.simpleLogger.defaultLogLevel", "TRACE" );
    }
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EchoServer.class);

    private static final String POISON_PILL = "POISON_PILL";

    public static void main(String[] args) throws IOException {

        final EchoServer server = new EchoServer();

        server.bind(new InetSocketAddress("localhost", 5454));

    }


    @Override
    protected void processMessage(byte[] messageBytes, SelectionKey key) throws IOException {
        final SocketChannel client = (SocketChannel) key.channel();

        final String msg = new String(messageBytes).trim();
        log.info( "answerWithEcho( {} )", msg );

        if (POISON_PILL.equals(msg)) {
            client.close();
            log.info("Not accepting client messages anymore");
        } else {
            client.write( ByteBuffer.wrap(messageBytes) );
        }

    }

}