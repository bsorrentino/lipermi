package net.sf.lipermi.socketchannel;

import net.sf.lipermi.handler.CallHandler;
import net.sf.lipermi.handler.filter.IProtocolFilter;
import net.sf.lipermi.net.IServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class ChannelSocketServer implements IServer {

    public static final int DEFAULT_BUFFER_SIZE = 4*1024;

    private boolean enabled;

    @Override
    public void close() throws IOException {
        enabled = false;
    }

    private void register(Selector selector, ServerSocketChannel serverSocket) throws IOException {
        final SocketChannel client = serverSocket.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE );
    }

    @Override
    public int bind(int port, CallHandler callHandler, IProtocolFilter filter) throws IOException {

        final Selector selector = Selector.open();
        final ServerSocketChannel serverSocket = ServerSocketChannel.open();

        serverSocket.bind(new InetSocketAddress(port));
        serverSocket.configureBlocking(false);
        serverSocket.register(selector, SelectionKey.OP_ACCEPT);

        final ByteBuffer buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);

        enabled = true;

        while (enabled) {
            selector.select();
            final Set<SelectionKey> selectedKeys = selector.selectedKeys();
            final Iterator<SelectionKey> keys = selectedKeys.iterator();
            while (keys.hasNext()) {

                final SelectionKey key = keys.next();

                if (key.isAcceptable()) {
                    register(selector, serverSocket);
                }

                if (key.isReadable()) {
                    //answerWithEcho(buffer, key);
                }

                if (key.isWritable()) {
                    //answerWithEcho(buffer, key);
                }

                keys.remove();
            }
        }
        return 0;
    }
}
