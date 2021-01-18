package net.sf.lipermi.rmi;

import net.sf.lipermi.exception.LipeRMIException;
import net.sf.lipermi.handler.CallLookup;
import net.sf.lipermi.handler.filter.GZipFilter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import static java.util.Optional.empty;

/**
 *
 */
public final class LocateRegistry {

  static class ClientRegistry implements Registry{
    final Client client;

    public ClientRegistry(Client client) {
      this.client = client;
    }

    @Override
    public void bind(Class<?> ifc, Object obj) throws LipeRMIException {
      client.exportObject(ifc, obj);
    }

    @Override
    public <T> T lookup(Class<T> ifc) {
      return client.getGlobal(ifc);
    }
  }


  static class ServerRegistry implements Registry {
    final Server server;

    public ServerRegistry(int port) throws IOException {
      this.server = new Server();
      server.bind(port, UnicastRemoteObject.callHandler, empty() );
    }

    @Override
    public void bind(Class<?> ifc, Object obj) throws LipeRMIException {
      UnicastRemoteObject.callHandler.registerGlobal( ifc, obj);
    }

    @Override
    public <T> T lookup(Class<T> ifc) {
      throw new UnsupportedOperationException("lookup is not supported yet!");
    }
  }

  /**
   *
   * @param port
   * @return
   */
  public static Registry	createRegistry(int port) throws LipeRMIException {

    try {

      ServerRegistry server = new ServerRegistry(port);

      return server;

    } catch (IOException ex) {
      throw new LipeRMIException(ex);
    }
  }

  /**
   *
   * @param port
   * @return
   */
  public static Registry getRegistry(int port) throws LipeRMIException {
    try {

      final String address = InetAddress.getLocalHost().getHostAddress();
      final Client client = new Client(address, port, UnicastRemoteObject.callHandler, empty());
      return new ClientRegistry(client);
    }
    catch( Exception ex ) {
        throw new LipeRMIException(ex);
    }
  }
}
