package net.sf.lipermi;

import java.net.Socket;

/**
 * This listener can be used to monitor a Server.
 * (ie. know when it receives new connections or
 *  close it)
 *
 * @date   07/10/2006
 * @author lipe
 *
 * @see    Server
 */
public interface IServerListener {

  void clientConnected(Socket socket);

  void clientDisconnected(Socket socket);

}
