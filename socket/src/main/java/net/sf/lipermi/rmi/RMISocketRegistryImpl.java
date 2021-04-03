package net.sf.lipermi.rmi;

import net.sf.lipermi.SocketClient;
import net.sf.lipermi.SocketServer;
import net.sf.lipermi.handler.CallHandler;
import net.sf.lipermi.handler.filter.IProtocolFilter;

/**
 *
 */
public class RMISocketRegistryImpl extends AbstractRegistry {

    public RMISocketRegistryImpl(String host, int port ) {
        super( host, port );
    }

    @Override
    protected SocketServer newServer() throws Exception {
        return new SocketServer();
    }

    @Override
    protected SocketClient newClient(String host, int port, CallHandler callHandler, IProtocolFilter filter) throws Exception {
        return new SocketClient(host, port, UnicastRemoteObject.callHandler, filter);
    }

}
