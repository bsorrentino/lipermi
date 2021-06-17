package net.sf.lipermi;

import net.sf.lipermi.handler.CallHandler;
import net.sf.lipermi.handler.ConnectionHandler;
import net.sf.lipermi.handler.filter.DefaultFilter;
import net.sf.lipermi.handler.filter.IProtocolFilter;
import net.sf.lipermi.net.IServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.Optional.ofNullable;


/**
 * The LipeRMI server.
 * This object listen to a specific port and
 * when a client connects it delegates the connection
 * to a {@link net.sf.lipermi.handler.ConnectionHandler ConnectionHandler}.
 *
 * @author lipe
 * @date   05/10/2006
 *
 * @see    net.sf.lipermi.handler.CallHandler
 * @see    IServer
 */
public class SocketServer implements IServer {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SocketServer.class);

    private ServerSocket serverSocket;
    private boolean enabled;

    private ExecutorService threadPool = Executors.newCachedThreadPool( ConnectionHandler.threadFactory );

    @Override
    public void close() throws IOException {
        enabled = false;
        threadPool.shutdown();
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

        final IProtocolFilter _filter = ofNullable(filter).orElseGet(DefaultFilter::new);

        final Thread bindThread = new Thread(() -> {
            while (enabled) {

                try {
                    final Socket acceptSocket = serverSocket.accept();

                    final FullDuplexSocketStreamAdapter socketAdapter =
                            new FullDuplexSocketStreamAdapter(acceptSocket);

                    threadPool.execute( ConnectionHandler.of( socketAdapter, callHandler, _filter ) );
                    // ConnectionHandler.start( socketAdapter, callHandler, _filter );

                } catch (IOException e) {
                    log.warn("bindThread error", e);
                }
            }
        }, String.format("Bind (%d)", port));
        bindThread.start();

        return serverSocket.getLocalPort();
    }

}
