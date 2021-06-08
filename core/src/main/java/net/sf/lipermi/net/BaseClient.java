package net.sf.lipermi.net;

import net.sf.lipermi.TCPFullDuplexStream;
import net.sf.lipermi.handler.CallHandler;
import net.sf.lipermi.handler.CallProxy;
import net.sf.lipermi.handler.ConnectionHandler;
import net.sf.lipermi.handler.IConnectionHandlerListener;
import net.sf.lipermi.handler.filter.IProtocolFilter;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.LinkedList;
import java.util.List;

import static java.lang.String.format;

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
 */
public class BaseClient implements Closeable {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BaseClient.class);

    private final TCPFullDuplexStream TCPStream;

    private final ConnectionHandler connectionHandler;

    private final List<IClientListener> listeners = new LinkedList<IClientListener>();

    private final IConnectionHandlerListener connectionHandlerListener = () -> {
            for (IClientListener listener : listeners)
                listener.disconnected();
    };
    protected BaseClient(TCPFullDuplexStream stream, CallHandler callHandler, IProtocolFilter filter) {
        if( stream == null ) throw new IllegalArgumentException("stream argument is null!");
        if( callHandler == null ) throw new IllegalArgumentException("callHandler argument is null!");
        if( filter == null ) throw new IllegalArgumentException("filter argument is null!");

        TCPStream = stream;
        connectionHandler =
            ConnectionHandler.start(   stream,
                                    callHandler,
                                    filter);
    }

    public void addClientListener(IClientListener listener) {
        listeners.add(listener);
    }

    public void removeClientListener(IClientListener listener) {
        listeners.remove(listener);
    }

    /**
     *
     * @param clazz
     * @param <T>
     * @return
     */
    public <T> T getGlobal(Class<T> clazz) {
        return CallHandler.getInterface( clazz ).map( ifc -> {
                   log.trace( "getGlobal({})={}", clazz, ifc);
                   return  (T)Proxy.newProxyInstance(clazz.getClassLoader(),
                                new Class[] { ifc },
                                new CallProxy(connectionHandler));
                })
                .orElseThrow( () -> new IllegalArgumentException(format("class %s is not a valid interface", clazz)));
    }

    public void close() throws IOException {
        TCPStream.close();
    }
}
