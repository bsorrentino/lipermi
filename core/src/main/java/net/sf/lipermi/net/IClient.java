package net.sf.lipermi.net;

import net.sf.lipermi.handler.CallHandler;
import net.sf.lipermi.handler.CallProxy;
import net.sf.lipermi.handler.ConnectionHandler;

import java.io.Closeable;
import java.lang.reflect.Proxy;

import static java.lang.String.format;

/**
 *
 */
public interface IClient extends Closeable  {

  ConnectionHandler getConnectionHandler();

  default <T> T getGlobal(Class<T> clazz) {
    return CallHandler.getInterface( clazz ).map(ifc ->
        (T) Proxy.newProxyInstance(clazz.getClassLoader(),
            new Class[] { ifc },
            new CallProxy(getConnectionHandler())) )
        .orElseThrow( () -> new IllegalArgumentException(format("class %s is not a valid interface", clazz)));
  }

}
