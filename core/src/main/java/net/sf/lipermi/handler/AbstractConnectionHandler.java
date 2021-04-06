package net.sf.lipermi.handler;

import net.sf.lipermi.call.IRemoteMessage;
import net.sf.lipermi.call.RemoteCall;
import net.sf.lipermi.call.RemoteInstance;
import net.sf.lipermi.call.RemoteReturn;
import net.sf.lipermi.exception.LipeRMIException;
import net.sf.lipermi.handler.filter.IProtocolFilter;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Optional.empty;

public abstract class AbstractConnectionHandler implements Closeable  {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AbstractConnectionHandler.class);

  private final CallHandler callHandler;

  protected final IProtocolFilter filter;

  private static AtomicLong callId = new AtomicLong(0L);

  private final Map<RemoteInstance, Object> remoteInstanceProxys = new HashMap<>();

  protected final List<RemoteReturn> remoteReturns = new LinkedList<>();

  /**
   *
   * @param callHandler
   * @param filter
   */
  protected AbstractConnectionHandler(CallHandler callHandler, IProtocolFilter filter) {
    this.callHandler = callHandler;
    this.filter = filter;
  }

  /**
   *
   * @param remoteMessage
   * @throws IOException
   */
  protected abstract void writeMessage(IRemoteMessage remoteMessage) throws IOException ;

  /**
   *
   * @return
   */
  protected abstract boolean isConnected();

  /**
   *
   * @param task
   */
  protected void delegateCall(Runnable task ) {
    final Thread delegator = new Thread( task, "Delegator" );
    delegator.setDaemon(true);
    delegator.start();
  }

  /**
   *
   * @param remoteMessage
   *
   * @throws Exception
   */
  protected final void readMessage( final IRemoteMessage remoteMessage  ) throws Exception {

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

      delegateCall(() -> {
        try {

          log.trace("remoteCall: {} - {}", remoteCall.getCallId(), remoteCall.getRemoteInstance().getInstanceId());
          final RemoteReturn remoteReturn = callHandler.delegateCall(remoteCall);

          log.trace("remote return {} = {}", remoteCall.getCallId(), remoteReturn.getRet());
          writeMessage(remoteReturn);

        } catch (Exception e) {
          log.error("remoteCall error", e);
        }
      });

    } else if (remoteMessage instanceof RemoteReturn) {
      RemoteReturn remoteReturn = (RemoteReturn) remoteMessage;
      synchronized (remoteReturns) {
        remoteReturns.add(remoteReturn);
        remoteReturns.notifyAll();
      }
    } else
      throw new LipeRMIException("Unknown IRemoteMessage type");


  }

  /**
   *
   * @param proxy
   * @param method
   * @param args
   * @return
   * @throws Throwable
   */
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

    writeMessage(remoteCall);

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
    while (isConnected() && !bReturned);

    if (!isConnected() && !bReturned)
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

}
