package net.sf.lipermi.handler;

/**
 * This listener can be used to monitor a ConnectionHandler.
 * (ie. to know when it finishes)
 *
 * @date   07/10/2006
 * @author lipe

 * @see       net.sf.lipermi.handler.ConnectionHandler
 */

@FunctionalInterface
public interface IConnectionHandlerListener {

    void connectionClosed();

}
