package net.sf.lipermi;

import net.sf.lipermi.handler.CallHandler;
import net.sf.lipermi.handler.ConnectionHandler;
import net.sf.lipermi.handler.filter.DefaultFilter;
import net.sf.lipermi.handler.filter.IProtocolFilter;
import net.sf.lipermi.net.IClient;

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
 * @see    IClient
 */
public class SocketClient implements IClient {

    private final TCPFullDuplexStream TCPStream;

    private final ConnectionHandler connectionHandler;

    public SocketClient(String address, int port, CallHandler callHandler, IProtocolFilter filter) throws IOException {

        TCPStream = new FullDuplexSocketStreamAdapter(new Socket( address, port ));
        connectionHandler =
            ConnectionHandler.start(TCPStream,
                callHandler,
                ofNullable(filter).orElseGet(DefaultFilter::new));

    }

    @Override
    public ConnectionHandler getConnectionHandler() {
        return connectionHandler;
    }

    public void close() throws IOException {
        TCPStream.close();
    }

}
