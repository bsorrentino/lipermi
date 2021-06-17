package net.sf.lipermi.handler;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

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
import static java.util.Optional.ofNullable;


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

//    public static final ThreadFactory threadFactory = runnable ->
//            (new Thread("ConnectionHandler"));

    public static final ConnectionHandler of( TCPFullDuplexStream socket, CallHandler callHandler, IProtocolFilter filter ) {
        return new ConnectionHandler(socket, callHandler, filter);
    }

    public static ConnectionHandler start(TCPFullDuplexStream socket, CallHandler callHandler, IProtocolFilter filter) {
        ConnectionHandler connectionHandler = of(socket, callHandler, filter);

        String threadName = format("ConnectionHandler (%s:%d)", socket.getInetAddress().getHostAddress(), socket.getPort());
        Thread connectionHandlerThread = new Thread(connectionHandler, threadName);
        connectionHandlerThread.setDaemon(true);
        connectionHandlerThread.start();

        return connectionHandler;
    }

    private final TCPFullDuplexStream tcpStream;

    private final CallHandler callHandler;

    private final IProtocolFilter filter;

    private ObjectOutputStream output;

    private static AtomicLong callId = new AtomicLong(0L);

    private final Map<RemoteInstance, Object> remoteInstanceProxys = new HashMap<>();

    private final List<RemoteReturn> remoteReturns = new LinkedList<>();

    private final ReentrantLock _lock = new ReentrantLock();
    private final Condition _remoteReturns = _lock.newCondition();

    private ConnectionHandler(TCPFullDuplexStream socket, CallHandler callHandler, IProtocolFilter filter) {
        this.callHandler = callHandler;
        this.tcpStream = socket;
        this.filter = filter;
    }

    private void process( RemoteReturn remoteReturn ) {

        _lock.lock();
        try {
            remoteReturns.add(remoteReturn);
            _remoteReturns.signalAll();
        }
        finally {
            _lock.unlock();
        }

    }

    /**
     *
     * @param remoteCall
     */
    private void process( RemoteCall remoteCall ) {

        ofNullable(remoteCall.getArgs()).ifPresent( args -> {

            for (int n = 0; n < args.length; n++) {
                final Object arg = args[n];

                if (arg instanceof RemoteInstance) {
                    RemoteInstance remoteInstance = (RemoteInstance) arg;
                    args[n] = getProxyFromRemoteInstance(remoteInstance);
                }
            }
        });

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

    }


    @Override
    public void run() {
        try (
                final ObjectInputStream input = new ObjectInputStream(tcpStream.getInputStream())
        )
        {

            while (tcpStream.isConnected()) {

                final Object objFromStream = input.readUnshared();

                final IRemoteMessage remoteMessage = filter.readObject(objFromStream);

                if (remoteMessage instanceof RemoteCall) {

                    process( (RemoteCall)remoteMessage );

                } else if (remoteMessage instanceof RemoteReturn) {

                    process( (RemoteReturn) remoteMessage);

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

            _lock.lock();
            try {
                _remoteReturns.signalAll();
            }
            finally {
                _lock.unlock();
            }
        }
    }

    private void sendMessage(IRemoteMessage remoteMessage) throws IOException {
        synchronized (tcpStream) {
            if (output == null)
                output = new ObjectOutputStream(tcpStream.getOutputStream());

            final Object objToWrite = filter.prepareWrite(remoteMessage);
            output.reset();
            output.writeUnshared(objToWrite);
            output.flush();
        }
    }

    final Object remoteInvocation(final Object proxy, final Method method, final Object[] args) throws Throwable {
        final long id = callId.getAndIncrement();

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
            _lock.lock();
           try {
                for (RemoteReturn ret : remoteReturns) {
                    if (ret.getCallId().equals(id)) {
                        bReturned = true;
                        remoteReturn = ret;
                        break;
                    }
                }
                if (bReturned) {
                    remoteReturns.remove(remoteReturn);
                }
                else {
                    log.trace( "wait for remote return");
                    _remoteReturns.await(10, TimeUnit.SECONDS);
                    log.trace( "got remote return");
                }
            }
            catch( InterruptedException ex ) {
                log.warn("wait for remote return iterrupted!");
                break;
            }
            finally {
               _lock.unlock();
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
