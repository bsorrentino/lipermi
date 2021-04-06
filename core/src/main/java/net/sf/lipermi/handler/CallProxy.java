package net.sf.lipermi.handler;

import net.sf.lipermi.call.RemoteInstance;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;

/**
 * A dynamic proxy which delegates interface
 * calls to a ConnectionHandler
 *
 * @author lipe
 * @date   05/10/2006
 *
 * @see       net.sf.lipermi.handler.CallHandler
 */
public class CallProxy<C extends AbstractConnectionHandler> implements InvocationHandler  {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CallProxy.class);

    private final C connectionHandler;

    /**
     * Create new CallProxy with a ConnectionHandler which will
     * transport invocations on this Proxy
     *
     * @param connectionHandler
     */
    public CallProxy(C connectionHandler) {
        this.connectionHandler = connectionHandler;
    }

    /**
     * Delegates call to this proxy to it's ConnectionHandler
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        if( log.isTraceEnabled()) {
            final Class<?> ifc[] = proxy.getClass().getInterfaces();
            final Class<?>  clazz = ( ifc!=null && ifc.length > 0 ) ? ifc[0] :proxy.getClass();

            final String argsType = ofNullable(args).map(a ->
                    Arrays.stream(a).map( String::valueOf).collect(joining(",")))
                    .orElse("null");

            log.trace( "invoke on instance of {} method:{} ( {} )",
                    clazz.getName(),
                    method.getName(),
                    argsType
            );
        }
        // GUARD CODE
        if( method.getName().equals("hashCode")) {
            throw new IllegalAccessException("'hashCode' is not valid invocation on remote proxy");
        }
        else if( method.getName().equals("toString")) {
            throw new IllegalAccessException("'toString' is not valid invocation on remote proxy");
        }
        return connectionHandler.remoteInvocation(proxy, method, args);
    }

    /**
     * Build a proxy to a {@see net.sf.lipermi.call.RemoteInstance RemoteInstance}
     * specifing how it could be reached (i.e., through a ConnectionHandler)
     *
     * @param  remoteInstance
     * @param  connectionHandler
     * @return dymamic proxy for RemoteInstance
     * @throws ClassNotFoundException
     */
    public static <C extends AbstractConnectionHandler>  Object buildProxy(RemoteInstance remoteInstance, C connectionHandler) throws ClassNotFoundException {
        log.trace( "buildProxy for remote instance {}", remoteInstance.getClassName());

        final Class<?> clazz = Class.forName(remoteInstance.getClassName());
        return Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] { clazz }, new CallProxy(connectionHandler));
    }
}
