package net.sf.lipermi.net;

import net.sf.lipermi.TCPFullDuplexStream;
import net.sf.lipermi.handler.CallHandler;
import net.sf.lipermi.handler.CallProxy;
import net.sf.lipermi.handler.ConnectionHandler;
import net.sf.lipermi.handler.IConnectionHandlerListener;
import net.sf.lipermi.handler.filter.DefaultFilter;
import net.sf.lipermi.handler.filter.IProtocolFilter;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
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
 */
public class BaseClient implements Closeable {

    private final TCPFullDuplexStream TCPStream;

    private final ConnectionHandler connectionHandler;

    protected BaseClient(TCPFullDuplexStream stream, CallHandler callHandler, IProtocolFilter filter) {
        if( stream == null ) throw new IllegalArgumentException("stream argument is null!");
        if( callHandler == null ) throw new IllegalArgumentException("callHandler argument is null!");

        TCPStream = stream;
        connectionHandler =
            ConnectionHandler.start(stream,
                                    callHandler,
                                    ofNullable(filter).orElseGet(DefaultFilter::new));
    }

    /**
     *
     * @param clazz
     * @param <T>
     * @return
     */
    public <T> T getGlobal(Class<T> clazz) {
        return CallHandler.getInterface( clazz ).map( ifc ->
                    (T)Proxy.newProxyInstance(clazz.getClassLoader(),
                                new Class[] { ifc },
                                new CallProxy(connectionHandler)) )
                .orElseThrow( () -> new IllegalArgumentException(format("class %s is not a valid interface", clazz)));
    }

    public void close() throws IOException {
        TCPStream.close();
    }
}
