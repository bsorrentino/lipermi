package net.sf.lipermi.socket;

import net.sf.lipermi.handler.CallHandler;
import net.sf.lipermi.handler.filter.DefaultFilter;
import net.sf.lipermi.handler.filter.IProtocolFilter;
import net.sf.lipermi.net.IClient;
import net.sf.lipermi.net.IServer;
import net.sf.lipermi.socket.handler.SocketConnectionHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import static java.util.Optional.ofNullable;


/**
 * The LipeRMI server.
 * This object listen to a specific port and
 * when a client connects it delegates the connection
 * to a {@link net.sf.lipermi.handler.AbstractConnectionHandler ConnectionHandler}.
 *
 * @author lipe
 * @date   05/10/2006
 *
 * @see    net.sf.lipermi.handler.CallHandler
 * @see    IClient
 */
public class SocketServer implements IServer {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SocketServer.class);

    private ServerSocket serverSocket;

    private boolean enabled;

    @Override
    public void close() throws IOException {
        enabled = false;
    }

    @Override
    public int bind(int port, final CallHandler callHandler, IProtocolFilter filter) throws IOException {
        serverSocket = new ServerSocket();
        serverSocket.setPerformancePreferences(1, 0, 2);
            enabled = true;

        if (port >= 0) {
            serverSocket.bind(new InetSocketAddress(port));
        } else {
            serverSocket.bind(null);
        }

        final Thread bindThread = new Thread(() -> {
            while (enabled) {
                Socket acceptSocket = null;
                try {
                    acceptSocket = serverSocket.accept();

                    SocketConnectionHandler.start( acceptSocket,
                                            callHandler,
                                            ofNullable(filter).orElseGet(DefaultFilter::new));

                } catch (IOException e) {
                    log.warn("bindThread error", e);
                }
            }
        }, String.format("Bind (%d)", port)); //$NON-NLS-1$ //$NON-NLS-2$
        bindThread.start();

        return serverSocket.getLocalPort();
    }

}
