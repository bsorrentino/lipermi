package net.sf.lipermi.socket;

import net.sf.lipermi.handler.CallHandler;
import net.sf.lipermi.handler.filter.DefaultFilter;
import net.sf.lipermi.handler.filter.IProtocolFilter;
import net.sf.lipermi.net.IClient;
import net.sf.lipermi.socket.handler.SocketConnectionHandler;

import java.io.IOException;
import java.net.Socket;

import static java.util.Optional.ofNullable;


/**
 * The LipeRMI client.
 * Connects to a LipeRMI Server in a address:port
 * and create local dynamic proxys to call remote
 * methods through a simple interface.
 *
 * @author lipe
 * @date   05/10/2006
 *
 * @see    net.sf.lipermi.handler.CallHandler
 * @see    SocketServer
 */
public class SocketClient implements IClient<SocketConnectionHandler> {

    final SocketConnectionHandler connectionHandler;

    public SocketClient(String address, int port, CallHandler callHandler, IProtocolFilter filter) throws IOException {
        if( callHandler == null ) throw new IllegalArgumentException("callHandler argument is null!");

        connectionHandler =
            SocketConnectionHandler.start(new Socket( address, port ),
                callHandler,
                ofNullable(filter).orElseGet(DefaultFilter::new));

    }

    @Override
    public SocketConnectionHandler getConnectionHandler() {
        return connectionHandler;
    }

    @Override
    public void close() throws IOException {
        connectionHandler.close();
    }
}
