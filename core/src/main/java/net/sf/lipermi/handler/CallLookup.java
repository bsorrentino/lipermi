package net.sf.lipermi.handler;

import net.sf.lipermi.TCPFullDuplexStream;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.synchronizedMap;
import static java.util.Optional.ofNullable;

/**
 * A common static way to access the Socket
 * which started the Delegator Thread. A very useful
 * way to know who called the current Method.
 *
 * @author lipe
 * @date   07/10/2006
 *
 * @see       net.sf.lipermi.handler.CallHandler
 */
public class CallLookup {

    private static Map<Thread, ConnectionHandler> connectionMap = synchronizedMap(new HashMap<>());

    /**
     * Get the current Socket for this call.
     * Only works in the main thread call.
     *
     * @return Socket which started the Delegator Thread
     */
    public static Optional<TCPFullDuplexStream> getCurrentTCPStream() {
        final ConnectionHandler handler = connectionMap.get(Thread.currentThread());
        return ofNullable(handler).flatMap( (h) -> ofNullable(handler.getTCPStream()) );
    }

    static void handlingMe(ConnectionHandler connectionHandler) {
        connectionMap.put(Thread.currentThread(), connectionHandler);
    }

    static void forgetMe() {
        connectionMap.remove(Thread.currentThread());
    }
}
