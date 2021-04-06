package net.sf.lipermi.net;

import net.sf.lipermi.handler.AbstractConnectionHandler;
import net.sf.lipermi.handler.CallHandler;
import net.sf.lipermi.handler.CallProxy;

import java.io.Closeable;
import java.lang.reflect.Proxy;

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
public interface IClient<C extends AbstractConnectionHandler> extends Closeable {

    /**
     *
     * @return
     */
    C getConnectionHandler();

    /**
     *
     * @param clazz
     * @param <T>
     * @return
     */
    default <T> T getGlobal(Class<T> clazz) {
        return CallHandler.getInterface( clazz ).map( ifc ->
                    (T)Proxy.newProxyInstance(clazz.getClassLoader(),
                                new Class[] { ifc },
                                new CallProxy(getConnectionHandler())) )
                .orElseThrow( () -> new IllegalArgumentException(format("class %s is not a valid interface", clazz)));
    }

}
