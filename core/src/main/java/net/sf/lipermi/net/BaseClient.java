/*
 * LipeRMI - a light weight Internet approach for remote method invocation
 * Copyright (C) 2006  Felipe Santos Andrade
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * For more information, see http://lipermi.sourceforge.net/license.php
 * You can also contact author through lipeandrade@users.sourceforge.net
 */

package net.sf.lipermi.net;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import net.sf.lipermi.TCPFullDuplexStream;
import net.sf.lipermi.exception.LipeRMIException;
import net.sf.lipermi.handler.CallHandler;
import net.sf.lipermi.handler.CallProxy;
import net.sf.lipermi.handler.ConnectionHandler;
import net.sf.lipermi.handler.IConnectionHandlerListener;
import net.sf.lipermi.handler.filter.DefaultFilter;
import net.sf.lipermi.handler.filter.IProtocolFilter;


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

    private final List<IClientListener> listeners = new LinkedList<IClientListener>();

    private final IConnectionHandlerListener connectionHandlerListener = () -> {
            for (IClientListener listener : listeners)
                listener.disconnected();
    };
    protected BaseClient(TCPFullDuplexStream stream, CallHandler callHandler, Optional<IProtocolFilter> filter) {
        if( stream == null ) throw new IllegalArgumentException("stream argument is null!");
        if( callHandler == null ) throw new IllegalArgumentException("callHandler argument is null!");
        if( filter == null ) throw new IllegalArgumentException("filter argument is null!");

        TCPStream = stream;
        connectionHandler =
            ConnectionHandler.of(   stream,
                                    callHandler,
                                    filter.orElseGet( () -> new DefaultFilter()),
                                    connectionHandlerListener);
    }

    public void addClientListener(IClientListener listener) {
        listeners.add(listener);
    }

    public void removeClientListener(IClientListener listener) {
        listeners.remove(listener);
    }

    public <T> T getGlobal(Class<T> clazz) {
        return (T)Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] { clazz }, new CallProxy(connectionHandler));
    }

    public void exportObject(Class<?> cInterface, Object exportedObject) throws LipeRMIException {
        connectionHandler.getCallHandler().exportObject(cInterface, exportedObject);
    }

    public void close() throws IOException {
        TCPStream.close();
    }
}
