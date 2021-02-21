package net.sf.lipermi.handler;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import net.sf.lipermi.TCPFullDuplexStream;
import net.sf.lipermi.call.IRemoteMessage;
import net.sf.lipermi.call.RemoteCall;
import net.sf.lipermi.call.RemoteInstance;
import net.sf.lipermi.call.RemoteReturn;
import net.sf.lipermi.exception.LipeRMIException;
import net.sf.lipermi.handler.filter.IProtocolFilter;
import net.sf.lipermi.net.BaseClient;

import static java.lang.String.format;
import static java.util.Optional.empty;


/**
 * A ConnectionHandler is object which can call remote
 * methods, receive remote calls and dispatch its returns.
 *
 * @author lipe
 * @date 05/10/2006
 * @see net.sf.lipermi.handler.CallHandler
 * @see net.sf.lipermi.call.RemoteInstance
 * @see net.sf.lipermi.call.RemoteCall
 * @see net.sf.lipermi.call.RemoteReturn
 * @see BaseClient
 * @see net.sf.lipermi.handler.filter.DefaultFilter
 */
public class ConnectionHandler implements Runnable {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ConnectionHandler.class);

    public static ConnectionHandler of(TCPFullDuplexStream socket, CallHandler callHandler, IProtocolFilter filter) {
        ConnectionHandler connectionHandler = new ConnectionHandler(socket, callHandler, filter);

        String threadName = format("ConnectionHandler (%s:%d)", socket.getInetAddress().getHostAddress(), socket.getPort());
        Thread connectionHandlerThread = new Thread(connectionHandler, threadName);
        connectionHandlerThread.setDaemon(true);
        connectionHandlerThread.start();

        return connectionHandler;
    }

    public static ConnectionHandler of(TCPFullDuplexStream socket, CallHandler callHandler, IProtocolFilter filter, IConnectionHandlerListener listener) {
        ConnectionHandler connectionHandler = of(socket, callHandler, filter);
        connectionHandler.addConnectionHandlerListener(listener);
        return connectionHandler;
    }

    private final CallHandler callHandler;

    private TCPFullDuplexStream tcpStream;

    private ObjectOutputStream output;

    private static AtomicLong callId = new AtomicLong(0L);

    private IProtocolFilter filter;

    private List<IConnectionHandlerListener> listeners = new LinkedList<IConnectionHandlerListener>();

    private Map<RemoteInstance, Object> remoteInstanceProxys = new HashMap<RemoteInstance, Object>();

    private List<RemoteReturn> remoteReturns = new LinkedList<RemoteReturn>();

    public void addConnectionHandlerListener(IConnectionHandlerListener listener) {
        listeners.add(listener);
    }

    public void removeConnectionHandlerListener(IConnectionHandlerListener listener) {
        listeners.remove(listener);
    }

    private ConnectionHandler(TCPFullDuplexStream socket, CallHandler callHandler, IProtocolFilter filter) {
        this.callHandler = callHandler;
        this.tcpStream = socket;
        this.filter = filter;
    }

    public void run() {
        try (
                final ObjectInputStream input = new ObjectInputStream(tcpStream.getInputStream())
        )
        {

            while (tcpStream.isConnected()) {

                final Object objFromStream = input.readUnshared();

                final IRemoteMessage remoteMessage = filter.readObject(objFromStream);

                if (remoteMessage instanceof RemoteCall) {

                    final RemoteCall remoteCall = (RemoteCall) remoteMessage;
                    if (remoteCall.getArgs() != null) {
                        for (int n = 0; n < remoteCall.getArgs().length; n++) {
                            Object arg = remoteCall.getArgs()[n];
                            if (arg instanceof RemoteInstance) {
                                RemoteInstance remoteInstance = (RemoteInstance) arg;
                                remoteCall.getArgs()[n] = getProxyFromRemoteInstance(remoteInstance);
                            }
                        }
                    }

                    final Thread delegator = new Thread(() -> {
                        CallLookup.handlingMe(ConnectionHandler.this);

                        try {

                            log.trace("remoteCall: {} - {}", remoteCall.getCallId(), remoteCall.getRemoteInstance().getInstanceId());
                            final RemoteReturn remoteReturn = callHandler.delegateCall(remoteCall);

                            log.trace("remote return {} = {}", remoteCall.getCallId(), remoteReturn.getRet());
                            sendMessage(remoteReturn);

                        } catch (Exception e) {
                            log.error("remoteCall error", e);
                        }

                        CallLookup.forgetMe();
                    }, "Delegator");
                    delegator.setDaemon(true);
                    delegator.start();
                } else if (remoteMessage instanceof RemoteReturn) {
                    RemoteReturn remoteReturn = (RemoteReturn) remoteMessage;
                    synchronized (remoteReturns) {
                        remoteReturns.add(remoteReturn);
                        remoteReturns.notifyAll();
                    }
                } else
                    throw new LipeRMIException("Unknown IRemoteMessage type");

            }
        }
        catch( java.io.EOFException eof ) {
            log.warn("ConnectionHandler EOFException!");
        }
        catch (Exception e) {
            try {
                log.error("ConnectionHandler exception. Closing tcpStream!", e);
                tcpStream.close();
            } catch (IOException ex) {
                log.warn("error closing tcpStream: {}", ex.getMessage());
            }

            synchronized (remoteReturns) {
                remoteReturns.notifyAll();
            }

            for (IConnectionHandlerListener listener : listeners)
                listener.connectionClosed();
        }
    }

    private synchronized void sendMessage(IRemoteMessage remoteMessage) throws IOException {
        if (output == null)
            output = new ObjectOutputStream(tcpStream.getOutputStream());

        Object objToWrite = filter.prepareWrite(remoteMessage);
        output.reset();
        output.writeUnshared(objToWrite);
        output.flush();
    }

    final synchronized Object remoteInvocation(final Object proxy, final Method method, final Object[] args) throws Throwable {
        final Long id = callId.getAndIncrement();

        final RemoteInstance remoteInstance =
                getRemoteInstanceFromProxy(proxy)
                        .orElseGet( () -> new RemoteInstance(null, proxy.getClass().getInterfaces()[0].getName()) );

        if (args != null) {
            for (int n = 0; n < args.length; n++) {
                final Optional<RemoteInstance> remoteRef = callHandler.getRemoteReference(args[n]);
                if (remoteRef.isPresent())
                    args[n] = remoteRef.get();
            }
        }

        final String methodId = method.toString().substring(15);

        final IRemoteMessage remoteCall = new RemoteCall(remoteInstance, methodId, args, id);

        sendMessage(remoteCall);

        RemoteReturn remoteReturn = null;

        boolean bReturned = false;
        do {
            synchronized (remoteReturns) {
                for (RemoteReturn ret : remoteReturns) {
                    if (ret.getCallId().equals(id)) {
                        bReturned = true;
                        remoteReturn = ret;
                        break;
                    }
                }
                if (bReturned)
                    remoteReturns.remove(remoteReturn);
                else {
                    try {
                        log.trace( "wait for remote return");
                        remoteReturns.wait( );
                        log.trace( "got remote return");
                    } catch (InterruptedException ie) {
                        log.warn("wait for remote return iterrupted!");
                        break;
                    }
                }
            }
        }
        while (tcpStream.isConnected() && !bReturned);

        if (!tcpStream.isConnected() && !bReturned)
            throw new LipeRMIException("Connection aborted");

        if (remoteReturn.isThrowing() && remoteReturn.getRet() instanceof Throwable)
            throw ((Throwable) remoteReturn.getRet()).getCause();

        if (remoteReturn.getRet() instanceof RemoteInstance) {
            RemoteInstance remoteInstanceReturn = (RemoteInstance) remoteReturn.getRet();
            Object proxyReturn = remoteInstanceProxys.get(remoteInstanceReturn);
            if (proxyReturn == null) {
                proxyReturn = CallProxy.buildProxy(remoteInstanceReturn, this);
                remoteInstanceProxys.put(remoteInstanceReturn, proxyReturn);
            }
            return proxyReturn;
        }

        return remoteReturn.getRet();
    }

    private Object getProxyFromRemoteInstance(RemoteInstance remoteInstance) {
        Object proxy = remoteInstanceProxys.get(remoteInstance);
        if (proxy == null) {
            try {
                proxy = CallProxy.buildProxy(remoteInstance, this);
            } catch (ClassNotFoundException e) {
                log.error("buildProxy error", e);
            }
            remoteInstanceProxys.put(remoteInstance, proxy);
        }
        return proxy;
    }

    private Optional<RemoteInstance> getRemoteInstanceFromProxy(Object proxy) {
        for (RemoteInstance remoteInstance : remoteInstanceProxys.keySet()) {
            if (remoteInstanceProxys.get(remoteInstance) == proxy)
                return Optional.of(remoteInstance);
        }

        return empty();
    }

    public CallHandler getCallHandler() {
        return callHandler;
    }

    public TCPFullDuplexStream getTCPStream() {
        return tcpStream;
    }
}
