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

package net.sf.lipermi;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import net.sf.lipermi.handler.CallHandler;
import net.sf.lipermi.handler.ConnectionHandler;
import net.sf.lipermi.handler.filter.DefaultFilter;
import net.sf.lipermi.handler.filter.IProtocolFilter;
import net.sf.lipermi.net.BaseClient;


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
 * @see    BaseClient
 */
public class Server {

    private ServerSocket serverSocket;

    private boolean enabled;

    private List<IServerListener> listeners = new LinkedList<IServerListener>();

    public void addServerListener(IServerListener listener) {
        listeners.add(listener);
    }

    public void removeServerListener(IServerListener listener) {
        listeners.remove(listener);
    }

    public void close() {
        enabled = false;
    }

    public int bind(int port, final CallHandler callHandler, Optional<IProtocolFilter> filter) throws IOException {
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

                    final FullDuplexSocketStreamAdapter socketAdapter =
                            new FullDuplexSocketStreamAdapter(acceptSocket);
                    ConnectionHandler.of(   socketAdapter,
                                            callHandler,
                                            filter.orElseGet( () -> new DefaultFilter()),
                                            () -> {
                                                for (IServerListener listener : listeners)
                                                    listener.clientDisconnected(socketAdapter.getSocket());
                                            });
                    for (IServerListener listener : listeners)
                        listener.clientConnected(socketAdapter.getSocket());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, String.format("Bind (%d)", port)); //$NON-NLS-1$ //$NON-NLS-2$
        bindThread.start();

        return serverSocket.getLocalPort();
    }

}

// vim: ts=4:sts=4:sw=4:expandtab
