package net.sf.lipermi.net;

/**
 * This listener can be used to monitor a Client.
 * (ie. know when it finishes the connection)
 *
 * @date   07/10/2006
 * @author lipe
 *
 * @see    BaseClient
 */
@FunctionalInterface
public interface IClientListener {

    void disconnected();

}
